package com.dyouwang.xiangqi.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 所有消息的基类, 包含消息类型
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseMessage {
    public MessageType type;

    // Jackson 需要一个无参数构造函数
    public BaseMessage() {}

    public BaseMessage(MessageType type) {
        this.type = type;
    }
}