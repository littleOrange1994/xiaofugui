package com.xiaofugui.controller;

import com.xiaofugui.dto.ChatRequest;
import com.xiaofugui.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 聊天控制器
 *
 * <p>该控制器提供与 AI 大语言模型（DeepSeek）交互的 HTTP 接口，支持同步和流式两种响应模式。
 * 通过 Spring AI 框架集成 DeepSeek API，实现智能对话功能。</p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *     <li>同步聊天：一次性返回完整的 AI 响应内容</li>
 *     <li>流式聊天：通过 SSE（Server-Sent Events）实时推送响应，提升用户体验</li>
 *     <li>会话记忆：基于内存的对话历史存储，支持多轮上下文理解</li>
 *     <li>系统提示词：支持自定义系统角色设定，定制 AI 行为</li>
 * </ul>
 *
 * <h3>接口列表：</h3>
 * <ul>
 *     <li>POST /api/chat - 同步聊天接口</li>
 *     <li>POST /api/chatStream - 流式聊天接口（SSE）</li>
 *     <li>GET /api/chatGet - 简单测试接口</li>
 * </ul>
 *
 * @author xiaofugui
 * @see ChatClient
 * @see MessageChatMemoryAdvisor
 */
@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    /**
     * Spring AI 聊天客户端
     *
     * <p>用于与 AI 模型进行交互，已配置会话记忆功能。</p>
     */
    private final ChatClient chatClient;

    /**
     * 构造函数，初始化聊天客户端
     *
     * <p>通过 Spring AI 框架自动注入的构建器创建 ChatClient 实例，
     * 并配置 {@link MessageChatMemoryAdvisor} 实现多轮对话的上下文记忆。</p>
     *
     * <p>记忆存储采用 {@link InMemoryChatMemory}，对话历史保存在内存中，
     * 应用重启后会话记录将丢失。</p>
     *
     * @param chatClientBuilder Spring AI 自动注入的 ChatClient 构建器（框架强制要求）
     */
    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    /**
     * 同步聊天接口
     *
     * <p>接收用户消息，调用 AI 模型生成响应，等待完整响应后一次性返回。
     * 适用于对响应时效性要求不高、需要完整响应内容的场景。</p>
     *
     * <h4>请求示例：</h4>
     * <pre>{@code
     * POST /api/chat
     * Content-Type: application/json
     *
     * {
     *     "message": "你好，请介绍一下自己",
     *     "systemPrompt": "你是一个友好的助手"  // 可选
     * }
     * }</pre>
     *
     * <h4>响应示例：</h4>
     * <pre>{@code
     * {
     *     "code": 200,
     *     "message": "success",
     *     "data": "你好！我是 AI 助手..."
     * }
     * }</pre>
     *
     * @param request 聊天请求对象，包含用户消息和可选的系统提示词
     * @return 封装了 AI 完整响应内容的统一结果对象
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
     * <p>基于 Server-Sent Events 技术实现流式响应，AI 生成的内容会实时推送到客户端。
     * 相比同步接口，流式接口能显著降低用户感知的响应延迟，提供更好的交互体验。</p>
     *
     * <h4>技术特点：</h4>
     * <ul>
     *     <li>响应类型：text/event-stream</li>
     *     <li>响应方式：逐字/逐词推送，无需等待完整响应</li>
     *     <li>底层实现：基于 Project Reactor 的 Flux 响应式流</li>
     * </ul>
     *
     * <h4>请求示例：</h4>
     * <pre>{@code
     * POST /api/chatStream
     * Content-Type: application/json
     *
     * {
     *     "message": "请写一首关于春天的诗",
     *     "systemPrompt": "你是一位诗人"  // 可选
     * }
     * }</pre>
     *
     * <h4>响应示例（SSE 格式）：</h4>
     * <pre>{@code
     * data: 春
     * data: 风
     * data: 拂
     * data: 面
     * ...
     * }</pre>
     *
     * @param request 聊天请求对象，包含用户消息和可选的系统提示词
     * @return 响应式字符串流，每个元素为 AI 响应的一个片段
     */
    @PostMapping(value = "/chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        log.info("chatStream 请求 - message: {}", request.getMessage());
        StringBuilder fullContent = new StringBuilder();

        Flux<String> responseFlux;
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            responseFlux = chatClient.prompt()
                    .system(request.getSystemPrompt())
                    .user(request.getMessage())
                    .stream()
                    .content();
        } else {
            responseFlux = chatClient.prompt()
                    .user(request.getMessage())
                    .stream()
                    .content();
        }

        return responseFlux
                .doOnNext(fullContent::append)
                .map(content -> content.replace("\n", "[BR]"))
                .doOnComplete(() -> log.info("chatStream 完整响应:\n{}", fullContent.toString()));
    }

    /**
     * 简单 GET 聊天接口
     *
     * <p>提供简化的 GET 请求方式进行 AI 对话，主要用于快速测试和调试。
     * 不支持系统提示词设置，仅接收用户消息参数。</p>
     *
     * <h4>使用场景：</h4>
     * <ul>
     *     <li>浏览器地址栏直接访问测试</li>
     *     <li>简单的 API 连通性验证</li>
     *     <li>快速原型开发和调试</li>
     * </ul>
     *
     * <h4>请求示例：</h4>
     * <pre>{@code
     * GET /api/chatGet?message=你好
     * }</pre>
     *
     * <h4>响应示例：</h4>
     * <pre>{@code
     * {
     *     "code": 200,
     *     "message": "success",
     *     "data": "你好！有什么可以帮助你的吗？"
     * }
     * }</pre>
     *
     * @param message 用户输入的消息文本，通过 URL 参数传递
     * @return 封装了 AI 响应内容的统一结果对象
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
