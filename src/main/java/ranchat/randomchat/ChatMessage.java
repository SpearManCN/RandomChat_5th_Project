package ranchat.randomchat;

import lombok.Data;

import java.awt.*;

@Data
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private Integer totUser;
    private String receiver;
    private String brokerName;
}

enum MessageType{
    CHAT,
    JOIN,
    LEAVE
}