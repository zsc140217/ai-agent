package com.jblmj.aiagent.chatmemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能遗忘机制测试
 *
 * 测试场景：
 * 1. 时间衰减：旧记忆重要性降低
 * 2. 访问频率：高频记忆优先保留
 * 3. 信息丰富度：信息多的记忆优先保留
 * 4. 智能清理：超过20条时清理低重要性记忆
 * 5. 语义去重：相同目的地+时间窗口合并
 */
public class SmartMemoryForgetTest {

    @TempDir
    File tempDir;

    private LongTermMemoryManager memoryManager;

    @BeforeEach
    void setUp() {
        memoryManager = new LongTermMemoryManager();
        // 使用临时目录
        ReflectionTestUtils.setField(memoryManager, "storagePath", tempDir.getAbsolutePath());
    }

    /**
     * 测试1：时间衰减 - 旧记忆应该被清理
     */
    @Test
    void testTimeDecay() {
        String userId = "user_time_decay";
        LongTermMemoryManager.UserProfile profile = new LongTermMemoryManager.UserProfile(userId);

        long now = System.currentTimeMillis();
        long oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000);
        long sixMonthsAgo = now - (180L * 24 * 60 * 60 * 1000);

        // 添加3条记忆：新、中、旧
        LongTermMemoryManager.TripSummary newTrip = createTrip("北京", now, 0);
        LongTermMemoryManager.TripSummary mediumTrip = createTrip("上海", oneMonthAgo, 0);
        LongTermMemoryManager.TripSummary oldTrip = createTrip("深圳", sixMonthsAgo, 0);

        profile.addTripSummary(newTrip);
        profile.addTripSummary(mediumTrip);
        profile.addTripSummary(oldTrip);

        memoryManager.saveUserProfile(profile);

        // 执行清理
        memoryManager.cleanupExpiredMemories();

        // 验证：6个月前且访问次数<2的记忆应该被删除
        LongTermMemoryManager.UserProfile loadedProfile = memoryManager.getUserProfile(userId);
        assertEquals(2, loadedProfile.getTripSummaries().size(), "旧记忆应该被清理");

        // 验证保留的是新记忆和中等记忆
        assertTrue(loadedProfile.getTripSummaries().stream()
                .anyMatch(t -> t.getDestination().equals("北京")));
        assertTrue(loadedProfile.getTripSummaries().stream()
                .anyMatch(t -> t.getDestination().equals("上海")));
        assertFalse(loadedProfile.getTripSummaries().stream()
                .anyMatch(t -> t.getDestination().equals("深圳")));
    }

    /**
     * 测试2：访问频率 - 高频记忆优先保留
     */
    @Test
    void testAccessFrequency() {
        String userId = "user_frequency";
        LongTermMemoryManager.UserProfile profile = new LongTermMemoryManager.UserProfile(userId);

        long now = System.currentTimeMillis();

        // 添加25条记忆（超过20条限制）
        for (int i = 0; i < 25; i++) {
            LongTermMemoryManager.TripSummary trip = createTrip("城市" + i, now - i * 1000, 0);

            // 前5条记忆设置高访问次数
            if (i < 5) {
                for (int j = 0; j < 10; j++) {
                    trip.recordAccess();
                }
            }

            profile.addTripSummary(trip);
        }

        memoryManager.saveUserProfile(profile);

        // 验证：应该只保留20条
        LongTermMemoryManager.UserProfile loadedProfile = memoryManager.getUserProfile(userId);
        assertEquals(20, loadedProfile.getTripSummaries().size(), "应该只保留20条记忆");

        // 验证：高频访问的记忆应该被保留
        List<LongTermMemoryManager.TripSummary> trips = loadedProfile.getTripSummaries();
        long highFrequencyCount = trips.stream()
                .filter(t -> t.getAccessCount() >= 10)
                .count();

        assertTrue(highFrequencyCount >= 4, "高频记忆应该被优先保留");
    }

    /**
     * 测试3：信息丰富度 - 信息多的记忆优先保留
     */
    @Test
    void testInformationRichness() {
        String userId = "user_richness";
        LongTermMemoryManager.UserProfile profile = new LongTermMemoryManager.UserProfile(userId);

        long now = System.currentTimeMillis();

        // 添加25条记忆
        for (int i = 0; i < 25; i++) {
            LongTermMemoryManager.TripSummary trip = createTrip("城市" + i, now - i * 1000, 0);

            // 前5条记忆添加丰富的意图信息
            if (i < 5) {
                List<String> intents = new ArrayList<>();
                intents.add("查询天气");
                intents.add("预订酒店");
                intents.add("查询交通");
                intents.add("客户拜访");
                trip.setIntents(intents);
            } else {
                List<String> intents = new ArrayList<>();
                intents.add("查询天气");
                trip.setIntents(intents);
            }

            profile.addTripSummary(trip);
        }

        memoryManager.saveUserProfile(profile);

        // 验证：信息丰富的记忆应该被保留
        LongTermMemoryManager.UserProfile loadedProfile = memoryManager.getUserProfile(userId);
        assertEquals(20, loadedProfile.getTripSummaries().size());

        long richMemoryCount = loadedProfile.getTripSummaries().stream()
                .filter(t -> t.getIntents() != null && t.getIntents().size() >= 4)
                .count();

        assertTrue(richMemoryCount >= 4, "信息丰富的记忆应该被优先保留");
    }

    /**
     * 测试4：语义去重 - 相同目的地+时间窗口应该合并
     */
    @Test
    void testSemanticDeduplication() {
        String userId = "user_dedup";
        LongTermMemoryManager.UserProfile profile = new LongTermMemoryManager.UserProfile(userId);

        long now = System.currentTimeMillis();
        long oneDayAgo = now - (1L * 24 * 60 * 60 * 1000);
        long twoDaysAgo = now - (2L * 24 * 60 * 60 * 1000);

        // 添加3条相同目的地、同一周的记忆
        LongTermMemoryManager.TripSummary trip1 = createTrip("北京", now, 5);
        trip1.setIntents(List.of("查询天气"));

        LongTermMemoryManager.TripSummary trip2 = createTrip("北京", oneDayAgo, 3);
        trip2.setIntents(List.of("预订酒店"));

        LongTermMemoryManager.TripSummary trip3 = createTrip("北京", twoDaysAgo, 2);
        trip3.setIntents(List.of("查询交通"));

        profile.addTripSummary(trip1);
        profile.addTripSummary(trip2);
        profile.addTripSummary(trip3);

        memoryManager.saveUserProfile(profile);

        // 执行去重
        memoryManager.cleanupExpiredMemories();

        // 验证：3条记忆应该合并为1条
        LongTermMemoryManager.UserProfile loadedProfile = memoryManager.getUserProfile(userId);
        assertEquals(1, loadedProfile.getTripSummaries().size(), "相同目的地+时间窗口应该合并");

        // 验证：访问次数应该累加
        LongTermMemoryManager.TripSummary merged = loadedProfile.getTripSummaries().get(0);
        assertEquals(10, merged.getAccessCount(), "访问次数应该累加 (5+3+2=10)");

        // 验证：意图应该合并
        assertEquals(3, merged.getIntents().size(), "意图应该合并");
    }

    /**
     * 测试5：记录访问功能
     */
    @Test
    void testRecordAccess() {
        String userId = "user_access";
        LongTermMemoryManager.UserProfile profile = new LongTermMemoryManager.UserProfile(userId);

        long now = System.currentTimeMillis();
        LongTermMemoryManager.TripSummary trip = createTrip("北京", now, 0);
        profile.addTripSummary(trip);

        memoryManager.saveUserProfile(profile);

        // 记录3次访问
        memoryManager.recordMemoryAccess(userId, "北京");
        memoryManager.recordMemoryAccess(userId, "北京");
        memoryManager.recordMemoryAccess(userId, "北京");

        // 验证访问次数
        LongTermMemoryManager.UserProfile loadedProfile = memoryManager.getUserProfile(userId);
        LongTermMemoryManager.TripSummary loadedTrip = loadedProfile.getTripSummaries().get(0);

        assertEquals(3, loadedTrip.getAccessCount(), "访问次数应该正确记录");
        assertTrue(loadedTrip.getLastAccessTime() > 0, "最后访问时间应该被记录");
    }

    /**
     * 测试6：获取记忆统计信息
     */
    @Test
    void testMemoryStats() {
        String userId = "user_stats";
        LongTermMemoryManager.UserProfile profile = new LongTermMemoryManager.UserProfile(userId);

        long now = System.currentTimeMillis();

        // 添加多条记忆
        LongTermMemoryManager.TripSummary trip1 = createTrip("北京", now, 10);
        LongTermMemoryManager.TripSummary trip2 = createTrip("上海", now, 5);
        LongTermMemoryManager.TripSummary trip3 = createTrip("深圳", now, 2);

        profile.addTripSummary(trip1);
        profile.addTripSummary(trip2);
        profile.addTripSummary(trip3);

        profile.incrementCityVisitCount("北京");
        profile.incrementCityVisitCount("上海");

        memoryManager.saveUserProfile(profile);

        // 获取统计信息
        LongTermMemoryManager.MemoryStats stats = memoryManager.getMemoryStats(userId);

        assertEquals(3, stats.getTotalMemories(), "总记忆数应该正确");
        assertEquals(2, stats.getTotalCities(), "总城市数应该正确");
        assertEquals("北京", stats.getMostAccessedDestination(), "最常访问的目的地应该是北京");
        assertEquals(10, stats.getMaxAccessCount(), "最大访问次数应该是10");
    }

    /**
     * 辅助方法：创建行程摘要
     */
    private LongTermMemoryManager.TripSummary createTrip(String destination, long timestamp, int accessCount) {
        LongTermMemoryManager.TripSummary trip = new LongTermMemoryManager.TripSummary();
        trip.setDestination(destination);
        trip.setTimestamp(timestamp);
        trip.setIntents(new ArrayList<>());

        // 设置访问次数
        for (int i = 0; i < accessCount; i++) {
            trip.recordAccess();
        }

        return trip;
    }
}
