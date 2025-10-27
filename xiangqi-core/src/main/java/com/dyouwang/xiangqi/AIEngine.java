package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.List;

/**
 * AI 引擎 (大脑)
 * V2 版: 包含 Minimax 搜索
 */
public class AIEngine {

    // --- 评估函数的分值定义, 保持不变 ---
    private static final double JIANG_SCORE = 10000.0;
    private static final double CHE_SCORE = 9.0;
    private static final double PAO_SCORE = 5.0;
    private static final double MA_SCORE = 4.0;
    private static final double XIANG_SCORE = 2.0;
    private static final double SHI_SCORE = 2.0;
    private static final double BING_SCORE_BEFORE_RIVER = 1.0;
    private static final double BING_SCORE_AFTER_RIVER = 2.0;

    // 我们需要一个代表 "正无穷" 和 "负无穷" 的分数
    // (我们用一个大数, 而不是 Double.MAX_VALUE, 以避免溢出)
    private static final double INFINITY = 1000000.0;


    /**
     * 【新】AI 的主入口
     * 寻找最佳走法
     */
    public Move findBestMove(Game game, int depth) {
        Move bestMove = null;
        double bestScore = -INFINITY; // AI (Max) 想要最大化分数
        
        Player aiPlayer = game.getCurrentPlayer();
        List<Move> allMoves = game.getAllValidMoves(aiPlayer);
        java.util.Collections.shuffle(allMoves); // 引入随机性
        
        // 遍历所有 *顶层* 的走法
        for (Move move : allMoves) {
            
            // 1. 假设走棋 (会切换玩家)
            Piece captured = game.hypotheticalMove(move);

            // 2. "思考": 让 Minimax 评估 *对手 (Min)* 的最佳回应
            //    (注意: isMaximizingPlayer = false)
            double score = minimax(game, depth - 1, false, aiPlayer);

            // 3. 撤销走棋 (会切换玩家回来)
            game.undoHypotheticalMove(move, captured);

            // 4. 比较
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        System.out.println("AI 思考完毕, 最佳分数: " + bestScore);
        if (bestMove == null && allMoves.size() > 0) {
            // 如果所有走法都会导致输棋 (bestScore 还是 -INFINITY), 随便走一个
            return allMoves.get(0);
        }
        return bestMove;
    }


    /**
     * 【新】Minimax 递归函数
     */
    private double minimax(Game game, int depth, boolean isMaximizingPlayer, Player aiPlayer) {
        
        Player currentPlayer = game.getCurrentPlayer();
        
        // 1. 【递归基例】: 游戏结束 (将死/逼和)
        if (game.isCheckmate(currentPlayer)) {
            // 如果是 Max (AI) 被将死, 返回 -无穷
            // 如果是 Min (对手) 被将死, 返回 +无穷
            return isMaximizingPlayer ? -INFINITY : INFINITY;
        }
        if (game.isStalemate(currentPlayer)) {
            return 0.0; // 逼和
        }

        // 2. 【递归基例】: 到达搜索深度
        if (depth == 0) {
            // 返回 *静态评估* 分数 (始终从 AI 的视角)
            return AIEngine.evaluate(game.getBoard(), aiPlayer);
        }

        // 3. 【递归步骤】
        List<Move> allMoves = game.getAllValidMoves(currentPlayer);

        // A. 如果是 Max (AI) 思考...
        if (isMaximizingPlayer) {
            double bestScore = -INFINITY;
            for (Move move : allMoves) {
                Piece captured = game.hypotheticalMove(move);
                double score = minimax(game, depth - 1, false, aiPlayer); // 轮到 Min
                game.undoHypotheticalMove(move, captured);
                bestScore = Math.max(bestScore, score);
            }
            return bestScore;
        } 
        // B. 如果是 Min (对手) 思考...
        else {
            double bestScore = INFINITY;
            for (Move move : allMoves) {
                Piece captured = game.hypotheticalMove(move);
                double score = minimax(game, depth - 1, true, aiPlayer); // 轮到 Max
                game.undoHypotheticalMove(move, captured);
                bestScore = Math.min(bestScore, score);
            }
            return bestScore;
        }
    }


    // --- (评估函数, 保持不变) ---
    public static double evaluate(Board board, Player playerToEvaluate) {
        double totalScore = 0.0;

        // 遍历棋盘的每一个格子
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Piece piece = board.getPiece(new Position(r, c));

                if (piece != null) {
                    // 获取这个棋子的分数
                    double pieceScore = getPieceScore(piece);
                    
                    // 如果是 "我方" 棋子, 加分
                    if (piece.getPlayer() == playerToEvaluate) {
                        totalScore += pieceScore;
                    } 
                    // 如果是 "敌方" 棋子, 减分
                    else {
                        totalScore -= pieceScore;
                    }
                }
            }
        }
        return totalScore;
    }

    /**
     * 辅助方法: 获取单个棋子的分数
     */
    private static double getPieceScore(Piece p) {
        // "instanceof" 检查这个 "Piece" 到底是什么子类
        if (p instanceof Jiang) {
            return JIANG_SCORE;
        }
        if (p instanceof Che) {
            return CHE_SCORE;
        }
        if (p instanceof Pao) {
            return PAO_SCORE;
        }
        if (p instanceof Ma) {
            return MA_SCORE;
        }
        if (p instanceof Xiang) {
            return XIANG_SCORE;
        }
        if (p instanceof Shi) {
            return SHI_SCORE;
        }
        if (p instanceof Bing) {
            // "兵" 需要特殊处理
            Bing bing = (Bing) p; // 把 Piece 转换成 Bing
            if (bing.hasCrossedRiver()) { // 调用我们刚改成 public 的方法
                return BING_SCORE_AFTER_RIVER;
            } else {
                return BING_SCORE_BEFORE_RIVER;
            }
        }
        return 0.0; // 理论上不会到这里
    }
}