package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.List;

public class Game {

    private Board board;
    private Player currentPlayer;

    public Game() {
        this.board = new Board(); // Board 构造函数会自动摆好棋子
        this.currentPlayer = Player.RED; // 红方先走
    }

    public Board getBoard() {
        return board;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * 核心方法: 检查一个玩家的 "将" 是否正被将军
     * @param playerToDefend (要防守的玩家, e.g., RED)
     * @return true 如果 "将" 处于被攻击状态
     */
    public boolean isKingInCheck(Player playerToDefend) {
        // 1. 找到这个玩家的 "将"
        Position kingPosition = findKing(playerToDefend);
        if (kingPosition == null) {
            // 理论上不可能, 但作为安全检查
            return false; 
        }

        // 2. 找出攻击方
        Player attackingPlayer = (playerToDefend == Player.RED) ? Player.BLACK : Player.RED;

        // 3. 遍历棋盘上 *所有* 的棋子
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Piece piece = board.getPiece(new Position(r, c));

                // 4. 如果这个棋子是 *攻击方* 的
                if (piece != null && piece.getPlayer() == attackingPlayer) {
                    
                    // 5. 获取它的所有 "合法" 走法
                    // (注意: getValidMoves 已经包含了吃子!)
                    List<Position> moves = piece.getValidMoves(board);

                    // 6. 检查它的走法是否包含 "将" 的位置
                    if (moves.contains(kingPosition)) {
                        // 找到了一个威胁!
                        // System.out.println("威胁来自: " + piece.getName() + " at " + piece.getPosition());
                        return true;
                    }
                }
            }
        }

        // 7. 遍历完所有棋子, 没发现威胁
        return false;
    }

    /**
     * 辅助方法: 找到指定玩家的 "将" 或 "帅"
     */
    private Position findKing(Player player) {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPiece(pos);
                if (piece != null && 
                    piece.getPlayer() == player && 
                    (piece instanceof Jiang)) { // "instanceof" 检查它是不是 Jiang 类
                    return pos;
                }
            }
        }
        return null; // 找不到 "将" (游戏已结束?)
    }


    /**
     * 尝试执行一步棋 (V2 版本 - 包含将军检查)
     * @param move 要走的棋
     * @return true 如果走法合法, false 如果非法
     */
    public boolean makeMove(Move move) {
        // 1. 拿到 'from' 位置的棋子
        Piece pieceToMove = board.getPiece(move.from());

        // 2. 检查基本合法性 (和 V1 一样)
        if (pieceToMove == null) {
            System.out.println("错误: 原位置 ( " + move.from() + " ) 没有棋子!");
            return false;
        }
        if (pieceToMove.getPlayer() != this.currentPlayer) {
            System.out.println("错误: 现在轮到 " + this.currentPlayer + " 走, 不能移动 " + pieceToMove.getPlayer() + " 的棋子!");
            return false;
        }

        // 3. 检查棋子规则 (和 V1 一样)
        List<Position> validMoves = pieceToMove.getValidMoves(board);
        if (!validMoves.contains(move.to())) {
            System.out.println("错误: 非法走法! " + pieceToMove.getName() + " 不能从 " + move.from() + " 走到 " + move.to());
            return false;
        }

        // 4. 【重要】 检查: 走了这步棋后, 自己是否被将军?
        // 我们使用 "假设-检查-撤销" (Hypothetical-Check-Undo) 逻辑
        
        // 4a. 记住 "to" 位置的棋子 (可能是 null, 也可能是敌人)
        Piece capturedPiece = board.getPiece(move.to());

        // 4b. 假设 (Hypothetical): 先把棋走了
        board.movePiece(move.from(), move.to());

        // 4c. 检查 (Check): 走了之后, 我 (currentPlayer) 是不是被将军了?
        boolean selfInCheck = isKingInCheck(this.currentPlayer);

        // 4d. 撤销 (Undo): 无论如何, 先把棋盘恢复原状
        board.movePiece(move.to(), move.from()); // 把棋子移回去
        if (capturedPiece != null) {
            board.setPiece(capturedPiece); // 把被吃的棋子放回去
        }
        
        // 4e. 判断: 如果是"自杀"走法, 则非法
        if (selfInCheck) {
            System.out.println("错误: 非法走法! " + this.currentPlayer + " 的 '帅' 会被将军!");
            return false;
        }

        // 5. 【执行】 既然走法合法, 正式执行
        if (capturedPiece != null) {
            System.out.println(pieceToMove.getPlayer() + " 的 " + pieceToMove.getName() + " 吃了 " + 
                               capturedPiece.getPlayer() + " 的 " + capturedPiece.getName() + "!");
        }
        board.movePiece(move.from(), move.to());

        // 6. 切换玩家
        this.currentPlayer = (this.currentPlayer == Player.RED) ? Player.BLACK : Player.RED;

        return true;
    }
}