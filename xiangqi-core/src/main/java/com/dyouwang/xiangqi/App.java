package com.dyouwang.xiangqi; // 确保这是你的包名

// (不需要 import List, Scanner 等)

public class App {

    // AI 思考的深度
    // 警告: 深度 5 可能会非常慢 (30-60 秒)!
    // 建议从 3 开始
    private static final int AI_SEARCH_DEPTH = 3;

    public static void main(String[] args) {
        runAiTest();
    }

    /**
     * 测试: AI 走第一步棋
     */
    public static void runAiTest() {
        System.out.println("=================================");
        System.out.println("      测试：AI Minimax (深度 " + AI_SEARCH_DEPTH + ")");
        System.out.println("=================================");
        
        Game game = new Game();
        AIEngine ai = new AIEngine();
        
        // 1. 打印开局棋盘
        System.out.println("--- 1. 开局 ---");
        game.getBoard().printBoard();
        System.out.println("轮到: " + game.getCurrentPlayer());

        // 2. AI 思考
        System.out.println("AI 正在思考 (深度 " + AI_SEARCH_DEPTH + ")... 这可能需要几秒钟...");
        long startTime = System.currentTimeMillis();
        
        Move aiMove = ai.findBestMove(game, AI_SEARCH_DEPTH);
        
        long endTime = System.currentTimeMillis();
        System.out.println("AI 思考用时: " + (endTime - startTime) + " 毫秒");

        // 3. AI 执行走法
        if (aiMove != null) {
            System.out.println("AI 选择的走法是: " + aiMove);
            game.makeMove(aiMove);
            
            System.out.println("\n--- 2. AI 走棋后 ---");
            game.getBoard().printBoard();
            System.out.println("轮到: " + game.getCurrentPlayer());
            
            // 检查 AI 是否将军了
            if (game.isKingInCheck(game.getCurrentPlayer())) {
                System.out.println("*******************");
                System.out.println("      将 军 ! (CHECK!)");
                System.out.println("*******************");
            }
            
        } else {
            System.out.println("错误: AI 无法找到任何走法!");
        }
    }
}