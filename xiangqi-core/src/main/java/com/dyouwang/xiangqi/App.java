package com.dyouwang.xiangqi; // 确保这是你的包名

import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        // 1. 创建游戏实例
        Game game = new Game();
        Scanner scanner = new Scanner(System.in);
        
        // 2. 游戏主循环
        while (true) {
        
            // 3. 打印当前棋盘
            game.getBoard().printBoard();
            
            // 4. 打印轮到谁走
            System.out.println("---------------------------------");
            System.out.println("轮到: " + game.getCurrentPlayer());
            
            // 5. (TODO: 检查将死或逼和)
            // if (game.isCheckmate(game.getCurrentPlayer())) { ... }

            // 6. 获取用户输入
            System.out.println("请输入走法 (例如: 9070 代表 (9,0) 走到 (7,0)):");
            String input = scanner.nextLine();
            
            if (input.equals("exit")) {
                System.out.println("游戏结束!");
                break;
            }

            // 7. 尝试解析并执行走法
            try {
                Move move = Move.fromString(input);
                boolean success = game.makeMove(move);
                
                if (success) {
                    System.out.println("走法成功!");
                    
                    // 【新功能】 检查是否 "将军" 了对方
                    if (game.isKingInCheck(game.getCurrentPlayer())) {
                        System.out.println("*******************");
                        System.out.println("      将 军 ! (CHECK!)");
                        System.out.println("*******************");
                    }

                } else {
                    System.out.println("走法失败, 请重试.");
                }

            } catch (IllegalArgumentException e) {
                System.out.println("输入格式错误, 请输入 4 个数字 (例如 9070). " + e.getMessage());
            } catch (Exception e) {
                System.out.println("发生未知错误: " + e.getMessage());
                e.printStackTrace(); // 打印详细错误
            }
        }
        
        scanner.close();
    }
}