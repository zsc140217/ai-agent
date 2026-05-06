package com.jblmj.aiagent.controller;

import com.jblmj.aiagent.agent.JblmjManus;
import com.jblmj.aiagent.app.EnterpriseAssistantApp; // 注意这里的类名改了
import com.jblmj.aiagent.app.WorkflowOrchestrator;
import com.jblmj.aiagent.chatmemory.MemoryService;
import com.jblmj.aiagent.model.ExecutionMode;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private WorkflowOrchestrator workflowOrchestrator;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private MemoryService memoryService;

    /**
     * 同步调用：企业出差管家（集成记忆系统）
     * 路径改为 /enterprise/chat/sync
     */
    @GetMapping("/enterprise/chat/sync")
    public String doChatWithEnterpriseSync(
            @RequestParam String message,
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {

        // 1. 处理用户消息（更新工作记忆）
        memoryService.processUserMessage(userId, chatId, message);

        // 2. 调用LLM生成回复
        String response = enterpriseAssistantApp.doChat(message, chatId);

        // 3. 可选：会话结束时触发学习（这里简化为每次都学习）
        // 生产环境建议：每N次对话或用户明确结束会话时才学习
        // memoryService.learnFromConversation(userId, chatId);

        return response;
    }

    /**
     * SSE 流式调用：企业出差管家（集成记忆系统）
     * 路径改为 /enterprise/chat/sse
     */
    @GetMapping(value = "/enterprise/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithEnterpriseSSE(
            @RequestParam String message,
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {

        // 1. 处理用户消息（更新工作记忆）
        memoryService.processUserMessage(userId, chatId, message);

        // 2. 调用LLM生成回复（流式），并在结束时发送 [DONE] 标记
        return enterpriseAssistantApp.doChatByStream(message, chatId)
                .concatWith(Flux.just("[DONE]"));

        // 注意：流式响应结束后无法同步触发学习，需要前端调用 /api/memory/learn 接口
        // 或者使用 doOnComplete() 回调
    }

    /**
     * 终极功能接口：RAG + MCP 地图综合调度（集成记忆系统）
     * 这也是你面试最值得演示的接口
     */
    @GetMapping("/enterprise/chat/comprehensive")
    public String doComprehensiveChat(
            @RequestParam String message,
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {

        // 1. 处理用户消息（更新工作记忆）
        memoryService.processUserMessage(userId, chatId, message);

        // 2. 调用LLM生成回复
        String response = enterpriseAssistantApp.doComprehensiveChat(message, chatId);

        // 3. 可选：触发学习
        // memoryService.learnFromConversation(userId, chatId);

        return response;
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
     * 手撸的 ReAct 智能体接口（JblmjManus）
     * 建议这个也保留，可以演示你对 Agent 底层逻辑的掌握
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        JblmjManus jblmjManus = new JblmjManus(allTools, dashscopeChatModel);
        return jblmjManus.runStream(message);
    }

    /**
     * 新增：支持用户主动选择执行模式的接口（集成记忆系统）
     *
     * @param message 用户消息
     * @param chatId 会话 ID
     * @param userId 用户 ID（用于长期记忆）
     * @param mode 执行模式（可选）：
     *             - "default" 或 "默认" 或 "快速"：默认模式（复杂度评估 + 并行执行，5-10秒）
     *             - "thinking" 或 "思考" 或 "详细"：思考模式（ReAct 循环，15-30秒）
     *             - 不传或传 null：默认使用 DEFAULT 模式
     * @return 响应结果
     *
     * 示例：
     * - 默认模式（快速）：/ai/enterprise/chat?message=规划去杭州出差&chatId=test123&userId=user001
     * - 默认模式（显式）：/ai/enterprise/chat?message=规划去杭州出差&chatId=test123&userId=user001&mode=default
     * - 思考模式（详细）：/ai/enterprise/chat?message=规划去杭州出差&chatId=test123&userId=user001&mode=thinking
     */
    @GetMapping("/enterprise/chat")
    public String doChatWithMode(
            @RequestParam String message,
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId,
            @RequestParam(required = false) String mode) {

        // 1. 处理用户消息（更新工作记忆）
        memoryService.processUserMessage(userId, chatId, message);

        // 2. 解析执行模式
        ExecutionMode executionMode = ExecutionMode.fromString(mode);

        // 3. 使用 WorkflowOrchestrator 路由
        String response = workflowOrchestrator.route(message, chatId, executionMode);

        // 4. 可选：触发学习
        // memoryService.learnFromConversation(userId, chatId);

        return response;
    }
}