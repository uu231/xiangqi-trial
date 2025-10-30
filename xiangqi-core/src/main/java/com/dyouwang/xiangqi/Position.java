package com.dyouwang.xiangqi;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 用来表示棋盘上的位置 (行, 列)
 * 中国象棋棋盘是 10 行 (0-9) x 9 列 (0-8)
 */
public record Position(int row, int col) {
    /**
     * 检查此位置是否在棋盘范围内
     */
    @JsonIgnore
    public boolean isValid() {
        // 10 行 (0-9)
        if (row < 0 || row > 9) {
            return false;
        }
        // 9 列 (0-8)
        if (col < 0 || col > 8) {
            return false;
        }
        return true;
    }
}