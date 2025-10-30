package com.dyouwang.xiangqi.messages;

import com.dyouwang.xiangqi.Player; // 需要导入 Player

// 服务器告知客户端其角色
public class AssignPlayerMessage extends BaseMessage {
    public Player assignedPlayer; // 分配的角色 (RED 或 BLACK)

    public AssignPlayerMessage() {
        super(MessageType.ASSIGN_PLAYER);
    }

    public AssignPlayerMessage(Player assignedPlayer) {
        this();
        this.assignedPlayer = assignedPlayer;
    }
}