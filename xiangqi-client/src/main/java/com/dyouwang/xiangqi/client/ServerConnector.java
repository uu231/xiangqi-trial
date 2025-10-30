package com.dyouwang.xiangqi.client;

import com.dyouwang.xiangqi.messages.BaseMessage;
import com.dyouwang.xiangqi.messages.GameStateMessage;
import com.dyouwang.xiangqi.messages.MessageType;
import com.dyouwang.xiangqi.messages.SendMoveMessage;
import com.dyouwang.xiangqi.messages.AssignPlayerMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform; // 用于在 JavaFX 线程上更新 UI
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class ServerConnector {

    private WebSocketClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MainApp mainApp; // 需要回调主应用来更新 UI

    public ServerConnector(String serverUri, MainApp mainApp) {
        this.mainApp = mainApp;
        try {
            URI uri = new URI(serverUri);
            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("WebSocket 连接已打开");
                    // 连接打开后可以做一些事情，比如请求加入游戏
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("收到服务器消息: " + message);
                    try {
                        // 1. 解析基础消息判断类型
                        BaseMessage baseMessage = objectMapper.readValue(message, BaseMessage.class);

                        // --- 【新】处理角色分配消息 ---
                        if (baseMessage.type == MessageType.ASSIGN_PLAYER) {
                            AssignPlayerMessage assignMsg = objectMapper.readValue(message, AssignPlayerMessage.class);
                            Platform.runLater(() -> {
                                mainApp.setMyPlayer(assignMsg.assignedPlayer); // 调用 MainApp 的新方法
                            });
                        }
                        // --- 处理角色分配结束 ---

                        // 2. 如果是游戏状态更新
                        else if (baseMessage.type == MessageType.GAME_STATE) {
                            GameStateMessage gameState = objectMapper.readValue(message, GameStateMessage.class);
                            Platform.runLater(() -> {
                                mainApp.updateGameFromState(gameState);
                            });
                        }
                        // 3. 如果是错误消息
                        else if (baseMessage.type == MessageType.ERROR) {
                            // ... (错误处理不变) ...
                        }
                        // (处理其他消息类型)

                    } catch (JsonProcessingException e) {
                         System.err.println("无法解析服务器消息 JSON: " + message + " - Error: " + e.getMessage());
                    } catch (Exception e) {
                         System.err.println("处理服务器消息时出错: " + e.getMessage());
                         e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket 连接已关闭: " + reason);
                    // TODO: 通知 MainApp 连接已断开
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket 错误: " + ex.getMessage());
                    // TODO: 通知 MainApp 连接错误
                }
            };
        } catch (URISyntaxException e) {
            System.err.println("无效的服务器 URI: " + serverUri + " - " + e.getMessage());
        }
    }

    // ... (在 ServerConnector.java 中) ...

    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    public void connect() {
        // 在连接前检查
        if (client != null && !client.isOpen()) { 
            System.out.println("尝试连接到 WebSocket 服务器...");
            client.connect(); // 异步连接
        } else if (client != null && client.isOpen()) {
            System.out.println("已经连接到服务器。");
        }
    }

    public void disconnect() {
        if (client != null && client.isOpen()) {
            client.close();
        }
    }

    /**
     * 向服务器发送走法消息
     */
    public void sendMove(com.dyouwang.xiangqi.Move move) {
        if (client != null && client.isOpen()) {
            try {
                SendMoveMessage message = new SendMoveMessage(move);
                String jsonMessage = objectMapper.writeValueAsString(message);
                System.out.println("发送走法消息: " + jsonMessage);
                client.send(jsonMessage);
            } catch (JsonProcessingException e) {
                System.err.println("无法序列化走法消息为 JSON: " + e.getMessage());
            }
        } else {
            System.err.println("WebSocket 未连接, 无法发送消息");
            // TODO: 提示用户连接已断开
        }
    }
}