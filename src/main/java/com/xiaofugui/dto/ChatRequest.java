package com.xiaofugui.dto;

import lombok.Data;

/**
 * AI 聊天请求 DTO
 */
@Data
public class ChatRequest {

    /**
     * 用户消息
     */
    private String message;

    /**
     * 系统提示词（可选）
     */
    private String systemPrompt;
}
