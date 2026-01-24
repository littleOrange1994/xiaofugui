package com.xiaofugui.controller;

import com.xiaofugui.dto.ChatRequest;
import com.xiaofugui.dto.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 聊天控制器
 * 接入 DeepSeek 提供聊天服务
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        // 构建 ChatClient，可添加记忆功能
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    /**
     * 同步聊天接口
     *
     * @param request 聊天请求
     * @return 完整的 AI 响应
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response;
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            response = chatClient.prompt()
                    .system(request.getSystemPrompt())
                    .user(request.getMessage())
                    .call()
                    .content();
        } else {
            response = chatClient.prompt()
                    .user(request.getMessage())
                    .call()
                    .content();
        }
        return Result.success(response);
    }

    /**
     * 流式聊天接口（SSE）
     *
     * @param request 聊天请求
     * @return 流式响应
     */
    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            return chatClient.prompt()
                    .system(request.getSystemPrompt())
                    .user(request.getMessage())
                    .stream()
                    .content();
        } else {
            return chatClient.prompt()
                    .user(request.getMessage())
                    .stream()
                    .content();
        }
    }

    /**
     * 简单 GET 接口，方便测试
     *
     * @param message 用户消息
     * @return AI 响应
     */
    @GetMapping("/chatGet")
    public Result<String> chatGet(@RequestParam String message) {
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();
        return Result.success(response);
    }
}
