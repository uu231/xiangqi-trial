package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.ArrayList; // 新增 import
import java.util.List;

public class Game {

    private Board board;
    private Player currentPlayer;

    public Game() {
        this.board = new Board(); 
        this.currentPlayer = Player.RED; 
    }

    public Board getBoard() {
        return board;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * 【新方法】
     * 生成一个玩家 *所有* 真正合法的走法
     * (这个方法会过滤掉所有 "自杀" 走法)
     * @param player 要为其生成走法的玩家
     * @return 一个包含所有合法 Move 对象的列表
     */
    public List<Move> getAllValidMoves(Player player) {
        List<Move> allValidMoves = new ArrayList<>();

        // 1. 遍历棋盘
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Position fromPos = new Position(r, c);
                Piece piece = board.getPiece(fromPos);

                // 2. 如果是该玩家的棋子
                if (piece != null && piece.getPlayer() == player) {
                    
                    // 3. 获取它所有 "理论上" 的走法
                    List<Position> targetPositions = piece.getValidMoves(board);

                    // 4. 检查每一步 "理论走法" 是否会导致 "自杀"
                    for (Position toPos : targetPositions) {
                        Move move = new Move(fromPos, toPos);

                        // 5. 使用 "假设-检查-撤销" 逻辑
                        Piece capturedPiece = board.getPiece(toPos);
                        board.movePiece(fromPos, toPos); // 假设

                        boolean selfInCheck = isKingInCheck(player); // 检查

                        board.movePiece(toPos, fromPos); // 撤销
                        if (capturedPiece != null) {
                            board.setPiece(capturedPiece);
                        }

                        // 6. 如果不是自杀, 才加入列表
                        if (!selfInCheck) {
                            allValidMoves.add(move);
                        }
                    }
                }
            }
        }
        return allValidMoves;
    }


    /**
     * 【新方法】
     * 检查一个玩家是否被 "将死" (Checkmate)
     */
    public boolean isCheckmate(Player player) {
        // "将死" 的定义:
        // 1. 你 *必须* 正在被将军
        // 2. 你 *没有* 任何合法的走法 (getAllValidMoves 已经帮你过滤掉自杀棋了)
        
        if (!isKingInCheck(player)) {
            return false; // 没被将军, 不可能被将死
        }
        
        return getAllValidMoves(player).isEmpty();
    }
    
    /**
     * 【新方法】
     * 检查一个玩家是否被 "逼和/困毙" (Stalemate)
     */
    public boolean isStalemate(Player player) {
        // "逼和" 的定义:
        // 1. 你 *没有* 正在被将军
        // 2. 你 *没有* 任何合法的走法
        
        if (isKingInCheck(player)) {
            return false; // 正被将军, 这是将死, 不是逼和
        }
        
        return getAllValidMoves(player).isEmpty();
    }


    // --- (以下方法保持不变) ---

    /**
     * 核心方法: 检查一个玩家的 "将" 是否正被将军
     */
    public boolean isKingInCheck(Player playerToDefend) {
        // ... (你现有的代码, 保持不变) ...
        // 1. 找到这个玩家的 "将"
        Position kingPosition = findKing(playerToDefend);
        if (kingPosition == null) {
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
                    List<Position> moves = piece.getValidMoves(board);
                    // 6. 检查它的走法是否包含 "将" 的位置
                    if (moves.contains(kingPosition)) {
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
        // ... (你现有的代码, 保持不变) ...
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPiece(pos);
                if (piece != null && 
                    piece.getPlayer() == player && 
                    (piece instanceof Jiang)) { 
                    return pos;
                }
            }
        }
        return null; 
    }


    /**
     * 尝试执行一步棋 (V3 版本)
     * (这个方法现在变得 *非常* 简单了)
     */
    public boolean makeMove(Move move) {
        // 1. 拿到 'from' 位置的棋子
        Piece pieceToMove = board.getPiece(move.from());

        // 2. 检查基本合法性
        if (pieceToMove == null) {
            System.out.println("错误: 原位置 ( " + move.from() + " ) 没有棋子!");
            return false;
        }
        if (pieceToMove.getPlayer() != this.currentPlayer) {
            System.out.println("错误: 现在轮到 " + this.currentPlayer + " 走, 不能移动 " + pieceToMove.getPlayer() + " 的棋子!");
            return false;
        }

        // 3. 【重要】 检查: 这一步棋是否在 "所有合法走法" 列表里?
        //    (getAllValidMoves 已经帮我们处理了棋子规则 + 自杀规则)
        List<Move> allValidMoves = getAllValidMoves(this.currentPlayer);

        if (!allValidMoves.contains(move)) {
            System.out.println("错误: 非法走法!");
            System.out.println("(原因: 棋子规则不允许, 或者会导致 '帅' 被将军)");
            return false;
        }

        // 4. 【执行】 
        Piece capturedPiece = board.getPiece(move.to());
        if (capturedPiece != null) {
            System.out.println(pieceToMove.getPlayer() + " 的 " + pieceToMove.getName() + " 吃了 " + 
                               capturedPiece.getPlayer() + " 的 " + capturedPiece.getName() + "!");
        }
        board.movePiece(move.from(), move.to());

        // 5. 切换玩家
        this.currentPlayer = (this.currentPlayer == Player.RED) ? Player.BLACK : Player.RED;

        return true;
    }

// ... (This is after the end of your makeMove method) ...

    /**
     * 【新】假设走棋 (供 AI 使用)
     * 这个方法 *会* 改变棋盘, 但 *会* 切换玩家 (模拟回合交替)
     * @return 被吃的棋子 (可能是 null)
     */
    public Piece hypotheticalMove(Move move) {
        Piece capturedPiece = board.getPiece(move.to());
        board.movePiece(move.from(), move.to());
        
        // 关键: 我们 *手动* 切换玩家, 模拟回合交替
        this.currentPlayer = (this.currentPlayer == Player.RED) ? Player.BLACK : Player.RED;
        
        return capturedPiece;
    }

    /**
     * 【新】撤销走棋 (供 AI 使用)
     * 必须和 hypotheticalMove 成对使用
     */
    public void undoHypotheticalMove(Move move, Piece capturedPiece) {
        // 1. 把玩家切换回来
        this.currentPlayer = (this.currentPlayer == Player.RED) ? Player.BLACK : Player.RED;

        // 2. 把棋子移回去
        board.movePiece(move.to(), move.from());
        
        // 3. 如果有被吃的棋子, 把它放回去
        if (capturedPiece != null) {
            board.setPiece(capturedPiece);
        }
    }

    // ... (The rest of your Game.java file, like isKingInCheck, findKing, etc.) ...
}