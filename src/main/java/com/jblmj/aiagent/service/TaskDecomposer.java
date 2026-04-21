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
     * 构建任务分解的 Prompt（优化版：支持任务依赖）
     */
    private String buildDecomposePrompt(String query) {
        return String.format("""
                你是一个任务规划专家，请将用户的复杂查询分解为多个子任务，并标注任务之间的依赖关系。

                可用的任务类型：
                1. QUERY_WEATHER: 查询天气（参数：city）
                2. QUERY_ROUTE: 查询路线（参数：origin, destination）
                3. QUERY_CUSTOMER: 查询客户信息（参数：keyword）
                4. QUERY_POLICY: 查询差旅政策（参数：keyword）
                5. QUERY_HOTEL: 查询酒店推荐（参数：city）

                任务依赖规则：
                - 如果任务 B 需要任务 A 的结果，则 B 依赖 A（在 dependsOn 中填写 A 的 id）
                - 例如：查询路线需要先知道客户地址，所以路线查询依赖客户查询
                - 没有依赖关系的任务可以并行执行

                请按照以下 JSON 格式输出（只输出 JSON，不要其他内容）：
                [
                  {
                    "id": 0,
                    "taskType": "QUERY_WEATHER",
                    "description": "查询杭州天气",
                    "parameters": "{\\"city\\": \\"杭州\\"}",
                    "dependsOn": [],
                    "priority": 0
                  },
                  {
                    "id": 1,
                    "taskType": "QUERY_CUSTOMER",
                    "description": "查询客户公司地址",
                    "parameters": "{\\"keyword\\": \\"阿里巴巴\\"}",
                    "dependsOn": [],
                    "priority": 0
                  },
                  {
                    "id": 2,
                    "taskType": "QUERY_ROUTE",
                    "description": "查询从酒店到客户公司的路线",
                    "parameters": "{\\"origin\\": \\"杭州西湖区\\", \\"destination\\": \\"阿里巴巴\\"}",
                    "dependsOn": [1],
                    "priority": 1
                  }
                ]

                示例 1：
                用户查询："明天去杭州出差，查一下天气，还要拜访阿里巴巴，帮我规划一下路线"
                分解结果：
                - 任务 0：查询杭州天气（无依赖，可并行）
                - 任务 1：查询阿里巴巴地址（无依赖，可并行）
                - 任务 2：查询路线（依赖任务 1，因为需要知道目的地地址）

                示例 2：
                用户查询："去北京出差，住宿标准是多少"
                分解结果：
                - 任务 0：查询北京住宿标准（无依赖）

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

            // 验证任务依赖关系（检测循环依赖）
            validateTaskDependencies(tasks);

            log.info("任务分解成功，共 {} 个子任务", tasks.size());
            for (SubTask task : tasks) {
                log.info("  - 任务 {}: {} (依赖: {})", task.getId(), task.getDescription(), task.getDependsOn());
            }

            return tasks;

        } catch (Exception e) {
            log.error("解析任务列表失败: {}", response, e);
            throw new RuntimeException("解析任务列表失败", e);
        }
    }

    /**
     * 验证任务依赖关系（检测循环依赖）
     */
    private void validateTaskDependencies(List<SubTask> tasks) {
        for (SubTask task : tasks) {
            if (hasCyclicDependency(task, tasks, new ArrayList<>())) {
                log.error("检测到循环依赖: 任务 {} - {}", task.getId(), task.getDescription());
                throw new RuntimeException("任务依赖关系存在循环: " + task.getDescription());
            }
        }
    }

    /**
     * 检测循环依赖（深度优先搜索）
     */
    private boolean hasCyclicDependency(SubTask task, List<SubTask> allTasks, List<Integer> visited) {
        if (visited.contains(task.getId())) {
            return true;  // 发现循环
        }

        visited.add(task.getId());

        if (task.getDependsOn() != null) {
            for (int depId : task.getDependsOn()) {
                SubTask depTask = allTasks.stream()
                        .filter(t -> t.getId() == depId)
                        .findFirst()
                        .orElse(null);

                if (depTask != null && hasCyclicDependency(depTask, allTasks, new ArrayList<>(visited))) {
                    return true;
                }
            }
        }

        return false;
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
        task.setId(0);
        task.setTaskType("QUERY_POLICY");  // 默认走 RAG 查询
        task.setDescription("查询差旅政策");
        task.setParameters("{\"keyword\": \"" + query + "\"}");
        task.setDependsOn(new ArrayList<>());
        task.setPriority(0);
        task.setSuccess(false);

        tasks.add(task);

        return tasks;
    }

    /**
     * 按依赖关系排序任务（拓扑排序）
     * 返回可以按顺序执行的任务列表
     */
    public List<List<SubTask>> sortTasksByDependency(List<SubTask> tasks) {
        List<List<SubTask>> result = new ArrayList<>();
        List<SubTask> remaining = new ArrayList<>(tasks);
        List<SubTask> completed = new ArrayList<>();

        while (!remaining.isEmpty()) {
            // 找出当前可以执行的任务（所有依赖都已完成）
            List<SubTask> currentBatch = new ArrayList<>();
            for (SubTask task : remaining) {
                if (canExecuteNow(task, completed)) {
                    currentBatch.add(task);
                }
            }

            if (currentBatch.isEmpty()) {
                log.error("无法继续执行，可能存在循环依赖或依赖的任务不存在");
                log.error("剩余任务: {}", remaining.stream().map(t -> "任务" + t.getId()).toList());
                log.error("已完成任务: {}", completed.stream().map(t -> "任务" + t.getId()).toList());
                break;
            }

            // 按优先级排序
            currentBatch.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));

            result.add(currentBatch);
            completed.addAll(currentBatch);
            remaining.removeAll(currentBatch);
        }

        log.info("任务排序完成，共 {} 批次", result.size());
        for (int i = 0; i < result.size(); i++) {
            log.info("  批次 {}: {} 个任务可并行执行", i, result.get(i).size());
        }

        return result;
    }

    /**
     * 判断任务是否可以执行（所有依赖都已完成）
     */
    private boolean canExecuteNow(SubTask task, List<SubTask> completedTasks) {
        if (task.getDependsOn() == null || task.getDependsOn().isEmpty()) {
            return true;
        }

        // 检查所有依赖的任务是否都在 completedTasks 列表中
        for (int depId : task.getDependsOn()) {
            boolean depCompleted = completedTasks.stream()
                    .anyMatch(t -> t.getId() == depId);
            if (!depCompleted) {
                return false;
            }
        }

        return true;
    }
}
