package com.dyouwang.xiangqi.messages;

// import com.dyouwang.xiangqi.Board; // Board 太复杂, 不直接传输
import com.dyouwang.xiangqi.Player;
import com.dyouwang.xiangqi.Piece; // 需要 Piece 来定义棋盘状态
import com.dyouwang.xiangqi.Position; // 需要 Position

import java.util.ArrayList;
import java.util.List;

// 服务器发送游戏状态时使用
public class GameStateMessage extends BaseMessage {
    // 我们不直接发送 Board 对象, 而是发送一个简化的表示
    public List<SimplePieceInfo> pieces; // 所有棋子的列表
    public Player currentPlayer;
    public boolean isCheck; // 当前玩家是否被将军
    public boolean isCheckmate;
    public boolean isStalemate;

    // 内部类, 用于简化棋子信息传输
    public static class SimplePieceInfo {
        public int row;
        public int col;
        public String name; // 棋子名称 ("车", "马", ...)
        public Player player; // 棋子颜色 (RED/BLACK)

        public SimplePieceInfo() {} // Jackson 需要

        public SimplePieceInfo(Piece piece) {
            this.row = piece.getPosition().row();
            this.col = piece.getPosition().col();
            this.name = piece.getName();
            this.player = piece.getPlayer();
        }
    }

    public GameStateMessage() {
        super(MessageType.GAME_STATE);
        this.pieces = new ArrayList<>();
    }

    // 可以添加一个从 Game 对象构造的辅助方法 (暂不实现)
}