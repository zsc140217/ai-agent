package com.jblmj.aiagent.skill;

/**
 * Skill 层级枚举
 *
 * 注意：标准的 Skill 定义中，所有 Skill 都是面向用户任务的业务层
 * 不应该有"能力层 Skill"（如复杂度评估、任务分解等）
 * 这些应该是 Service，而不是 Skill
 *
 * @author jblmj
 */
public enum SkillLayer {

    /**
     * 业务层 Skill
     * 面向用户任务的功能单元
     * 例如：天气查询、差旅规划、客户拜访
     */
    BUSINESS
}
