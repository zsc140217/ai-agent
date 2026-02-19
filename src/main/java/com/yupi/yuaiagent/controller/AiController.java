package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.YuManus;
import com.yupi.yuaiagent.app.EnterpriseAssistantApp; // 注意这里的类名改了
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用：企业出差管家
     * 路径改为 /enterprise/chat/sync
     */
    @GetMapping("/enterprise/chat/sync")
    public String doChatWithEnterpriseSync(String message, String chatId) {
        return enterpriseAssistantApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用：企业出差管家
     * 路径改为 /enterprise/chat/sse
     */
    @GetMapping(value = "/enterprise/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithEnterpriseSSE(String message, String chatId) {
        return enterpriseAssistantApp.doChatByStream(message, chatId);
    }

    /**
     * 终极功能接口：RAG + MCP 地图综合调度
     * 这也是你面试最值得演示的接口
     */
    @GetMapping("/enterprise/chat/comprehensive")
    public String doComprehensiveChat(String message, String chatId) {
        return enterpriseAssistantApp.doComprehensiveChat(message, chatId);
    }

    /**
     * SSE 流式调用：SseEmitter 版本（更适合前端实时展示进度）
     */
    @GetMapping(value = "/enterprise/chat/sse_emitter")
    public SseEmitter doEnterpriseSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L);
        enterpriseAssistantApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    /**
     * 手撸的 ReAct 智能体接口（YuManus）
     * 建议这个也保留，可以演示你对 Agent 底层逻辑的掌握
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        YuManus yuManus = new YuManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }
}