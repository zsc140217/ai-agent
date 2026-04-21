package com.jblmj.aiagent.skill.business;

import com.jblmj.aiagent.model.QueryComplexity;
import com.jblmj.aiagent.model.SubTask;
import com.jblmj.aiagent.service.ComplexityAssessor;
import com.jblmj.aiagent.service.TaskDecomposer;
import com.jblmj.aiagent.skill.Skill;
import com.jblmj.aiagent.skill.SkillComponent;
import com.jblmj.aiagent.skill.SkillLayer;
import com.jblmj.aiagent.tools.WeatherQueryTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 差旅规划 Skill
 *
 * 面向用户任务：规划差旅行程
 *
 * 功能：
 * - 根据用户需求规划差旅行程
 * - 整合天气、路线、酒店、政策等信息
 * - 内部调用 Service（复杂度评估、任务分解）和 Tool（天气查询）
 *
 * 使用场景：
 * - "帮我规划明天去杭州的行程"
 * - "去深圳出差，查天气和推荐酒店"
 * - "规划北京3天出差，包括客户拜访"
 *
 * 设计说明：
 * - Skill 是面向用户的任务，不是"能力"或"中间件"
 * - 内部调用的 ComplexityAssessor、TaskDecomposer 是 Service，不是 Skill
 * - 这符合标准的 Skill 定义：一个任务一个 Skill
 *
 * @author jblmj
 */
@Slf4j
@SkillComponent(
        name = "travel_planning",
        description = "规划差旅行程，整合天气、路线、酒店、政策等信息",
        layer = SkillLayer.BUSINESS,
        keywords = {"规划", "行程", "出差", "安排", "计划", "准备"},
        priority = 60
)
public class TravelPlanningSkill implements Skill {

    // 注入 Service（不是 Skill）
    @Resource
    private ComplexityAssessor complexityAssessor;

    @Resource
    private TaskDecomposer taskDecomposer;

    @Resource
    private WeatherQueryTool weatherQueryTool;

    @Resource
    @org.springframework.beans.factory.annotation.Qualifier("dashscopeChatModel")
    private ChatModel chatModel;

    private ChatClient chatClient;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String getName() {
        return "travel_planning";
    }

    @Override
    public String getDescription() {
        return "规划差旅行程，整合天气、路线、酒店、政策等信息";
    }

    @Override
    public SkillLayer getLayer() {
        return SkillLayer.BUSINESS;
    }

    @Override
    public boolean canHandle(String query) {
        // 包含规划关键词
        String[] keywords = {"规划", "行程", "出差", "安排", "计划", "准备"};
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String execute(String query, String chatId) {
        log.info("[TravelPlanningSkill] 开始规划差旅行程: {}", query);

        try {
            // 1. 调用 Service：评估复杂度
            QueryComplexity complexity = complexityAssessor.assess(query);
            log.info("[TravelPlanningSkill] 复杂度: {}", complexity);

            // 2. 根据复杂度选择处理策略
            return switch (complexity) {
                case SIMPLE -> handleSimplePlanning(query, chatId);
                case MEDIUM -> handleMediumPlanning(query, chatId);
                case COMPLEX -> handleComplexPlanning(query, chatId);
            };

        } catch (Exception e) {
            log.error("[TravelPlanningSkill] 规划失败", e);
            return "抱歉，差旅规划失败：" + e.getMessage();
        }
    }

    /**
     * 处理简单规划（SIMPLE）
     */
    private String handleSimplePlanning(String query, String chatId) {
        log.info("[TravelPlanningSkill] 简单规划");

        // 提取城市并查询天气
        String city = extractCity(query);
        if (city != null) {
            try {
                String weatherInfo = weatherQueryTool.queryWeather(city);
                return "差旅规划如下：\n\n【天气信息】\n" + weatherInfo;
            } catch (Exception e) {
                log.error("[TravelPlanningSkill] 天气查询失败", e);
            }
        }

        return "简单规划：" + query;
    }

    /**
     * 处理中等规划（MEDIUM）
     */
    private String handleMediumPlanning(String query, String chatId) {
        log.info("[TravelPlanningSkill] 中等规划");

        StringBuilder result = new StringBuilder();
        result.append("差旅规划如下：\n\n");

        // 查询天气
        String city = extractCity(query);
        if (city != null) {
            try {
                String weatherInfo = weatherQueryTool.queryWeather(city);
                result.append("【天气信息】\n").append(weatherInfo).append("\n\n");
            } catch (Exception e) {
                log.error("[TravelPlanningSkill] 天气查询失败", e);
            }
        }

        result.append("提示：如需更详细的规划，请提供更多信息（如出差天数、拜访客户等）。");

        return result.toString();
    }

    /**
     * 处理复杂规划（COMPLEX）
     * 调用 Service：任务分解 → 并行执行 → 结果整合
     */
    private String handleComplexPlanning(String query, String chatId) {
        log.info("[TravelPlanningSkill] 复杂规划");

        try {
            // 1. 调用 Service：任务分解
            List<SubTask> tasks = taskDecomposer.decompose(query);
            log.info("[TravelPlanningSkill] 任务分解完成，共 {} 个子任务", tasks.size());

            // 2. 调用 Service：按依赖关系排序
            List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
            log.info("[TravelPlanningSkill] 任务排序完成，共 {} 批次", batches.size());

            // 3. 并行执行任务
            Map<String, String> results = new HashMap<>();
            for (List<SubTask> batch : batches) {
                executeTasksInParallel(batch, results);
            }

            // 4. 调用 LLM 整合结果
            String finalResult = integrateResults(query, results);
            log.info("[TravelPlanningSkill] 规划完成");

            return finalResult;

        } catch (Exception e) {
            log.error("[TravelPlanningSkill] 复杂规划失败", e);
            return "抱歉，复杂规划失败：" + e.getMessage();
        }
    }

    /**
     * 并行执行任务
     */
    private void executeTasksInParallel(List<SubTask> tasks, Map<String, String> results) {
        List<CompletableFuture<Void>> futures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(() -> {
                    String result = executeSubTask(task);
                    synchronized (results) {
                        results.put(task.getTaskType() + "_" + task.getId(), result);
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 执行单个子任务
     */
    private String executeSubTask(SubTask task) {
        log.info("[TravelPlanningSkill] 执行子任务: {}", task.getDescription());

        try {
            return switch (task.getTaskType()) {
                case "QUERY_WEATHER" -> {
                    String city = extractCityFromParameters(task.getParameters());
                    yield weatherQueryTool.queryWeather(city);
                }
                default -> "不支持的任务类型: " + task.getTaskType();
            };
        } catch (Exception e) {
            log.error("[TravelPlanningSkill] 子任务执行失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 整合结果
     */
    private String integrateResults(String query, Map<String, String> results) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下多个子任务的执行结果整合为一个连贯、自然的差旅规划回复：\n\n");
        prompt.append("用户查询：").append(query).append("\n\n");

        int index = 1;
        for (Map.Entry<String, String> entry : results.entrySet()) {
            prompt.append(String.format("%d. %s:\n%s\n\n", index++, entry.getKey(), entry.getValue()));
        }

        prompt.append("要求：\n");
        prompt.append("1. 用自然语言整合所有信息\n");
        prompt.append("2. 保持逻辑连贯，避免简单罗列\n");
        prompt.append("3. 突出重点信息\n");
        prompt.append("4. 语气友好、专业\n");

        return chatClient.prompt()
                .user(prompt.toString())
                .call()
                .content();
    }

    /**
     * 从查询中提取城市
     */
    private String extractCity(String query) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "成都", "西安", "南京",
                "武汉", "重庆", "天津", "苏州", "郑州", "长沙", "沈阳", "青岛"};
        for (String city : cities) {
            if (query.contains(city)) {
                return city;
            }
        }
        return null;
    }

    /**
     * 从参数中提取城市
     */
    private String extractCityFromParameters(String parameters) {
        try {
            // 简单解析 JSON
            if (parameters.contains("city")) {
                int start = parameters.indexOf("\"city\"") + 8;
                int end = parameters.indexOf("\"", start);
                return parameters.substring(start, end);
            }
        } catch (Exception e) {
            log.error("[TravelPlanningSkill] 参数解析失败", e);
        }
        return "北京";
    }

    @Override
    public int getPriority() {
        return 60;
    }
}
