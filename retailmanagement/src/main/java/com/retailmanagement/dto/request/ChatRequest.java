package com.retailmanagement.dto.request    ;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;          // null = new session
    private List<ChatMessage> history; // optional: client-side history
}