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
            // 记录访问
            lastTrip.get().recordAccess();
            saveUserProfile(profile);

            hint.append("上次入住的酒店信息已为您调取。");
        }

        return hint.toString();
    }

    /**
     * 记录记忆访问（用于智能遗忘）
     *
     * 当用户查询历史行程时调用此方法
     */
    public void recordMemoryAccess(String userId, String destination) {
        UserProfile profile = getUserProfile(userId);

        for (TripSummary trip : profile.getTripSummaries()) {
            if (trip.getDestination().equals(destination)) {
                trip.recordAccess();
                log.debug("记录记忆访问: userId={}, destination={}, accessCount={}",
                        userId, destination, trip.getAccessCount());
            }
        }

        saveUserProfile(profile);
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
     * 定期清理过期记忆（建议每天执行）
     *
     * 清理策略：
     * 1. 删除6个月前且访问次数<2的记忆
     * 2. 归档1年前的数据
     * 3. 语义去重（相同目的地+时间窗口）
     */
    public void cleanupExpiredMemories() {
        log.info("开始清理过期记忆");
        File dir = new File(storagePath);
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        int totalCleaned = 0;
        for (File file : files) {
            try {
                UserProfile profile = objectMapper.readValue(file, UserProfile.class);
                int beforeSize = profile.getTripSummaries().size();

                // 清理过期记忆
                cleanupExpiredMemoriesForUser(profile);

                // 语义去重
                deduplicateMemories(profile);

                int afterSize = profile.getTripSummaries().size();
                int cleaned = beforeSize - afterSize;
                totalCleaned += cleaned;

                if (cleaned > 0) {
                    saveUserProfile(profile);
                    log.info("用户 {} 清理了 {} 条记忆", profile.getUserId(), cleaned);
                }

            } catch (IOException e) {
                log.error("处理用户画像失败: {}", file.getName(), e);
            }
        }

        log.info("记忆清理完成，共清理 {} 条记忆", totalCleaned);
    }

    /**
     * 清理单个用户的过期记忆
     */
    private void cleanupExpiredMemoriesForUser(UserProfile profile) {
        long now = System.currentTimeMillis();
        long sixMonthsAgo = now - (180L * 24 * 60 * 60 * 1000);

        // 删除6个月前且访问次数<2的记忆
        profile.getTripSummaries().removeIf(trip ->
                trip.getTimestamp() < sixMonthsAgo &&
                trip.getAccessCount() < 2
        );
    }

    /**
     * 语义去重（基于目的地和时间窗口）
     *
     * 策略：同一目的地在同一周内的多次行程合并为一条
     */
    private void deduplicateMemories(UserProfile profile) {
        Map<String, TripSummary> uniqueTrips = new LinkedHashMap<>();

        for (TripSummary trip : profile.getTripSummaries()) {
            // 按周分组：destination_weekNumber
            long weekNumber = trip.getTimestamp() / (7L * 24 * 60 * 60 * 1000);
            String key = trip.getDestination() + "_" + weekNumber;

            if (!uniqueTrips.containsKey(key)) {
                uniqueTrips.put(key, trip);
            } else {
                // 合并重复记忆
                TripSummary existing = uniqueTrips.get(key);

                // 合并意图列表
                if (trip.getIntents() != null) {
                    if (existing.getIntents() == null) {
                        existing.setIntents(new ArrayList<>());
                    }
                    existing.getIntents().addAll(trip.getIntents());
                }

                // 累加访问次数
                existing.setAccessCount(existing.getAccessCount() + trip.getAccessCount());

                // 保留最新的访问时间
                if (trip.getLastAccessTime() > existing.getLastAccessTime()) {
                    existing.setLastAccessTime(trip.getLastAccessTime());
                }

                log.debug("合并重复记忆: destination={}, week={}", trip.getDestination(), weekNumber);
            }
        }

        profile.setTripSummaries(new ArrayList<>(uniqueTrips.values()));
    }

    /**
     * 获取记忆统计信息（用于监控）
     */
    public MemoryStats getMemoryStats(String userId) {
        UserProfile profile = getUserProfile(userId);
        MemoryStats stats = new MemoryStats();

        stats.totalMemories = profile.getTripSummaries().size();
        stats.totalCities = profile.getCityVisitCount().size();

        // 计算平均访问次数
        double avgAccess = profile.getTripSummaries().stream()
                .mapToInt(TripSummary::getAccessCount)
                .average()
                .orElse(0.0);
        stats.avgAccessCount = avgAccess;

        // 找出最常访问的记忆
        profile.getTripSummaries().stream()
                .max(Comparator.comparingInt(TripSummary::getAccessCount))
                .ifPresent(trip -> {
                    stats.mostAccessedDestination = trip.getDestination();
                    stats.maxAccessCount = trip.getAccessCount();
                });

        return stats;
    }

    /**
     * 记忆统计信息
     */
    @Data
    public static class MemoryStats {
        private int totalMemories;
        private int totalCities;
        private double avgAccessCount;
        private String mostAccessedDestination;
        private int maxAccessCount;
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
            tripSummaries.add(summary);
            updatedAt = System.currentTimeMillis();

            // 智能清理：如果超过容量，清理低重要性记忆
            if (tripSummaries.size() > 20) {
                cleanupLowImportanceMemories();
            }
        }

        /**
         * 清理低重要性记忆（智能遗忘）
         */
        private void cleanupLowImportanceMemories() {
            long now = System.currentTimeMillis();

            // 计算每条记忆的重要性
            for (TripSummary trip : tripSummaries) {
                trip.importanceScore = calculateImportance(trip, now);
            }

            // 按重要性排序，保留Top-20
            tripSummaries.sort((a, b) -> Double.compare(b.importanceScore, a.importanceScore));

            // 删除低重要性记忆
            if (tripSummaries.size() > 20) {
                tripSummaries = new ArrayList<>(tripSummaries.subList(0, 20));
            }
        }

        /**
         * 计算记忆重要性评分
         *
         * 算法：importance = timeDecay * (1 + frequencyScore + richnessScore)
         *
         * 因素1：时间衰减（越新越重要）
         * 因素2：访问频率（越常用越重要）
         * 因素3：信息丰富度（信息越多越重要）
         */
        private double calculateImportance(TripSummary trip, long now) {
            // 因素1：时间衰减（指数衰减）
            // 公式：e^(-λ * days)
            // λ=0.05 表示每20天衰减到原来的37%
            long daysSinceCreation = (now - trip.timestamp) / (1000 * 60 * 60 * 24);
            double timeDecay = Math.exp(-0.05 * daysSinceCreation);

            // 因素2：访问频率（对数增长，避免过度偏向高频）
            // 访问1次=0, 访问10次=2.3, 访问100次=4.6
            double frequencyScore = Math.log(trip.accessCount + 1);

            // 因素3：信息丰富度
            // 意图越多，信息越丰富
            int intentCount = (trip.intents != null) ? trip.intents.size() : 0;
            double richnessScore = intentCount * 0.2;

            // 综合评分
            return timeDecay * (1 + frequencyScore + richnessScore);
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

        // 智能遗忘相关字段
        private int accessCount = 0;           // 访问次数
        private long lastAccessTime;           // 最后访问时间
        private double importanceScore = 0.0;  // 重要性评分（缓存）

        /**
         * 记录访问
         */
        public void recordAccess() {
            this.accessCount++;
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
