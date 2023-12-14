package com.example.demo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Message {
    private MessageType type;
    private String content;
    private String sender;
    public enum MessageType {
        PLAY,
        JOIN,
        LEAVE
    }
}
