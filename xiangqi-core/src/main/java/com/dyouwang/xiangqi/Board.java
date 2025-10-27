package com.dyouwang.xiangqi; // 确保这是你的包名

/**
 * 棋盘类
 */
public class Board {
    // 10 行 x 9 列 的棋盘
    private final Piece[][] grid = new Piece[10][9];

    /**
     * 构造函数 - 现在我们在这里初始化棋盘
     */
    public Board() {
        // --- 放置黑方 (BLACK) 棋子 ---
        // 底线
        grid[0][0] = new Che(Player.BLACK, new Position(0, 0));
        grid[0][1] = new Ma(Player.BLACK, new Position(0, 1));
        grid[0][2] = new Xiang(Player.BLACK, new Position(0, 2));
        grid[0][3] = new Shi(Player.BLACK, new Position(0, 3));
        grid[0][4] = new Jiang(Player.BLACK, new Position(0, 4));
        grid[0][5] = new Shi(Player.BLACK, new Position(0, 5));
        grid[0][6] = new Xiang(Player.BLACK, new Position(0, 6));
        grid[0][7] = new Ma(Player.BLACK, new Position(0, 7));
        grid[0][8] = new Che(Player.BLACK, new Position(0, 8));
        // 炮
        grid[2][1] = new Pao(Player.BLACK, new Position(2, 1));
        grid[2][7] = new Pao(Player.BLACK, new Position(2, 7));
        // 卒
        grid[3][0] = new Bing(Player.BLACK, new Position(3, 0));
        grid[3][2] = new Bing(Player.BLACK, new Position(3, 2));
        grid[3][4] = new Bing(Player.BLACK, new Position(3, 4));
        grid[3][6] = new Bing(Player.BLACK, new Position(3, 6));
        grid[3][8] = new Bing(Player.BLACK, new Position(3, 8));

        // --- 放置红方 (RED) 棋子 ---
        // 底线
        grid[9][0] = new Che(Player.RED, new Position(9, 0));
        grid[9][1] = new Ma(Player.RED, new Position(9, 1));
        grid[9][2] = new Xiang(Player.RED, new Position(9, 2));
        grid[9][3] = new Shi(Player.RED, new Position(9, 3));
        grid[9][4] = new Jiang(Player.RED, new Position(9, 4));
        grid[9][5] = new Shi(Player.RED, new Position(9, 5));
        grid[9][6] = new Xiang(Player.RED, new Position(9, 6));
        grid[9][7] = new Ma(Player.RED, new Position(9, 7));
        grid[9][8] = new Che(Player.RED, new Position(9, 8));
        // 炮
        grid[7][1] = new Pao(Player.RED, new Position(7, 1));
        grid[7][7] = new Pao(Player.RED, new Position(7, 7));
        // 兵
        grid[6][0] = new Bing(Player.RED, new Position(6, 0));
        grid[6][2] = new Bing(Player.RED, new Position(6, 2));
        grid[6][4] = new Bing(Player.RED, new Position(6, 4));
        grid[6][6] = new Bing(Player.RED, new Position(6, 6));
        grid[6][8] = new Bing(Player.RED, new Position(6, 8));
    }

    /**
     * 在指定位置放置一个棋子
     * (注意: 这个方法会覆盖该位置的任何东西)
     */
    public void setPiece(Piece piece) {
        if (piece == null) {
            return;
        }
        grid[piece.getPosition().row()][piece.getPosition().col()] = piece;
    }
    
    /**
     * 清空一个位置 (用于 movePiece)
     */
    public void clearPiece(Position pos) {
        if (pos.isValid()) {
            grid[pos.row()][pos.col()] = null;
        }
    }

    /**
     * 获取指定位置的棋子
     */
    public Piece getPiece(Position pos) {
        if (!pos.isValid()) {
            return null;
        }
        return grid[pos.row()][pos.col()];
    }

    /**
     * (辅助方法) 移动棋子
     * 这是 "Board" 级别的方法
     */
    public void movePiece(Position from, Position to) {
        Piece pieceToMove = getPiece(from);
        if (pieceToMove != null) {
            // 清空原位置
            grid[from.row()][from.col()] = null;
            
            // 放置到新位置
            pieceToMove.setPosition(to);
            grid[to.row()][to.col()] = pieceToMove;
        }
    }

    /* 打印当前棋盘状态到控制台 (修复版)
     */
    public void printBoard() {
        System.out.println("  0  1  2  3  4  5  6  7  8  (列)");
        System.out.println("---------------------------------");
        for (int row = 0; row < 10; row++) {
            System.out.print(row + "|");
            for (int col = 0; col < 9; col++) {
                Piece p = grid[row][col];
                if (p == null) {
                    System.out.print(" . "); // 3 字符宽
                } else {
                    // 根据红黑方打印不同颜色的名字
                    // 我们也把它变成 3 字符宽
                    if (p.getPlayer() == Player.RED) {
                        // \u001B[31m 是红色, \u001B[0m 是重置
                        System.out.print("\u001B[31m" + " " + p.getName() + " " + "\u001B[0m");
                    } else {
                        // \u001B[34m 是蓝色 (代替黑色)
                        System.out.print("\u001B[34m" + " " + p.getName() + " " + "\u001B[0m");
                    }
                }
            }
            System.out.println("|");
            
            // 打印 "河界"
            if (row == 4) {
                System.out.println(" |          楚 河 汉 界          |");
            }
        }
        System.out.println("---------------------------------");
    }
}