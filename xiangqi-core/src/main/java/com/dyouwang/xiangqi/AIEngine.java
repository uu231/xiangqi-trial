package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.Collections; // 需要
import java.util.List;

/**
 * AI 引擎 (大脑)
 * V3 版: 包含 Alpha-Beta 剪枝
 */
public class AIEngine {

    // --- 评估函数的分值定义 (不变) ---
    private static final double JIANG_SCORE = 10000.0;
    private static final double CHE_SCORE = 9.0;
    private static final double PAO_SCORE = 5.0;
    private static final double MA_SCORE = 4.0;
    private static final double XIANG_SCORE = 2.0;
    private static final double SHI_SCORE = 2.0;
    private static final double BING_SCORE_BEFORE_RIVER = 1.0;
    private static final double BING_SCORE_AFTER_RIVER = 2.0;

    // --- 无穷大/小 (不变) ---
    private static final double INFINITY = 1000000.0;


    /**
     * 【修改版】 AI 的主入口, 调用 Alpha-Beta
     */
    public Move findBestMove(Game game, int depth) {
        Move bestMove = null;
        double bestScore = -INFINITY; // AI (Max) 想要最大化分数
        
        Player aiPlayer = game.getCurrentPlayer();
        List<Move> allMoves = game.getAllValidMoves(aiPlayer);
        Collections.shuffle(allMoves); // 引入随机性

        double alpha = -INFINITY; // Alpha-Beta 的 Alpha
        double beta = INFINITY;  // Alpha-Beta 的 Beta
        
        // 遍历所有 *顶层* 的走法
        for (Move move : allMoves) {
            
            // 1. 假设走棋 (会切换玩家)
            Piece captured = game.hypotheticalMove(move);

            // 2. "思考": 让 Minimax 评估 *对手 (Min)* 的最佳回应
            //    (注意: isMaximizingPlayer = false)
            //    我们把当前的 alpha 传下去
            double score = minimax(game, depth - 1, alpha, beta, false, aiPlayer);

            // 3. 撤销走棋 (会切换玩家回来)
            game.undoHypotheticalMove(move, captured);

            // 4. 比较 (在顶层)
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            
            // 5. 【修改】在顶层也要更新 alpha
            alpha = Math.max(alpha, bestScore);
        }
        
        System.out.println("AI 思考完毕, 最佳分数: " + bestScore);
        if (bestMove == null && allMoves.size() > 0) {
            return allMoves.get(0);
        }
        return bestMove;
    }


    /**
     * 【Alpha-Beta 剪枝版】 Minimax 递归函数
     * @param game 游戏状态
     * @param depth 剩余深度
     * @param alpha Max 玩家的最好分数
     * @param beta Min 玩家的最好分数
     * @param isMaximizingPlayer (是 Max (AI) 在思考, 还是 Min (对手) 在思考?)
     * @param aiPlayer (AI 自己的身份, 用于评估)
     * @return 这个局面的评估分数
     */
    private double minimax(Game game, int depth, double alpha, double beta, boolean isMaximizingPlayer, Player aiPlayer) {
        
        Player currentPlayer = game.getCurrentPlayer();
        
        // 1. 【递归基例】: 游戏结束 (将死/逼和)
        if (game.isCheckmate(currentPlayer)) {
            return isMaximizingPlayer ? -INFINITY : INFINITY;
        }
        if (game.isStalemate(currentPlayer)) {
            return 0.0; // 逼和
        }

        // 2. 【递归基例】: 到达搜索深度
        if (depth == 0) {
            return AIEngine.evaluate(game.getBoard(), aiPlayer); // 静态评估
        }

        // 3. 【递归步骤】
        List<Move> allMoves = game.getAllValidMoves(currentPlayer);

        // A. 如果是 Max (AI) 思考...
        if (isMaximizingPlayer) {
            double maxEval = -INFINITY;
            for (Move move : allMoves) {
                Piece captured = game.hypotheticalMove(move);
                double eval = minimax(game, depth - 1, alpha, beta, false, aiPlayer); // 轮到 Min
                game.undoHypotheticalMove(move, captured);
                
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, maxEval); // 更新 Alpha
                
                // 【剪枝】
                if (beta <= alpha) {
                    break; // Min 玩家在上一层有更好的选择, 不用再看了
                }
            }
            return maxEval;
        } 
        // B. 如果是 Min (对手) 思考...
        else {
            double minEval = INFINITY;
            for (Move move : allMoves) {
                Piece captured = game.hypotheticalMove(move);
                double eval = minimax(game, depth - 1, alpha, beta, true, aiPlayer); // 轮到 Max
                game.undoHypotheticalMove(move, captured);
                
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, minEval); // 更新 Beta
                
                // 【剪枝】
                if (beta <= alpha) {
                    break; // Max 玩家在上一层有更好的选择, 不用再看了
                }
            }
            return minEval;
        }
    }


    // --- (评估函数 evaluate, getPieceScore, 保持不变) ---
    public static double evaluate(Board board, Player playerToEvaluate) {
        double totalScore = 0.0;
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Piece piece = board.getPiece(new Position(r, c));
                if (piece != null) {
                    double pieceScore = getPieceScore(piece);
                    if (piece.getPlayer() == playerToEvaluate) {
                        totalScore += pieceScore;
                    } else {
                        totalScore -= pieceScore;
                    }
                }
            }
        }
        return totalScore;
    }
    private static double getPieceScore(Piece p) {
        if (p instanceof Jiang) return JIANG_SCORE;
        if (p instanceof Che) return CHE_SCORE;
        if (p instanceof Pao) return PAO_SCORE;
        if (p instanceof Ma) return MA_SCORE;
        if (p instanceof Xiang) return XIANG_SCORE;
        if (p instanceof Shi) return SHI_SCORE;
        if (p instanceof Bing) {
            Bing bing = (Bing) p;
            if (bing.hasCrossedRiver()) {
                return BING_SCORE_AFTER_RIVER;
            } else {
                return BING_SCORE_BEFORE_RIVER;
            }
        }
        return 0.0;
    }
}