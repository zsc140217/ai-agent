package com.jblmj.aiagent.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jblmj.aiagent.model.QueryComplexity;
import com.jblmj.aiagent.model.SubTask;
import com.jblmj.aiagent.service.ComplexityAssessor;
import com.jblmj.aiagent.service.TaskDecomposer;
import com.jblmj.aiagent.tools.WeatherQueryTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流编排器 2.0
 *
 * 核心升级：
 * 1. 集成复杂度评估框架（ComplexityAssessor）
 * 2. 集成任务分解器（TaskDecomposer）
 * 3. 根据复杂度自动选择处理策略
 *
 * 三种处理策略：
 * - SIMPLE: 关键词匹配 + 预编排（单次工具调用）
 * - MEDIUM: 关键词匹配 + 预编排（多次工具调用）
 * - COMPLEX: 任务分解 + 依次执行 + LLM 整合
 *
 * 面试价值：
 * - 展示完整的 Agent 架构设计能力
 * - 体现对不同复杂度场景的处理策略
 * - 证明你理解如何平衡智能性和稳定性
 *
 * @author jblmj
 */
@Slf4j
@Component
public class WorkflowOrchestrator {

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
     * @param query 用户查询
     * @param chatId 会话 ID
     * @return 响应结果
     */
    public String route(String query, String chatId) {
        log.info("========================================");
        log.info("工作流路由开始: {}", query);
        log.info("========================================");

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
     * 多意图，需要任务分解
     */
    private String handleComplexQuery(String query, String chatId) {
        log.info("执行策略: COMPLEX（多意图，任务分解）");

        try {
            // 1. 任务分解
            List<SubTask> subTasks = taskDecomposer.decompose(query);
            log.info("任务分解完成，共 {} 个子任务", subTasks.size());

            // 2. 依次执行子任务
            Map<String, String> results = new HashMap<>();
            for (SubTask task : subTasks) {
                log.info("执行子任务: {} - {}", task.getTaskType(), task.getDescription());
                String result = executeSubTask(task);
                results.put(task.getTaskType(), result);
                task.setResult(result);
                task.setSuccess(true);
            }

            // 3. LLM 整合结果
            return integrateResults(query, results);

        } catch (Exception e) {
            log.error("复杂查询处理失败，降级为 LLM 决策", e);
            return enterpriseAssistantApp.doComprehensiveChat(query, chatId);
        }
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
