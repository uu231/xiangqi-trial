package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.ArrayList;
import java.util.List;

public class Xiang extends Piece {

    public Xiang(Player player, Position position) {
        super(player, position);
    }

    @Override
    public String getName() {
        return (this.player == Player.RED) ? "相" : "象";
    }

    @Override
    public List<Position> getValidMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int r = this.position.row();
        int c = this.position.col();

        // 4 个"田"字目标点
        Position[] targetPositions = {
            new Position(r - 2, c - 2), // 左上
            new Position(r - 2, c + 2), // 右上
            new Position(r + 2, c - 2), // 左下
            new Position(r + 2, c + 2)  // 右下
        };
        
        // 4 个"象眼" (拐点)
        Position[] blockerPositions = {
            new Position(r - 1, c - 1), // 左上
            new Position(r - 1, c + 1), // 右上
            new Position(r + 1, c - 1), // 左下
            new Position(r + 1, c + 1)  // 右下
        };

        for (int i = 0; i < 4; i++) {
            Position targetPos = targetPositions[i];
            Position blockerPos = blockerPositions[i];

            // 1. 检查目标点不能过河
            if (!isInOwnTerritory(targetPos)) {
                continue;
            }

            // 2. 检查象眼 (blockerPos) 必须为空
            if (blockerPos.isValid() && board.getPiece(blockerPos) == null) {
                // 3. 检查目标点
                addMoveIfValid(board, moves, targetPos);
            }
        }
        
        return moves;
    }

    /**
     * 辅助方法: 检查"象"是否在自己领地 (不能过河)
     */
    private boolean isInOwnTerritory(Position pos) {
        if (!pos.isValid()) {
            return false;
        }
        if (this.player == Player.RED) {
            return pos.row() >= 5; // 红方领地 (5-9)
        } else {
            return pos.row() <= 4; // 黑方领地 (0-4)
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