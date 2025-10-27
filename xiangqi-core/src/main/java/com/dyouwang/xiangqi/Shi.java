package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.ArrayList;
import java.util.List;

public class Shi extends Piece {

    public Shi(Player player, Position position) {
        super(player, position);
    }

    @Override
    public String getName() {
        return (this.player == Player.RED) ? "仕" : "士";
    }

    @Override
    public List<Position> getValidMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int r = this.position.row();
        int c = this.position.col();

        // 定义 4 个可能的斜向 (左上, 右上, 左下, 右下)
        int[] dRow = {-1, -1, 1, 1};
        int[] dCol = {-1, 1, -1, 1};

        for (int i = 0; i < 4; i++) {
            Position targetPos = new Position(r + dRow[i], c + dCol[i]);

            // 1. 检查目标点是否在九宫内
            if (!isInPalace(targetPos)) {
                continue;
            }

            // 2. 检查目标点是空, 还是敌人
            addMoveIfValid(board, moves, targetPos);
        }
        
        return moves;
    }

    /**
     * 辅助方法: 检查一个位置是否在九宫内
     */
    private boolean isInPalace(Position pos) {
        if (pos.col() < 3 || pos.col() > 5) {
            return false;
        }
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
        if (!targetPos.isValid()) {
            return;
        }
        Piece targetPiece = board.getPiece(targetPos);
        if (targetPiece == null) {
            moves.add(targetPos);
        } else if (targetPiece.getPlayer() != this.player) {
            moves.add(targetPos);
        }
    }
}