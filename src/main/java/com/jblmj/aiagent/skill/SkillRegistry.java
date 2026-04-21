package com.jblmj.aiagent.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill 注册中心
 *
 * 职责：
 * 1. 自动扫描并注册所有带 @SkillComponent 注解的 Skill
 * 2. 根据查询选择合适的 Skill
 * 3. 管理所有业务层 Skill
 *
 * 设计说明：
 * - 所有 Skill 都是面向用户任务的业务层 Skill
 * - 不再有"能力层 Skill"（复杂度评估、任务分解等应该是 Service）
 *
 * @author jblmj
 */
@Slf4j
@Component
public class SkillRegistry implements ApplicationContextAware {

    /**
     * 所有 Skill 映射表
     * key: Skill 名称
     * value: Skill 实例
     */
    private final Map<String, Skill> skills = new HashMap<>();

    /**
     * 所有 Skill 列表（按优先级排序）
     */
    private final List<Skill> allSkills = new ArrayList<>();

    /**
     * Spring 容器启动时自动扫描并注册 Skill
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        log.info("========================================");
        log.info("开始扫描并注册 Skill");
        log.info("========================================");

        // 扫描所有带 @SkillComponent 注解的 Bean
        Map<String, Object> beans = context.getBeansWithAnnotation(SkillComponent.class);

        for (Object bean : beans.values()) {
            if (bean instanceof Skill) {
                register((Skill) bean);
            }
        }

        // 按优先级排序
        allSkills.sort(Comparator.comparingInt(Skill::getPriority));

        log.info("========================================");
        log.info("Skill 注册完成，共注册 {} 个 Skill", allSkills.size());
        log.info("已注册的 Skill: {}", skills.keySet());
        log.info("========================================");
    }

    /**
     * 注册 Skill
     */
    public void register(Skill skill) {
        skills.put(skill.getName(), skill);
        allSkills.add(skill);
        log.info("注册 Skill: {} - {}", skill.getName(), skill.getDescription());
    }

    /**
     * 根据名称获取 Skill
     *
     * @param name Skill 名称
     * @return Skill 实例，如果不存在则返回 null
     */
    public Skill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * 根据查询选择 Skill
     *
     * 选择策略：
     * 1. 遍历所有 Skill
     * 2. 调用 canHandle() 判断是否能处理
     * 3. 如果多个 Skill 都能处理，选择优先级最高的
     *
     * @param query 用户查询
     * @return 匹配的 Skill，如果没有匹配则返回 null
     */
    public Skill selectSkill(String query) {
        List<Skill> candidates = skills.values().stream()
                .filter(skill -> skill.canHandle(query))
                .sorted(Comparator.comparingInt(Skill::getPriority))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            log.debug("没有找到匹配的 Skill: {}", query);
            return null;
        }

        Skill selected = candidates.get(0);
        log.info("选择 Skill: {} (优先级: {})", selected.getName(), selected.getPriority());
        return selected;
    }

    /**
     * 获取所有 Skill
     */
    public List<Skill> getAllSkills() {
        return new ArrayList<>(allSkills);
    }

    /**
     * 获取所有 Skill 的描述（用于 LLM 选择）
     */
    public String getAllSkillDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用的 Skill：\n\n");

        for (Skill skill : skills.values()) {
            sb.append(String.format("- %s: %s\n", skill.getName(), skill.getDescription()));
        }

        return sb.toString();
    }

    /**
     * 获取 Skill 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allSkills.size());
        stats.put("skills", skills.keySet());
        return stats;
    }
}
