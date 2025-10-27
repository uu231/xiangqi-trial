package com.dyouwang.xiangqi; // 确保这是你的包名

/**
 * 一个 "record" (数据类), 用来表示一步棋
 * 从 'from' 位置 移动到 'to' 位置
 */
public record Move(Position from, Position to) {
    
    /**
     * 辅助方法: 从 "e2e4" 这种字符串创建 Move
     * (我们暂时不用, 留给以后)
     * e.g., "0010" -> from(0,0) to(1,0)
     */
    public static Move fromString(String s) {
        if (s == null || s.length() != 4) {
            throw new IllegalArgumentException("Invalid move string: " + s);
        }
        try {
            int r1 = Integer.parseInt(s.substring(0, 1));
            int c1 = Integer.parseInt(s.substring(1, 2));
            int r2 = Integer.parseInt(s.substring(2, 3));
            int c2 = Integer.parseInt(s.substring(3, 4));
            return new Move(new Position(r1, c1), new Position(r2, c2));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid move string: " + s);
        }
    }
}