package com.jblmj.aiagent.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jblmj.aiagent.model.QueryComplexity;
import com.jblmj.aiagent.model.SubTask;
import com.jblmj.aiagent.service.ComplexityAssessor;
import com.jblmj.aiagent.service.TaskDecomposer;
import com.jblmj.aiagent.skill.Skill;
import com.jblmj.aiagent.skill.SkillRegistry;
import com.jblmj.aiagent.tools.WeatherQueryTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流编排器 3.0 - 基于标准 Skill 架构
 *
 * 核心设计：
 * 1. Skill 是面向用户任务的功能单元（查天气、规划行程）
 * 2. Service 是框架层的能力（复杂度评估、任务分解）
 * 3. Tool 是原子能力（API 调用、数据库查询）
 *
 * 路由策略：
 * 1. 优先使用 Skill 处理（面向用户任务）
 * 2. 如果没有匹配的 Skill，降级到传统复杂度评估流程
 *
 * 面试要点：
 * - Skill 是面向用户的任务，不是"能力"或"中间件"
 * - ComplexityAssessor、TaskDecomposer 是 Service，不是 Skill
 * - 这符合标准的 Skill 定义：一个任务一个 Skill
 *
 * @author jblmj
 */
@Slf4j
@Component
public class WorkflowOrchestrator {

    @Resource
    private SkillRegistry skillRegistry;

    @Resource
    private WeatherQueryTool weatherQueryTool;

    @Resource
    private EnterpriseAssistantApp enterpriseAssistantApp;

    @Resource
    private ComplexityAssessor complexityAssessor;

    @Resource
    private TaskDecomposer taskDecomposer;

    @Resource
    @org.springframework.beans.factory.annotation.Qualifier("dashscopeChatModel")
    private ChatModel chatModel;

    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化 ChatClient
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 智能路由：根据用户意图选择执行策略
     *
     * 路由策略：
     * 1. 优先尝试使用 Skill 处理（面向用户任务）
     * 2. 如果没有匹配的 Skill，降级到传统复杂度评估流程
     *
     * @param query 用户查询
     * @param chatId 会话 ID
     * @return 响应结果
     */
    public String route(String query, String chatId) {
        log.info("========================================");
        log.info("工作流路由开始: {}", query);
        log.info("========================================");

        // 1. 尝试使用 Skill 处理
        Skill skill = skillRegistry.selectSkill(query);
        if (skill != null) {
            log.info("使用 Skill: {}", skill.getName());
            try {
                String result = skill.execute(query, chatId);
                log.info("Skill 执行成功");
                return result;
            } catch (Exception e) {
                log.error("Skill 执行失败，降级到传统流程", e);
                // 继续执行降级流程
            }
        }

        // 2. 没有匹配的 Skill，降级到传统复杂度评估流程
        log.info("未找到匹配的 Skill，使用传统复杂度评估流程");
        return routeByComplexity(query, chatId);
    }

    /**
     * 传统复杂度评估路由（降级方案）
     */
    private String routeByComplexity(String query, String chatId) {
        // 1. 评估查询复杂度
        QueryComplexity complexity = complexityAssessor.assess(query);
        log.info("复杂度评估结果: {}", complexity);

        // 2. 根据复杂度选择处理策略
        return switch (complexity) {
            case SIMPLE -> handleSimpleQuery(query, chatId);
            case MEDIUM -> handleMediumQuery(query, chatId);
            case COMPLEX -> handleComplexQuery(query, chatId);
        };
    }

    /**
     * 处理简单查询（SIMPLE）
     * 单一意图，单次工具调用
     */
    private String handleSimpleQuery(String query, String chatId) {
        log.info("执行策略: SIMPLE（单一意图，单次工具调用）");

        // 判断意图类型
        if (isWeatherQuery(query)) {
            return handleSimpleWeather(query, chatId);
        }

        // 其他简单查询 → 走 LLM 决策（RAG）
        log.info("非天气查询，走 LLM 决策流程");
        return enterpriseAssistantApp.doComprehensiveChat(query, chatId);
    }

    /**
     * 处理中等复杂查询（MEDIUM）
     * 单一意图，多次工具调用
     */
    private String handleMediumQuery(String query, String chatId) {
        log.info("执行策略: MEDIUM（单一意图，多次工具调用）");

        // 判断意图类型
        if (isWeatherQuery(query)) {
            return handleWeatherComparison(query, chatId);
        }

        // 其他中等复杂查询 → 走 LLM 决策
        log.info("非天气对比，走 LLM 决策流程");
        return enterpriseAssistantApp.doComprehensiveChat(query, chatId);
    }

    /**
     * 处理复杂查询（COMPLEX）
     * 多意图，需要任务分解 + 并行执行
     */
    private String handleComplexQuery(String query, String chatId) {
        log.info("执行策略: COMPLEX（多意图，任务分解 + 并行执行）");

        try {
            // 1. 任务分解
            List<SubTask> subTasks = taskDecomposer.decompose(query);
            log.info("任务分解完成，共 {} 个子任务", subTasks.size());

            // 2. 按依赖关系排序（拓扑排序）
            List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(subTasks);
            log.info("任务排序完成，共 {} 批次", batches.size());

            // 3. 按批次执行（每批次内的任务可以并行执行）
            Map<String, String> results = new HashMap<>();
            for (int i = 0; i < batches.size(); i++) {
                List<SubTask> batch = batches.get(i);
                log.info("执行第 {} 批次，共 {} 个任务", i + 1, batch.size());

                if (batch.size() == 1) {
                    // 单个任务，直接执行
                    SubTask task = batch.get(0);
                    String result = executeSubTask(task);
                    results.put(task.getTaskType() + "_" + task.getId(), result);
                    task.setResult(result);
                    task.setSuccess(true);
                } else {
                    // 多个任务，并行执行
                    executeTasksInParallel(batch, results);
                }
            }

            // 4. LLM 整合结果
            return integrateResults(query, results);

        } catch (Exception e) {
            log.error("复杂查询处理失败，降级为 LLM 决策", e);
            return enterpriseAssistantApp.doComprehensiveChat(query, chatId);
        }
    }

    /**
     * 并行执行多个任务
     */
    private void executeTasksInParallel(List<SubTask> tasks, Map<String, String> results) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (SubTask task : tasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.info("并行执行子任务: {} - {}", task.getTaskType(), task.getDescription());
                    String result = executeSubTask(task);
                    synchronized (results) {
                        results.put(task.getTaskType() + "_" + task.getId(), result);
                    }
                    task.setResult(result);
                    task.setSuccess(true);
                } catch (Exception e) {
                    log.error("子任务执行失败: {}", task.getDescription(), e);
                    task.setSuccess(false);
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("批次执行完成");
    }

    /**
     * 执行单个子任务
     */
    private String executeSubTask(SubTask task) {
        try {
            // 解析参数
            JsonNode params = objectMapper.readTree(task.getParameters());

            return switch (task.getTaskType()) {
                case "QUERY_WEATHER" -> {
                    String city = params.get("city").asText();
                    yield weatherQueryTool.queryWeather(city);
                }
                case "QUERY_CUSTOMER", "QUERY_POLICY", "QUERY_HOTEL" -> {
                    // 这些任务走 RAG 查询
                    String keyword = params.has("keyword") ? params.get("keyword").asText() : "";
                    yield enterpriseAssistantApp.doComprehensiveChat(keyword, "temp");
                }
                default -> "未知任务类型: " + task.getTaskType();
            };
        } catch (Exception e) {
            log.error("子任务执行失败: {}", task.getTaskType(), e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 整合所有子任务的结果
     */
    private String integrateResults(String query, Map<String, String> results) {
        StringBuilder context = new StringBuilder();
        results.forEach((type, result) -> {
            context.append(String.format("【%s】\n%s\n\n", type, result));
        });

        String prompt = String.format("""
                用户查询：%s

                已收集的信息：
                %s

                请根据以上信息，生成一份详细的回复，包括：
                1. 如果有天气信息，给出天气情况和穿衣建议
                2. 如果有客户信息，给出客户地址和联系方式
                3. 如果有路线信息，给出交通建议
                4. 整体的时间安排和注意事项
                """, query, context);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 判断是否为天气查询
     */
    private boolean isWeatherQuery(String query) {
        String[] weatherKeywords = {"天气", "温度", "带伞", "穿什么", "下雨"};
        for (String keyword : weatherKeywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理简单天气查询（预编排工作流）
     */
    private String handleSimpleWeather(String query, String chatId) {
        log.info("执行预编排工作流: 简单天气查询");

        // 1. 提取城市名
        String city = extractCity(query);
        if (city == null) {
            return "抱歉，我没有识别出您要查询的城市，请明确告诉我城市名称（如：北京、上海、杭州）。";
        }

        // 2. 直接调用天气工具
        String weatherInfo = weatherQueryTool.queryWeather(city);
        log.info("天气查询结果: {}", weatherInfo);

        // 3. 让 LLM 润色回复（不需要工具调用，只需要文本生成）
        String prompt = String.format("""
                用户询问：%s

                天气信息：%s

                请根据以上天气信息，用专业且友好的语气回答用户。
                如果用户问"需要带伞吗"，根据天气状况给出建议。
                如果用户问"穿什么衣服"，根据温度给出穿衣建议。
                """, query, weatherInfo);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 处理天气对比查询（预编排工作流）
     */
    private String handleWeatherComparison(String query, String chatId) {
        log.info("执行预编排工作流: 天气对比");

        // 1. 提取两个城市名
        String[] cities = extractCities(query);
        if (cities.length < 2) {
            return "抱歉，我没有识别出您要对比的两个城市，请明确告诉我（如：上海和广州）。";
        }

        // 2. 分别查询天气
        String weather1 = weatherQueryTool.queryWeather(cities[0]);
        String weather2 = weatherQueryTool.queryWeather(cities[1]);
        log.info("对比查询结果: {} vs {}", weather1, weather2);

        // 3. 让 LLM 对比分析
        String prompt = String.format("""
                用户询问：%s

                %s的天气：%s
                %s的天气：%s

                请对比两个城市的天气，从温度、天气状况、舒适度等角度分析，
                并根据用户的出差需求给出建议（如哪个城市更适合出差）。
                """, query, cities[0], weather1, cities[1], weather2);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 提取城市名（简单正则匹配）
     */
    private String extractCity(String query) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "成都", "西安", "南京", "武汉", "重庆"};
        for (String city : cities) {
            if (query.contains(city)) {
                return city;
            }
        }
        return null;
    }

    /**
     * 提取多个城市名
     */
    private String[] extractCities(String query) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "成都", "西安", "南京", "武汉", "重庆"};
        return java.util.Arrays.stream(cities)
                .filter(query::contains)
                .toArray(String[]::new);
    }
}
