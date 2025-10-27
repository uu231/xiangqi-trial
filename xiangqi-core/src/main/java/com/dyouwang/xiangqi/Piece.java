package com.dyouwang.xiangqi;

import java.util.List;

/**
 * 抽象棋子类
 */
public abstract class Piece {
    // 棋子属于哪一方
    protected final Player player;
    // 棋子在棋盘上的位置
    protected Position position;

    public Piece(Player player, Position position) {
        this.player = player;
        this.position = position;
    }
    
    public Player getPlayer() {
        return player;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * 获取棋子的名字 (用于打印棋盘)
     */
    public abstract String getName();

    /**
     * 核心方法：获取该棋子在当前棋局下的所有合法走法
     */
    public abstract List<Position> getValidMoves(Board board);
}