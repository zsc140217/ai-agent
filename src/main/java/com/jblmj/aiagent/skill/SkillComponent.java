package com.jblmj.aiagent.skill;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skill 组件注解
 *
 * 用于标记一个类是 Skill，并自动注册到 SkillRegistry
 *
 * 使用示例：
 * <pre>
 * @SkillComponent(
 *     name = "weather_query",
 *     description = "查询天气信息",
 *     layer = SkillLayer.BUSINESS,
 *     keywords = {"天气", "温度", "下雨"}
 * )
 * public class WeatherQuerySkill implements Skill {
 *     // ...
 * }
 * </pre>
 *
 * @author jblmj
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface SkillComponent {

    /**
     * Skill 名称（唯一标识）
     */
    String name();

    /**
     * Skill 描述
     */
    String description();

    /**
     * Skill 层级
     */
    SkillLayer layer();

    /**
     * 触发关键词（用于快速匹配）
     * 如果查询包含任意一个关键词，则认为该 Skill 可以处理
     */
    String[] keywords() default {};

    /**
     * 优先级（数字越小优先级越高）
     */
    int priority() default 100;
}
