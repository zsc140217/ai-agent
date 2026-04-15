package com.jblmj.aiagent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jblmj.aiagent.model.SubTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务分解器
 *
 * 核心功能：
 * 1. 将复杂查询分解为多个子任务
 * 2. 使用 LLM 生成结构化的任务列表（JSON 格式）
 * 3. 每个子任务包含：任务类型、描述、参数
 *
 * 面试价值：
 * - 展示如何用 LLM 做结构化输出（JSON Schema）
 * - 体现对复杂任务的分解能力
 * - 证明你理解 Agent 的任务规划能力
 *
 * @author jblmj
 */
@Service
@Slf4j
public class TaskDecomposer {

    @Resource
    @Qualifier("dashscopeChatModel")
    private ChatModel chatModel;

    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 分解复杂查询为子任务列表
     *
     * @param query 用户查询
     * @return 子任务列表
     */
    public List<SubTask> decompose(String query) {
        log.info("开始分解任务: {}", query);

        String prompt = buildDecomposePrompt(query);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.trim().isEmpty()) {
                log.error("LLM 返回空响应");
                return createFallbackTasks(query);
            }

            // 解析 JSON 响应
            List<SubTask> tasks = parseTasksFromResponse(response);

            log.info("任务分解完成，共 {} 个子任务", tasks.size());
            return tasks;

        } catch (Exception e) {
            log.error("任务分解失败，使用降级方案", e);
            return createFallbackTasks(query);
        }
    }

    /**
     * 构建任务分解的 Prompt
     */
    private String buildDecomposePrompt(String query) {
        return String.format("""
                你是一个任务规划专家，请将用户的复杂查询分解为多个子任务。

                可用的任务类型：
                1. QUERY_WEATHER: 查询天气（参数：city）
                2. QUERY_ROUTE: 查询路线（参数：from, to）
                3. QUERY_CUSTOMER: 查询客户信息（参数：keyword）
                4. QUERY_POLICY: 查询差旅政策（参数：keyword）
                5. QUERY_HOTEL: 查询酒店推荐（参数：city）

                请按照以下 JSON 格式输出（只输出 JSON，不要其他内容）：
                [
                  {
                    "taskType": "QUERY_WEATHER",
                    "description": "查询杭州天气",
                    "parameters": "{\\"city\\": \\"杭州\\"}"
                  },
                  {
                    "taskType": "QUERY_CUSTOMER",
                    "description": "查询客户公司地址",
                    "parameters": "{\\"keyword\\": \\"阿里巴巴\\"}"
                  }
                ]

                用户查询：%s

                子任务列表：
                """, query);
    }

    /**
     * 从 LLM 响应中解析任务列表
     */
    private List<SubTask> parseTasksFromResponse(String response) {
        try {
            // 提取 JSON 部分（去除可能的 Markdown 代码块标记）
            String jsonContent = extractJsonContent(response);

            // 解析 JSON
            List<SubTask> tasks = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<SubTask>>() {}
            );

            // 初始化任务状态
            for (SubTask task : tasks) {
                task.setSuccess(false);
                task.setResult(null);
            }

            return tasks;

        } catch (Exception e) {
            log.error("解析任务列表失败: {}", response, e);
            throw new RuntimeException("解析任务列表失败", e);
        }
    }

    /**
     * 提取 JSON 内容（去除 Markdown 代码块标记）
     */
    private String extractJsonContent(String response) {
        String content = response.trim();

        // 去除 Markdown 代码块标记
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }

        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        return content.trim();
    }

    /**
     * 创建降级任务列表（当 LLM 分解失败时）
     */
    private List<SubTask> createFallbackTasks(String query) {
        log.warn("使用降级方案：将整个查询作为单个任务");

        List<SubTask> tasks = new ArrayList<>();

        SubTask task = new SubTask();
        task.setTaskType("QUERY_POLICY");  // 默认走 RAG 查询
        task.setDescription("查询差旅政策");
        task.setParameters("{\"keyword\": \"" + query + "\"}");
        task.setSuccess(false);

        tasks.add(task);

        return tasks;
    }
}
