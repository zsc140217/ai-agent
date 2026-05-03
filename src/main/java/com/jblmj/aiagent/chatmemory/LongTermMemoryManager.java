package com.jblmj.aiagent.chatmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 长期记忆管理器
 *
 * 面试要点：
 * 1. 为什么需要长期记忆？记录用户偏好，提供个性化服务
 * 2. 存储方式：JSON文件（可升级为数据库）
 * 3. 隐私保护：敏感信息脱敏，支持用户删除
 *
 * 应用场景：
 * - 用户常去城市：自动推荐该城市的协议酒店
 * - 偏好酒店档次：优先推荐符合预算的酒店
 * - 历史行程摘要：向量化后存入VectorStore，支持"上次去杭州住的哪家酒店"
 */
@Component
@Slf4j
public class LongTermMemoryManager {

    @Value("${chat.memory.longterm.path:./data/user-profiles}")
    private String storagePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(String userId) {
        File file = new File(storagePath, userId + ".json");
        if (!file.exists()) {
            return new UserProfile(userId);
        }

        try {
            return objectMapper.readValue(file, UserProfile.class);
        } catch (IOException e) {
            log.error("Failed to load user profile for userId={}", userId, e);
            return new UserProfile(userId);
        }
    }

    /**
     * 保存用户画像
     */
    public void saveUserProfile(UserProfile profile) {
        File dir = new File(storagePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(storagePath, profile.getUserId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, profile);
            log.info("Saved user profile for userId={}", profile.getUserId());
        } catch (IOException e) {
            log.error("Failed to save user profile for userId={}", profile.getUserId(), e);
        }
    }

    /**
     * 从对话历史中学习用户偏好
     * 当前实现：基于规则的简单统计
     * TODO: 升级为机器学习模型
     */
    public void learnFromConversation(String userId, String conversationId, WorkingMemory workingMemory) {
        UserProfile profile = getUserProfile(userId);

        // 1. 统计常去城市
        for (String city : workingMemory.getCities()) {
            profile.incrementCityVisitCount(city);
        }

        // 2. 记录历史行程摘要
        if (workingMemory.getCurrentDestination() != null) {
            TripSummary summary = new TripSummary();
            summary.setDestination(workingMemory.getCurrentDestination());
            summary.setTimestamp(System.currentTimeMillis());
            summary.setIntents(new ArrayList<>(workingMemory.getIntentHistory()));
            profile.addTripSummary(summary);
        }

        // 3. 保存更新后的画像
        saveUserProfile(profile);
        log.info("Learned preferences from conversationId={} for userId={}", conversationId, userId);
    }

    /**
     * 获取个性化推荐提示
     */
    public String getPersonalizedHint(String userId, String currentCity) {
        UserProfile profile = getUserProfile(userId);
        StringBuilder hint = new StringBuilder();

        // 1. 常去城市提示
        if (profile.getCityVisitCount().getOrDefault(currentCity, 0) > 0) {
            hint.append("您之前来过").append(currentCity).append("，");
        }

        // 2. 历史酒店推荐
        Optional<TripSummary> lastTrip = profile.getTripSummaries().stream()
                .filter(t -> t.getDestination().equals(currentCity))
                .max(Comparator.comparingLong(TripSummary::getTimestamp));

        if (lastTrip.isPresent()) {
            hint.append("上次入住的酒店信息已为您调取。");
        }

        return hint.toString();
    }

    /**
     * 删除用户数据（GDPR合规）
     */
    public void deleteUserData(String userId) {
        File file = new File(storagePath, userId + ".json");
        if (file.exists()) {
            file.delete();
            log.info("Deleted user data for userId={}", userId);
        }
    }

    /**
     * 用户画像数据结构
     */
    @Data
    public static class UserProfile {
        private String userId;
        private Map<String, Integer> cityVisitCount = new HashMap<>(); // 城市访问次数
        private String preferredHotelLevel;                            // 偏好酒店档次
        private List<TripSummary> tripSummaries = new ArrayList<>();   // 历史行程摘要
        private long createdAt = System.currentTimeMillis();
        private long updatedAt = System.currentTimeMillis();

        public UserProfile() {}

        public UserProfile(String userId) {
            this.userId = userId;
        }

        public void incrementCityVisitCount(String city) {
            cityVisitCount.put(city, cityVisitCount.getOrDefault(city, 0) + 1);
            updatedAt = System.currentTimeMillis();
        }

        public void addTripSummary(TripSummary summary) {
            // 保留最近20次行程
            if (tripSummaries.size() >= 20) {
                tripSummaries.remove(0);
            }
            tripSummaries.add(summary);
            updatedAt = System.currentTimeMillis();
        }
    }

    /**
     * 行程摘要
     */
    @Data
    public static class TripSummary {
        private String destination;
        private long timestamp;
        private List<String> intents;
        private String hotelName;
        private String customerName;
    }
}
