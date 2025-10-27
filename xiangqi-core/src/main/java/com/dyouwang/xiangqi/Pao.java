package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.ArrayList;
import java.util.List;

public class Pao extends Piece {

    public Pao(Player player, Position position) {
        super(player, position);
    }

    @Override
    public String getName() {
        return "炮";
    }

// ... 在 Pao.java ...

    @Override
    public List<Position> getValidMoves(Board board) {
        List<Position> moves = new ArrayList<>();

        // 1. 检查上方 (row 减小)
        boolean foundScreenUp = false; // "炮台"
        for (int r = this.position.row() - 1; r >= 0; r--) {
            Position newPos = new Position(r, this.position.col());
            Piece targetPiece = board.getPiece(newPos);

            if (targetPiece == null) {
                // 是空位
                if (!foundScreenUp) {
                    moves.add(newPos); // 炮台前的移动
                }
            } else {
                // 是棋子
                if (!foundScreenUp) {
                    foundScreenUp = true; // 找到了炮台
                } else {
                    // 这是炮台后的第一个棋子
                    if (targetPiece.getPlayer() != this.player) {
                        moves.add(newPos); // 吃子
                    }
                    break; // 【BUG 修复】 无论吃不吃, 必须停止
                }
            }
        }

        // 2. 检查下方 (row 增大, 最大到 9)
        boolean foundScreenDown = false;
        for (int r = this.position.row() + 1; r <= 9; r++) {
            Position newPos = new Position(r, this.position.col());
            Piece targetPiece = board.getPiece(newPos);
            if (targetPiece == null) {
                if (!foundScreenDown) {
                    moves.add(newPos);
                }
            } else {
                if (!foundScreenDown) {
                    foundScreenDown = true;
                } else {
                    if (targetPiece.getPlayer() != this.player) {
                        moves.add(newPos);
                    }
                    break; // 【BUG 修复】
                }
            }
        }

        // 3. 检查左方 (col 减小)
        boolean foundScreenLeft = false;
        for (int c = this.position.col() - 1; c >= 0; c--) {
            Position newPos = new Position(this.position.row(), c);
            Piece targetPiece = board.getPiece(newPos);
            if (targetPiece == null) {
                if (!foundScreenLeft) {
                    moves.add(newPos);
                }
            } else {
                if (!foundScreenLeft) {
                    foundScreenLeft = true;
                } else {
                    if (targetPiece.getPlayer() != this.player) {
                        moves.add(newPos);
                    }
                    break; // 【BUG 修复】
                }
            }
        }

        // 4. 检查右方 (col 增大, 最大到 8)
        boolean foundScreenRight = false;
        for (int c = this.position.col() + 1; c <= 8; c++) {
            Position newPos = new Position(this.position.row(), c);
            Piece targetPiece = board.getPiece(newPos);
            if (targetPiece == null) {
                if (!foundScreenRight) {
                    moves.add(newPos);
                }
            } else {
                if (!foundScreenRight) {
                    foundScreenRight = true;
                } else {
                    if (targetPiece.getPlayer() != this.player) {
                        moves.add(newPos);
                    }
                    break; // 【BUG 修复】
                }
            }
        }

        return moves;
    }
}