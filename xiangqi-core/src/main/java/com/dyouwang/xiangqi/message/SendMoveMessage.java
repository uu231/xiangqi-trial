package com.dyouwang.xiangqi.messages;

import com.dyouwang.xiangqi.Move; // 需要导入 Move

// 客户端发送走法时使用
public class SendMoveMessage extends BaseMessage {
    public Move move; // 包含具体的走法信息

    public SendMoveMessage() {
        super(MessageType.SEND_MOVE);
    }

    public SendMoveMessage(Move move) {
        this(); // 调用无参数构造函数设置 type
        this.move = move;
    }
}