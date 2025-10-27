package com.dyouwang.xiangqi;

import java.util.List;
import java.util.ArrayList;

public class Che extends Piece {
    public Che(Player player, Position position) {
        super(player, position);
    }

    @Override public String getName() {
        return "车";
    }

    @Override public List<Position> getValidMoves(Board board) {
        List<Position> moves = new ArrayList<>();//空链表存放所有合法走法

        for (int r = this.position.row() - 1; r >= 0; r--) {//向上检查
            Position newPos = new Position(r, this.position.col());
            if (checkPosition(board, newPos, moves) == false) {
                break;
            }
        }

        for (int r = this.position.row() + 1; r <= 9; r++) {//向下检查
            Position newPos = new Position(r, this.position.col());
            if (checkPosition(board, newPos, moves) == false) {
                break;
            }
        }

        for (int c = this.position.col() - 1; c >= 0; c--) {//向左检查
            Position newPos = new Position(this.position.row(), c);
            if (checkPosition(board, newPos, moves) == false) {
                break;
            }
        }

        for (int c = this.position.col() + 1; c <= 8; c++) {//向右检查
            Position newPos = new Position(this.position.row(), c);
            if (checkPosition(board, newPos, moves) == false) {
                break;
            }
        }

        return moves;//返回所有走法
    }

    /**
     * 这是一个辅助方法, 用来检查一个新位置
     * @param board 棋盘
     * @param newPos 要检查的新位置
     * @param moves 合法走法列表
     * @return 返回 true 表示"可以继续", 返回 false 表示"撞到了, 停止"
     */
    private boolean checkPosition(Board board, Position newPos, List<Position> moves) {
        Piece targetPiece = board.getPiece(newPos);

        if (targetPiece == null) {
            // A. 是空位, 加入列表, 继续循环
            moves.add(newPos);
            return true; // 可以继续
        }
        
        if (targetPiece.getPlayer() != this.player) {
            // B. 是敌方棋子, 加入列表 (吃子), 然后停止
            moves.add(newPos);
            return false; // 撞到敌人, 停止
        }

        // C. 是己方棋子, 停止 (不能吃, 也不能加入列表)
        return false; // 撞到队友, 停止
    }

}

