package com.jblmj.aiagent.skill;

/**
 * Skill 接口
 *
 * Skill 是面向用户任务的功能单元，一个任务对应一个 Skill
 *
 * 标准定义：
 * - Skill = 用户可以直接理解的任务（查天气、规划行程、发送邮件）
 * - 每个 Skill 内部可以调用多个 Service 和 Tool 来完成任务
 * - Skill 不应该是"能力"或"中间件"（如复杂度评估、任务分解）
 *
 * 示例：
 * - ✅ WeatherQuerySkill - 查询天气
 * - ✅ TravelPlanningSkill - 规划差旅行程
 * - ✅ EmailSendSkill - 发送邮件
 * - ❌ ComplexityAssessmentSkill - 这应该是 Service，不是 Skill
 * - ❌ TaskDecompositionSkill - 这应该是 Service，不是 Skill
 *
 * @author jblmj
 */
public interface Skill {

    /**
     * Skill 名称（唯一标识）
     */
    String getName();

    /**
     * Skill 描述（用于 LLM 选择或日志记录）
     */
    String getDescription();

    /**
     * Skill 层级（目前只有 BUSINESS）
     */
    SkillLayer getLayer();

    /**
     * 判断是否能处理该查询
     *
     * @param query 用户查询
     * @return true 表示可以处理
     */
    boolean canHandle(String query);

    /**
     * 执行 Skill
     *
     * @param query 用户查询
     * @param chatId 会话 ID
     * @return 执行结果
     */
    String execute(String query, String chatId);

    /**
     * Skill 优先级（数字越小优先级越高）
     * 当多个 Skill 都能处理同一查询时，选择优先级最高的
     * 默认优先级为 100
     */
    default int getPriority() {
        return 100;
    }
}
