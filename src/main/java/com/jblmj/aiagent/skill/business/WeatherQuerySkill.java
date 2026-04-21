package com.jblmj.aiagent.skill.business;

import com.jblmj.aiagent.skill.Skill;
import com.jblmj.aiagent.skill.SkillComponent;
import com.jblmj.aiagent.skill.SkillLayer;
import com.jblmj.aiagent.tools.WeatherQueryTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 天气查询 Skill
 *
 * 业务层 Skill，处理天气相关查询
 *
 * 功能：
 * - 单城市天气查询
 * - 多城市天气对比
 * - 自动提取城市名称
 *
 * 使用场景：
 * - "北京今天天气怎么样"
 * - "上海和广州天气对比"
 * - "深圳适合出差吗"
 *
 * @author jblmj
 */
@Slf4j
@SkillComponent(
        name = "weather_query",
        description = "查询天气信息，支持单城市查询和多城市对比",
        layer = SkillLayer.BUSINESS,
        keywords = {"天气", "温度", "下雨", "带伞", "气温", "热", "冷", "晴", "阴", "适合出差"},
        priority = 50
)
public class WeatherQuerySkill implements Skill {

    @Resource
    private WeatherQueryTool weatherQueryTool;

    // 支持的城市列表
    private static final List<String> CITIES = List.of(
            "北京", "上海", "广州", "深圳", "杭州", "成都", "西安", "南京",
            "武汉", "重庆", "天津", "苏州", "郑州", "长沙", "沈阳", "青岛",
            "无锡", "宁波", "佛山", "合肥"
    );

    @Override
    public String getName() {
        return "weather_query";
    }

    @Override
    public String getDescription() {
        return "查询天气信息，支持单城市查询和多城市对比";
    }

    @Override
    public SkillLayer getLayer() {
        return SkillLayer.BUSINESS;
    }

    @Override
    public boolean canHandle(String query) {
        // 包含天气关键词
        String[] keywords = {"天气", "温度", "下雨", "带伞", "气温", "热", "冷", "晴", "阴", "适合出差"};
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String execute(String query, String chatId) {
        log.info("[WeatherQuerySkill] 开始处理天气查询: {}", query);

        // 提取城市列表
        List<String> cities = extractCities(query);

        if (cities.isEmpty()) {
            log.warn("[WeatherQuerySkill] 未识别到城市名称，使用默认城市：北京");
            cities.add("北京");
        }

        log.info("[WeatherQuerySkill] 识别到城市: {}", cities);

        // 根据城市数量选择处理策略
        if (cities.size() == 1) {
            // 单城市查询
            return handleSingleCityQuery(cities.get(0));
        } else {
            // 多城市对比
            return handleMultiCityComparison(cities);
        }
    }

    /**
     * 处理单城市查询
     */
    private String handleSingleCityQuery(String city) {
        log.info("[WeatherQuerySkill] 单城市查询: {}", city);

        try {
            String result = weatherQueryTool.queryWeather(city);
            log.info("[WeatherQuerySkill] 查询成功");
            return result;
        } catch (Exception e) {
            log.error("[WeatherQuerySkill] 查询失败", e);
            return "抱歉，" + city + " 的天气查询失败：" + e.getMessage();
        }
    }

    /**
     * 处理多城市对比
     */
    private String handleMultiCityComparison(List<String> cities) {
        log.info("[WeatherQuerySkill] 多城市对比: {}", cities);

        StringBuilder result = new StringBuilder();
        result.append("以下是各城市的天气对比：\n\n");

        for (String city : cities) {
            try {
                String weatherInfo = weatherQueryTool.queryWeather(city);
                result.append("【").append(city).append("】\n");
                result.append(weatherInfo).append("\n\n");
            } catch (Exception e) {
                log.error("[WeatherQuerySkill] {} 查询失败", city, e);
                result.append("【").append(city).append("】\n");
                result.append("查询失败：").append(e.getMessage()).append("\n\n");
            }
        }

        result.append("综合建议：根据以上天气信息，您可以选择天气更适宜的城市进行出差。");

        log.info("[WeatherQuerySkill] 对比完成");
        return result.toString();
    }

    /**
     * 从查询中提取城市名称
     */
    private List<String> extractCities(String query) {
        List<String> extractedCities = new ArrayList<>();

        for (String city : CITIES) {
            if (query.contains(city)) {
                extractedCities.add(city);
            }
        }

        return extractedCities;
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
