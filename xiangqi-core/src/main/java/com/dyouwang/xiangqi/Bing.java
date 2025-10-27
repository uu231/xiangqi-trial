package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.ArrayList;
import java.util.List;

public class Bing extends Piece {

    public Bing(Player player, Position position) {
        super(player, position);
    }

    @Override
    public String getName() {
        return (this.player == Player.RED) ? "兵" : "卒";
    }

    @Override
    public List<Position> getValidMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int r = this.position.row();
        int c = this.position.col();

        // 1. "前进" 永远是合法方向
        // 红"兵" 前进 = row 减小
        // 黑"卒" 前进 = row 增大
        int forwardRow = (this.player == Player.RED) ? r - 1 : r + 1;
        Position forwardPos = new Position(forwardRow, c);
        addMoveIfValid(board, moves, forwardPos);

        // 2. 检查是否 "已过河"
        if (hasCrossedRiver()) {
            // 3. "左" 也是合法方向
            Position leftPos = new Position(r, c - 1);
            addMoveIfValid(board, moves, leftPos);

            // 4. "右" 也是合法方向
            Position rightPos = new Position(r, c + 1);
            addMoveIfValid(board, moves, rightPos);
        }
        
        return moves;
    }

    /**
     * 辅助方法: 检查是否已过河
     */
    private boolean hasCrossedRiver() {
        if (this.player == Player.RED) {
            return this.position.row() <= 4; // 红兵过河 (row 0-4)
        } else {
            return this.position.row() >= 5; // 黑卒过河 (row 5-9)
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