package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.ArrayList;
import java.util.List;

public class Jiang extends Piece {

    public Jiang(Player player, Position position) {
        super(player, position);
    }

    @Override
    public String getName() {
        // 红方是 "帅", 黑方是 "将"
        return (this.player == Player.RED) ? "帅" : "将";
    }

    @Override
    public List<Position> getValidMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int r = this.position.row();
        int c = this.position.col();

        // 定义 4 个可能的移动方向 (上, 下, 左, 右)
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            Position targetPos = new Position(r + dRow[i], c + dCol[i]);

            // 1. 检查目标点是否在九宫内
            if (!isInPalace(targetPos)) {
                continue;
            }

            // 2. 检查目标点是空, 还是敌人
            addMoveIfValid(board, moves, targetPos);
        }

        // TODO: "王见王" (Flying General) 规则
        // 这是一个游戏状态规则, 而不是棋子移动规则
        // 我们会在更高层(Game.java)中处理
        
        return moves;
    }

    /**
     * 辅助方法: 检查一个位置是否在九宫内
     */
    private boolean isInPalace(Position pos) {
        // 检查列 (3, 4, 5)
        if (pos.col() < 3 || pos.col() > 5) {
            return false;
        }

        // 检查行 (红方 7,8,9; 黑方 0,1,2)
        if (this.player == Player.RED) {
            return pos.row() >= 7 && pos.row() <= 9;
        } else {
            return pos.row() >= 0 && pos.row() <= 2;
        }
    }
    
    /**
     * 辅助方法: 检查目标点
     */
    private void addMoveIfValid(Board board, List<Position> moves, Position targetPos) {
        // 目标点必须在棋盘上 (虽然 isInPalace 已经检查过了)
        if (!targetPos.isValid()) {
            return;
        }
        
        Piece targetPiece = board.getPiece(targetPos);
        if (targetPiece == null) {
            moves.add(targetPos); // 空位
        } else if (targetPiece.getPlayer() != this.player) {
            moves.add(targetPos); // 敌人
        }
    }
}