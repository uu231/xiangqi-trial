package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.ArrayList;
import java.util.List;

public class Ma extends Piece {

    public Ma(Player player, Position position) {
        super(player, position);
    }

    @Override
    public String getName() {
        return "马";
    }

    @Override
    public List<Position> getValidMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int r = this.position.row();
        int c = this.position.col();

        // --- 任务：在这里检查 8 个方向 ---

        // 我来帮你实现 1 个方向 (上-右)
        // 1. "马腿" (拐点) 在 (r-1, c)
        Position blockerPos1 = new Position(r - 1, c);
        // 2. 检查"马腿"是否为空
        if (blockerPos1.isValid() && board.getPiece(blockerPos1) == null) {
            // 3. "马腿"为空, 检查目标点 (r-2, c+1)
            Position targetPos = new Position(r - 2, c + 1);
            // 4. 使用辅助方法检查目标点
            addMoveIfValid(board, moves, targetPos);
        }

        // 另 1 个方向 (上-左)
        // 1. "马腿" (拐点) 还是在 (r-1, c)
        // (注意: 我们不需要再次检查 (r-1, c) 了, 可以在同一个 if 里)
        if (blockerPos1.isValid() && board.getPiece(blockerPos1) == null) {
            // 3. 检查目标点 (r-2, c-1)
            Position targetPos = new Position(r - 2, c - 1);
            // 4. 使用辅助方法检查目标点
            addMoveIfValid(board, moves, targetPos);
        }
        
        // --- 你的任务 ---
        // 请模仿上面的逻辑, 实现剩下 6 个方向:
        // (右-上): 马腿 (r, c+1), 目标 (r-1, c+2)
        Position blockerPos2 = new Position(r, c + 1);
        if (blockerPos2.isValid() && board.getPiece(blockerPos2) == null) {
            Position targetPos = new Position(r - 1, c + 2);
            addMoveIfValid(board, moves, targetPos);
        }
        // (右-下): 马腿 (r, c+1), 目标 (r+1, c+2)
        if (blockerPos2.isValid() && board.getPiece(blockerPos2) == null) {
            Position targetPos = new Position(r + 1, c + 2);
            addMoveIfValid(board, moves, targetPos);
        }
        
        // (下-右): 马腿 (r+1, c), 目标 (r+2, c+1)
        Position blockerPos3 = new Position(r + 1, c);
        if (blockerPos3.isValid() && board.getPiece(blockerPos3) == null) {
            Position targetPos = new Position(r + 2, c + 1);
            addMoveIfValid(board, moves, targetPos);
        }
        // (下-左): 马腿 (r+1, c), 目标 (r+2, c-1)
        if (blockerPos3.isValid() && board.getPiece(blockerPos3) == null) {
            Position targetPos = new Position(r + 2, c - 1);
            addMoveIfValid(board, moves, targetPos);
        }

        // (左-上): 马腿 (r, c-1), 目标 (r-1, c-2)
        Position blockerPos4 = new Position(r, c - 1);
        if (blockerPos4.isValid() && board.getPiece(blockerPos4) == null) {
            Position targetPos = new Position(r - 1, c - 2);
            addMoveIfValid(board, moves, targetPos);
        }
        // (左-下): 马腿 (r, c-1), 目标 (r+1, c-2)
        if (blockerPos4.isValid() && board.getPiece(blockerPos4) == null) {
            Position targetPos = new Position(r + 1, c - 2);
            addMoveIfValid(board, moves, targetPos);
        }


        return moves;
    }

    /**
     * 辅助方法 (和 "车" 的 checkPosition 不一样!)
     * 这个方法只检查 *目标点* (targetPos)
     * 它假设你已经检查过 "马腿" (blockerPos) 是空的
     */
    private void addMoveIfValid(Board board, List<Position> moves, Position targetPos) {
        // 1. 检查目标点是否在棋盘内
        if (!targetPos.isValid()) {
            return;
        }

        // 2. 检查目标点上的棋子
        Piece targetPiece = board.getPiece(targetPos);

        if (targetPiece == null) {
            // A. 是空位, 加入
            moves.add(targetPos);
        } else if (targetPiece.getPlayer() != this.player) {
            // B. 是敌方棋子, 加入 (吃子)
            moves.add(targetPos);
        }
        // C. 是己方棋子, 什么也不做
    }
}