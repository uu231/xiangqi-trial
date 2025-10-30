package com.dyouwang.xiangqi.messages;

public enum MessageType {
    // --- Client -> Server ---
    SEND_MOVE,      // 客户端发送走法
    JOIN_GAME,      // 客户端请求加入游戏 (暂不用)
    CREATE_GAME,    // 客户端请求创建游戏 (暂不用)

    // --- Server -> Client ---
    ASSIGN_PLAYER,
    GAME_STATE,     // 服务器发送当前游戏状态
    ERROR,          // 服务器发送错误信息
    GAME_OVER       // 服务器通知游戏结束 (暂不用)
}