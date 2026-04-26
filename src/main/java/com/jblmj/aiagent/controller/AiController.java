package com.jblmj.aiagent.controller;

import com.jblmj.aiagent.agent.JblmjManus;
import com.jblmj.aiagent.app.EnterpriseAssistantApp; // 注意这里的类名改了
import com.jblmj.aiagent.app.WorkflowOrchestrator;
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
     * 手撸的 ReAct 智能体接口（JblmjManus）
     * 建议这个也保留，可以演示你对 Agent 底层逻辑的掌握
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        JblmjManus jblmjManus = new JblmjManus(allTools, dashscopeChatModel);
        return jblmjManus.runStream(message);
    }

    /**
     * 新增：支持用户主动选择执行模式的接口
     *
     * @param message 用户消息
     * @param chatId 会话 ID
     * @param mode 执行模式（可选）：
     *             - "default" 或 "默认" 或 "快速"：默认模式（复杂度评估 + 并行执行，5-10秒）
     *             - "thinking" 或 "思考" 或 "详细"：思考模式（ReAct 循环，15-30秒）
     *             - 不传或传 null：默认使用 DEFAULT 模式
     * @return 响应结果
     *
     * 示例：
     * - 默认模式（快速）：/ai/enterprise/chat?message=规划去杭州出差&chatId=test123
     * - 默认模式（显式）：/ai/enterprise/chat?message=规划去杭州出差&chatId=test123&mode=default
     * - 思考模式（详细）：/ai/enterprise/chat?message=规划去杭州出差&chatId=test123&mode=thinking
     */
    @GetMapping("/enterprise/chat")
    public String doChatWithMode(
            @RequestParam String message,
            @RequestParam String chatId,
            @RequestParam(required = false) String mode) {

        // 解析执行模式
        ExecutionMode executionMode = ExecutionMode.fromString(mode);

        // 使用 WorkflowOrchestrator 路由
        return workflowOrchestrator.route(message, chatId, executionMode);
    }
}