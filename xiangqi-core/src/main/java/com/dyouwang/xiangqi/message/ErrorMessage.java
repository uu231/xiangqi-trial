package com.dyouwang.xiangqi.messages;

public class ErrorMessage extends BaseMessage {
    public String message;

    public ErrorMessage() {
        super(MessageType.ERROR);
    }

    public ErrorMessage(String message) {
        this();
        this.message = message;
    }
}