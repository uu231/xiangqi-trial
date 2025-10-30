package com.dyouwang.xiangqi.server;

import com.dyouwang.xiangqi.Board;
import com.dyouwang.xiangqi.Game;
import com.dyouwang.xiangqi.Move;
import com.dyouwang.xiangqi.Piece;
import com.dyouwang.xiangqi.Player;
import com.dyouwang.xiangqi.Position;
import com.dyouwang.xiangqi.messages.BaseMessage;
import com.dyouwang.xiangqi.messages.ErrorMessage;
import com.dyouwang.xiangqi.messages.GameStateMessage;
import com.dyouwang.xiangqi.messages.MessageType;
import com.dyouwang.xiangqi.messages.SendMoveMessage;
import com.dyouwang.xiangqi.messages.AssignPlayerMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper; // Jackson 核心类

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // 线程安全的 Map

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);

    // 用于 JSON 序列化/反序列化
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 【极简】游戏状态管理: 只支持一个游戏, 最多两个玩家
    private Game game = new Game(); // 创建一个游戏实例
    private final Map<Player, WebSocketSession> players = new ConcurrentHashMap<>(); // 存储玩家和对应的 Session

   @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket 连接已建立: Session ID = {}", session.getId());

        Player assignedPlayer = null; // 用于记录分配的角色

        if (!players.containsKey(Player.RED)) {
            players.put(Player.RED, session);
            assignedPlayer = Player.RED; // 记录
            logger.info("玩家 RED 加入: Session ID = {}", session.getId());

        } else if (!players.containsKey(Player.BLACK)) {
            players.put(Player.BLACK, session);
            assignedPlayer = Player.BLACK; // 记录
            logger.info("玩家 BLACK 加入: Session ID = {}", session.getId());

        } else {
            // 游戏已满
            logger.warn("游戏已满, 拒绝连接: Session ID = {}", session.getId());
            sendMessage(session, new ErrorMessage("游戏已满"));
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Game full"));
            return; // 直接返回
        }

        // --- 【新】如果成功分配了角色, 发送 AssignPlayerMessage ---
        if (assignedPlayer != null) {
            sendMessage(session, new AssignPlayerMessage(assignedPlayer));
            logger.info("已向 Session {} 发送角色分配: {}", session.getId(), assignedPlayer);
        }
        // --- 发送角色结束 ---

        // 发送初始游戏状态 (这部分逻辑不变)
        sendGameState(session);
        if (players.size() == 2) {
            broadcastGameState();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("收到来自 Session {} 的消息: {}", session.getId(), payload);

        try {
            // 1. 解析 BaseMessage (检查 type)
            BaseMessage baseMessage = objectMapper.readValue(payload, BaseMessage.class);
            logger.info("消息类型解析成功: {}", baseMessage.type); // 添加日志

            // 2. 根据消息类型处理
            if (baseMessage.type == MessageType.SEND_MOVE) {
                logger.info("尝试解析为 SendMoveMessage..."); // 添加日志
                try {
                    // 【修改】将特定解析放在单独的 try-catch 中
                    SendMoveMessage moveMessage = objectMapper.readValue(payload, SendMoveMessage.class);
                    logger.info("SendMoveMessage 解析成功!"); // 添加日志
                    handleMoveMessage(session, moveMessage.move);

                } catch (JsonProcessingException jsonEx) {
                    // 【关键】捕获并记录详细的 JSON 解析错误
                    logger.error("解析 SendMoveMessage 失败! JSON: {}, 错误: {}", payload, jsonEx.getMessage());
                    sendMessage(session, new ErrorMessage("无法解析走法消息: " + jsonEx.getOriginalMessage())); // 把具体错误发回客户端
                }
            }
            // (其他消息类型)
            else {
                logger.warn("收到未知类型的消息: {}", baseMessage.type);
                sendMessage(session, new ErrorMessage("未知的消息类型: " + baseMessage.type));
            }

        } catch (JsonProcessingException e) {
            // 这个 catch 现在只处理 BaseMessage 解析失败的情况
            logger.error("基础消息 JSON 解析错误: {}", payload, e);
            sendMessage(session, new ErrorMessage("无效的基础 JSON 格式"));
        } catch (Exception e) {
            logger.error("处理消息时发生未知错误: {}", payload, e);
            sendMessage(session, new ErrorMessage("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 【新】处理客户端发送的走法消息
     */
    private void handleMoveMessage(WebSocketSession session, Move move) {
        // 1. 确定发送消息的玩家是谁
        Player currentPlayer = findPlayerBySession(session);
        if (currentPlayer == null) {
            logger.warn("收到来自未知 Session {} 的走法消息", session.getId());
            sendMessage(session, new ErrorMessage("未识别的玩家"));
            return;
        }

        // 2. 检查是否轮到该玩家走棋
        if (game.getCurrentPlayer() != currentPlayer) {
            logger.warn("玩家 {} 尝试在非其回合走棋", currentPlayer);
            sendMessage(session, new ErrorMessage("还没轮到你走棋"));
            return;
        }

        // 3. 尝试执行走法
        boolean success = game.makeMove(move);

        // 4. 如果走法成功
        if (success) {
            logger.info("玩家 {} 走棋成功: {} -> {}", currentPlayer, move.from(), move.to());
            // 向 *所有* 玩家广播更新后的游戏状态
            broadcastGameState(move);
        }
        // 5. 如果走法失败
        else {
            logger.warn("玩家 {} 走棋失败: {} -> {}", currentPlayer, move.from(), move.to());
            // 向该玩家发送错误信息 (Game.makeMove 会打印具体原因到服务器日志)
            sendMessage(session, new ErrorMessage("非法走法"));
            // (可选) 重新发送当前状态给该玩家, 确保他看到的是正确的棋盘
            // sendGameState(session);
        }
    }


/**
     * 【新】向所有玩家广播 (带走法)
     */
    private void broadcastGameState(Move lastMove) {
        GameStateMessage gameState = createGameStateMessage(lastMove); // Call the version WITH Move
        logger.info("广播游戏状态: 当前玩家 {}", gameState.currentPlayer);
        for (WebSocketSession playerSession : players.values()) {
            sendMessage(playerSession, gameState);
        }
    }

    /**
     * 【旧】向所有玩家广播 (无走法, 用于初始连接)
     */
    private void broadcastGameState() {
        broadcastGameState(null); // Call the new method with null
    }
/**
     * 【新】向指定 Session 发送 (带走法)
     */
    private void sendGameState(WebSocketSession session, Move lastMove) {
        GameStateMessage gameState = createGameStateMessage(lastMove); // Call the version WITH Move
        logger.info("向 Session {} 发送游戏状态", session.getId());
        sendMessage(session, gameState);
    }

    /**
     * 【旧】向指定 Session 发送 (无走法)
     */
    private void sendGameState(WebSocketSession session) {
        sendGameState(session, null); // Call the new method with null
    }

    /**
     * 【新】根据当前 Game 对象创建 GameStateMessage
     */
    private GameStateMessage createGameStateMessage(Move lastMove) {
        GameStateMessage msg = new GameStateMessage();
        Board board = game.getBoard();
        // 填充棋子信息
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Piece piece = board.getPiece(new Position(r, c));
                if (piece != null) {
                    msg.pieces.add(new GameStateMessage.SimplePieceInfo(piece));
                }
            }
        }
        // 填充其他状态
        msg.currentPlayer = game.getCurrentPlayer();
        msg.isCheck = game.isKingInCheck(msg.currentPlayer);
        msg.isCheckmate = game.isCheckmate(msg.currentPlayer);
        msg.isStalemate = game.isStalemate(msg.currentPlayer);
        msg.lastMove = lastMove; // 【新】设置 lastMove
        return msg;
    }

    /**
     * 【新】通用方法: 将 BaseMessage 对象序列化为 JSON 并发送
     */
    private void sendMessage(WebSocketSession session, BaseMessage message) {
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (JsonProcessingException e) {
                logger.error("JSON 序列化错误 for message type {}: {}", message.type, e.getMessage());
            } catch (IOException e) {
                logger.error("发送 WebSocket 消息失败 to Session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    /**
     * 【新】辅助方法: 根据 Session 查找玩家
     */
    private Player findPlayerBySession(WebSocketSession session) {
        for (Map.Entry<Player, WebSocketSession> entry : players.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // 当 WebSocket 连接关闭时调用
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket 连接已关闭: Session ID = {}, Status = {}", session.getId(), status);
        // 从玩家列表中移除
        Player leavingPlayer = findPlayerBySession(session);
        if (leavingPlayer != null) {
            players.remove(leavingPlayer);
            logger.info("玩家 {} 离开", leavingPlayer);
            // TODO: 处理玩家离开逻辑 (例如: 广播消息, 重置游戏?)
            // 为了简单, 我们暂时重启游戏
            if (players.isEmpty()) {
                 logger.info("所有玩家离开, 重置游戏");
                 game = new Game(); // 创建新游戏
            } else {
                 // 可以通知另一个玩家对方已离开
                 Player remainingPlayer = players.keySet().iterator().next();
                 sendMessage(players.get(remainingPlayer), new ErrorMessage("对方已断开连接"));
            }
        }
    }

    // 当传输发生错误时调用
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket 传输错误: Session ID = {}", session.getId(), exception);
        // 可以在这里也处理玩家离开逻辑
         afterConnectionClosed(session, CloseStatus.PROTOCOL_ERROR);
    }
}