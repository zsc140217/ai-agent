package com.jblmj.aiagent.skill.business;

import com.jblmj.aiagent.agent.JblmjManus;
import com.jblmj.aiagent.model.ReActStep;
import com.jblmj.aiagent.skill.Skill;
import com.jblmj.aiagent.skill.SkillComponent;
import com.jblmj.aiagent.skill.SkillLayer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 基于 ReAct 框架的差旅规划 Skill
 *
 * 核心改进：
 * 1. 使用 JblmjManus（增强版 ReAct Agent）作为执行引擎
 * 2. 完整的 Thought → Action → Observation → Reflection 循环
 * 3. 自动工具选择和策略调整
 * 4. 完整的执行轨迹追踪
 *
 * 使用场景：
 * - "帮我规划明天去杭州的行程"
 * - "去深圳出差，查天气和推荐酒店"
 * - "规划北京3天出差，包括客户拜访"
 *
 * @author jblmj
 */
@Slf4j
@SkillComponent(
        name = "react_travel_planning",
        description = "基于 ReAct 框架的智能差旅规划，自动选择工具并调整策略",
        layer = SkillLayer.BUSINESS,
        keywords = {"规划", "行程", "出差", "安排", "计划", "准备"},
        priority = 70  // 优先级高于原有的 TravelPlanningSkill
)
public class ReActTravelPlanningSkill implements Skill {

    @Resource
    private JblmjManus jblmjManus;

    @Override
    public String getName() {
        return "react_travel_planning";
    }

    @Override
    public String getDescription() {
        return "基于 ReAct 框架的智能差旅规划，自动选择工具并调整策略";
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
        log.info("[ReActTravelPlanningSkill] 开始基于 ReAct 框架的差旅规划: {}", query);
        log.info("========================================");

        try {
            // 构建增强的提示词，引导 Agent 进行差旅规划
            String enhancedPrompt = buildTravelPlanningPrompt(query);

            // 使用 JblmjManus 执行（完整的 ReAct 循环）
            String result = jblmjManus.run(enhancedPrompt);

            // 获取执行轨迹
            List<ReActStep> trace = jblmjManus.getExecutionTrace();
            log.info("[ReActTravelPlanningSkill] 执行完成，共 {} 个步骤", trace.size());

            // 输出执行轨迹（用于调试和分析）
            logExecutionTrace(trace);

            // 格式化输出结果
            return formatResult(result, trace);

        } catch (Exception e) {
            log.error("[ReActTravelPlanningSkill] 规划失败", e);
            return "抱歉，差旅规划失败：" + e.getMessage();
        }
    }

    /**
     * 构建差旅规划的提示词
     */
    private String buildTravelPlanningPrompt(String query) {
        return String.format("""
                你是一个专业的差旅规划助手，需要帮助用户规划出差行程。

                用户需求：%s

                请按照以下步骤进行规划：
                1. 分析用户需求，识别目的地城市
                2. 查询目的地天气情况（使用 queryWeather 工具）
                3. 根据天气给出穿衣和携带物品建议
                4. 如果用户提到客户拜访，查询客户信息
                5. 如果用户提到酒店，推荐合适的酒店
                6. 整合所有信息，生成完整的差旅规划

                注意：
                - 每次使用工具后，观察结果并判断是否需要调整策略
                - 如果工具调用失败，尝试其他方案
                - 最后使用 terminate 工具结束任务
                """, query);
    }

    /**
     * 记录执行轨迹
     */
    private void logExecutionTrace(List<ReActStep> trace) {
        log.info("========== 执行轨迹 ==========");
        for (ReActStep step : trace) {
            log.info("步骤 {}: ", step.getStepNumber());
            log.info("  💭 Thought: {}", truncate(step.getThought(), 100));
            log.info("  🔧 Action: {}", truncate(step.getAction(), 100));
            log.info("  👁️ Observation: {}", truncate(step.getObservation(), 100));
            log.info("  🤔 Reflection: {}", truncate(step.getReflection(), 100));
            log.info("  ⏱️ Duration: {}ms", step.getDuration());
        }
        log.info("========== 轨迹结束 ==========");
    }

    /**
     * 格式化结果
     */
    private String formatResult(String result, List<ReActStep> trace) {
        StringBuilder formatted = new StringBuilder();

        // 1. 主要结果
        formatted.append("【差旅规划】\n\n");
        formatted.append(extractMainResult(result));
        formatted.append("\n\n");

        // 2. 执行摘要
        formatted.append("【执行摘要】\n");
        formatted.append(String.format("- 执行步骤：%d 步\n", trace.size()));

        long totalDuration = trace.stream().mapToLong(ReActStep::getDuration).sum();
        formatted.append(String.format("- 总耗时：%d ms\n", totalDuration));

        long toolCallCount = trace.stream()
                .filter(step -> step.getAction() != null && step.getAction().contains("工具"))
                .count();
        formatted.append(String.format("- 工具调用：%d 次\n", toolCallCount));

        return formatted.toString();
    }

    /**
     * 提取主要结果（去除执行细节）
     */
    private String extractMainResult(String result) {
        // 简单处理：提取最后一步的结果
        String[] lines = result.split("\n");
        StringBuilder mainResult = new StringBuilder();

        for (String line : lines) {
            // 跳过执行步骤的标记
            if (line.startsWith("Step") || line.contains("===")) {
                continue;
            }
            mainResult.append(line).append("\n");
        }

        return mainResult.toString().trim();
    }

    /**
     * 截断字符串
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    @Override
    public int getPriority() {
        return 70;  // 优先级高于原有的 TravelPlanningSkill
    }
}
