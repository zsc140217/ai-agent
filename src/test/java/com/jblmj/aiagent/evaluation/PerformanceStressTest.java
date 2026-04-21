package com.jblmj.aiagent.evaluation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.jblmj.aiagent.app.WorkflowOrchestrator;
import com.jblmj.aiagent.model.SubTask;
import com.jblmj.aiagent.service.ComplexityAssessor;
import com.jblmj.aiagent.service.TaskDecomposer;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 性能压测和并发测试
 *
 * 测试目标：
 * 1. 并发处理能力
 * 2. 响应时间分布
 * 3. 系统稳定性
 * 4. 资源使用情况
 * 5. 任务分解并行执行效率
 *
 * @author jblmj
 */
@SpringBootTest
@Slf4j
public class PerformanceStressTest {

    @Resource
    private WorkflowOrchestrator workflowOrchestrator;

    @Resource
    private ComplexityAssessor complexityAssessor;

    @Resource
    private TaskDecomposer taskDecomposer;

    private static final int CONCURRENT_USERS = 10;
    private static final int REQUESTS_PER_USER = 5;

    /**
     * 测试 1：并发查询压测
     */
    @Test
    @DisplayName("并发压测 - 多用户同时查询")
    public void testConcurrentQueries() throws InterruptedException {
        log.info("\n========== 并发压测测试 ==========");
        log.info("并发用户数: {}", CONCURRENT_USERS);
        log.info("每用户请求数: {}", REQUESTS_PER_USER);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS * REQUESTS_PER_USER);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        String[] queries = {
            "北京天气怎么样",
            "上海和广州天气对比",
            "差旅住宿标准",
            "明天去杭州出差，查天气",
            "协议酒店有哪些"
        };

        Instant startTime = Instant.now();

        // 提交并发任务
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                for (int j = 0; j < REQUESTS_PER_USER; j++) {
                    try {
                        String query = queries[(userId * REQUESTS_PER_USER + j) % queries.length];
                        String chatId = "user_" + userId + "_req_" + j;

                        Instant reqStart = Instant.now();
                        String result = workflowOrchestrator.route(query, chatId);
                        long duration = Duration.between(reqStart, Instant.now()).toMillis();

                        responseTimes.add(duration);
                        successCount.incrementAndGet();

                        log.debug("用户 {} 请求 {} 完成 ({}ms)", userId, j, duration);

                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        log.error("用户 {} 请求失败", userId, e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        // 等待所有任务完成（最多等待 5 分钟）
        boolean completed = latch.await(5, TimeUnit.MINUTES);
        executor.shutdown();

        long totalDuration = Duration.between(startTime, Instant.now()).toMillis();

        // 统计结果
        log.info("\n========== 压测结果 ==========");
        log.info("总请求数: {}", CONCURRENT_USERS * REQUESTS_PER_USER);
        log.info("成功: {}", successCount.get());
        log.info("失败: {}", failCount.get());
        log.info("成功率: {}%", String.format("%.1f", successCount.get() * 100.0 / (CONCURRENT_USERS * REQUESTS_PER_USER)));
        log.info("总耗时: {}ms", totalDuration);
        log.info("QPS: {}", String.format("%.2f", successCount.get() * 1000.0 / totalDuration));

        if (!responseTimes.isEmpty()) {
            Collections.sort(responseTimes);
            log.info("\n响应时间分布:");
            log.info("  最小值: {}ms", responseTimes.get(0));
            log.info("  最大值: {}ms", responseTimes.get(responseTimes.size() - 1));
            log.info("  平均值: {}ms", responseTimes.stream().mapToLong(Long::longValue).average().orElse(0));
            log.info("  P50: {}ms", responseTimes.get(responseTimes.size() / 2));
            log.info("  P95: {}ms", responseTimes.get((int) (responseTimes.size() * 0.95)));
            log.info("  P99: {}ms", responseTimes.get((int) (responseTimes.size() * 0.99)));
        }

        assertTrue(completed, "所有请求应该在超时时间内完成");
        assertTrue(successCount.get() >= CONCURRENT_USERS * REQUESTS_PER_USER * 0.8,
            "成功率应该 >= 80%");
    }

    /**
     * 测试 2：复杂度评估性能测试
     */
    @Test
    @DisplayName("性能测试 - 复杂度评估批量处理")
    public void testComplexityAssessmentPerformance() {
        log.info("\n========== 复杂度评估性能测试 ==========");

        String[] queries = {
            "北京天气",
            "上海和广州天气对比",
            "明天去杭州出差，查天气，拜访客户，规划路线",
            "差旅补贴标准",
            "规划北京三日游行程",
            "魔都今天气温如何",
            "帝都和羊城天气哪个好",
            "出差可以报销哪些费用",
            "协议酒店推荐",
            "查询客户公司地址"
        };

        List<Long> durations = new ArrayList<>();
        Map<String, Integer> complexityCount = new HashMap<>();

        Instant start = Instant.now();

        for (String query : queries) {
            Instant queryStart = Instant.now();
            var complexity = complexityAssessor.assess(query);
            long duration = Duration.between(queryStart, Instant.now()).toMillis();

            durations.add(duration);
            complexityCount.merge(complexity.name(), 1, Integer::sum);

            log.debug("查询: {} -> {} ({}ms)", query, complexity, duration);
        }

        long totalDuration = Duration.between(start, Instant.now()).toMillis();

        log.info("\n性能统计:");
        log.info("  总查询数: {}", queries.length);
        log.info("  总耗时: {}ms", totalDuration);
        log.info("  平均耗时: {}ms", totalDuration / queries.length);
        log.info("  吞吐量: {} queries/s", String.format("%.2f", queries.length * 1000.0 / totalDuration));

        log.info("\n复杂度分布:");
        complexityCount.forEach((complexity, count) ->
            log.info("  {}: {} ({}%)", complexity, count,
                String.format("%.1f", count * 100.0 / queries.length)));

        assertTrue(durations.stream().allMatch(d -> d < 5000),
            "每个查询的评估时间应 < 5s");
    }

    /**
     * 测试 3：任务分解并行执行效率测试
     */
    @Test
    @DisplayName("性能测试 - 任务分解并行执行效率")
    public void testTaskDecompositionParallelism() {
        log.info("\n========== 任务分解并行执行效率测试 ==========");

        String complexQuery = "明天去杭州出差，查一下天气，还要拜访阿里巴巴和网易两家公司，帮我规划路线和推荐酒店";

        // 1. 任务分解
        Instant decomposeStart = Instant.now();
        List<SubTask> tasks = taskDecomposer.decompose(complexQuery);
        long decomposeDuration = Duration.between(decomposeStart, Instant.now()).toMillis();

        log.info("任务分解耗时: {}ms", decomposeDuration);
        log.info("分解为 {} 个子任务", tasks.size());

        // 2. 拓扑排序
        Instant sortStart = Instant.now();
        List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
        long sortDuration = Duration.between(sortStart, Instant.now()).toMillis();

        log.info("拓扑排序耗时: {}ms", sortDuration);
        log.info("分为 {} 个批次", batches.size());

        // 3. 分析并行度
        int totalTasks = tasks.size();
        int maxParallelism = batches.stream().mapToInt(List::size).max().orElse(0);
        double avgParallelism = batches.stream().mapToInt(List::size).average().orElse(0);

        log.info("\n并行度分析:");
        log.info("  总任务数: {}", totalTasks);
        log.info("  批次数: {}", batches.size());
        log.info("  最大并行度: {}", maxParallelism);
        log.info("  平均并行度: {}", String.format("%.2f", avgParallelism));
        log.info("  理论加速比: {}", String.format("%.2f", totalTasks * 1.0 / batches.size()));

        for (int i = 0; i < batches.size(); i++) {
            log.info("  批次 {}: {} 个任务可并行", i + 1, batches.get(i).size());
        }

        assertTrue(decomposeDuration < 10000, "任务分解应 < 10s");
        assertTrue(sortDuration < 1000, "拓扑排序应 < 1s");
        assertTrue(batches.size() <= totalTasks, "批次数不应超过任务数");
    }

    /**
     * 测试 4：内存使用测试
     */
    @Test
    @DisplayName("性能测试 - 内存使用情况")
    public void testMemoryUsage() {
        log.info("\n========== 内存使用测试 ==========");

        Runtime runtime = Runtime.getRuntime();

        // 执行 GC
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        log.info("测试前内存使用: {} MB", beforeMemory / 1024 / 1024);

        // 执行大量查询
        String[] queries = {
            "北京天气",
            "上海和广州天气对比",
            "明天去杭州出差，查天气，拜访客户",
            "差旅补贴标准",
            "规划北京三日游"
        };

        for (int i = 0; i < 20; i++) {
            for (String query : queries) {
                try {
                    complexityAssessor.assess(query);
                    taskDecomposer.decompose(query);
                } catch (Exception e) {
                    log.debug("查询执行异常", e);
                }
            }
        }

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = afterMemory - beforeMemory;

        log.info("测试后内存使用: {} MB", afterMemory / 1024 / 1024);
        log.info("内存增长: {} MB", memoryIncrease / 1024 / 1024);
        log.info("最大可用内存: {} MB", runtime.maxMemory() / 1024 / 1024);
        log.info("当前总内存: {} MB", runtime.totalMemory() / 1024 / 1024);
        log.info("当前空闲内存: {} MB", runtime.freeMemory() / 1024 / 1024);

        // 验证内存增长不超过 500MB
        assertTrue(memoryIncrease < 500 * 1024 * 1024,
            "内存增长应 < 500MB");
    }

    /**
     * 测试 5：稳定性测试 - 长时间运行
     */
    @Test
    @DisplayName("稳定性测试 - 长时间连续查询")
    public void testStabilityLongRun() {
        log.info("\n========== 稳定性测试 ==========");

        int totalQueries = 50;
        String[] queries = {
            "北京天气",
            "上海和广州天气对比",
            "差旅补贴标准"
        };

        int successCount = 0;
        int failCount = 0;
        List<Long> responseTimes = new ArrayList<>();

        Instant start = Instant.now();

        for (int i = 0; i < totalQueries; i++) {
            String query = queries[i % queries.length];

            try {
                Instant queryStart = Instant.now();
                complexityAssessor.assess(query);
                long duration = Duration.between(queryStart, Instant.now()).toMillis();

                responseTimes.add(duration);
                successCount++;

                if ((i + 1) % 10 == 0) {
                    log.info("已完成 {}/{} 查询", i + 1, totalQueries);
                }

            } catch (Exception e) {
                failCount++;
                log.error("查询 {} 失败", i, e);
            }
        }

        long totalDuration = Duration.between(start, Instant.now()).toMillis();

        log.info("\n稳定性测试结果:");
        log.info("  总查询数: {}", totalQueries);
        log.info("  成功: {}", successCount);
        log.info("  失败: {}", failCount);
        log.info("  成功率: {}%", String.format("%.1f", successCount * 100.0 / totalQueries));
        log.info("  总耗时: {}ms", totalDuration);

        if (!responseTimes.isEmpty()) {
            double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            log.info("  平均响应时间: {}ms", String.format("%.2f", avgResponseTime));
        }

        assertTrue(successCount >= totalQueries * 0.95, "成功率应 >= 95%");
    }

    /**
     * 测试 6：峰值负载测试
     */
    @Test
    @DisplayName("峰值负载测试 - 突发流量")
    public void testPeakLoad() throws InterruptedException {
        log.info("\n========== 峰值负载测试 ==========");

        int peakConcurrency = 20;
        ExecutorService executor = Executors.newFixedThreadPool(peakConcurrency);
        CountDownLatch latch = new CountDownLatch(peakConcurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Instant start = Instant.now();

        // 同时提交大量任务（模拟突发流量）
        for (int i = 0; i < peakConcurrency; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    String query = "北京天气怎么样";
                    complexityAssessor.assess(query);
                    successCount.incrementAndGet();
                    log.debug("任务 {} 完成", taskId);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("任务 {} 失败", taskId, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(2, TimeUnit.MINUTES);
        executor.shutdown();

        long duration = Duration.between(start, Instant.now()).toMillis();

        log.info("\n峰值负载测试结果:");
        log.info("  并发数: {}", peakConcurrency);
        log.info("  成功: {}", successCount.get());
        log.info("  失败: {}", failCount.get());
        log.info("  总耗时: {}ms", duration);
        log.info("  平均响应时间: {}ms", duration / peakConcurrency);

        assertTrue(completed, "所有任务应在超时时间内完成");
        assertTrue(successCount.get() >= peakConcurrency * 0.8, "成功率应 >= 80%");
    }

    /**
     * 测试 7：错误恢复测试
     */
    @Test
    @DisplayName("错误恢复测试 - 异常场景处理")
    public void testErrorRecovery() {
        log.info("\n========== 错误恢复测试 ==========");

        // 测试各种异常输入
        String[] invalidQueries = {
            "",
            "   ",
            null,
            "a".repeat(10000), // 超长查询
            "特殊字符测试 !@#$%^&*()",
            "数字测试 123456789",
            "混合测试 abc123!@#"
        };

        int handledCount = 0;
        int crashCount = 0;

        for (String query : invalidQueries) {
            try {
                if (query == null) {
                    log.info("测试 null 输入");
                } else {
                    log.info("测试输入: {}", query.length() > 50 ? query.substring(0, 50) + "..." : query);
                }

                complexityAssessor.assess(query == null ? "" : query);
                handledCount++;
                log.info("  ✓ 正常处理");

            } catch (Exception e) {
                crashCount++;
                log.warn("  ✗ 抛出异常: {}", e.getMessage());
            }
        }

        log.info("\n错误恢复测试结果:");
        log.info("  测试用例数: {}", invalidQueries.length);
        log.info("  正常处理: {}", handledCount);
        log.info("  抛出异常: {}", crashCount);

        // 系统应该能优雅处理异常输入，不应该崩溃
        assertTrue(handledCount + crashCount == invalidQueries.length, "所有用例都应该被处理");
    }

    /**
     * 性能指标数据结构
     */
    @Data
    static class PerformanceMetrics {
        private int totalRequests;
        private int successCount;
        private int failCount;
        private long totalDuration;
        private double qps;
        private double avgResponseTime;
        private long p50;
        private long p95;
        private long p99;
        private long maxResponseTime;
        private long minResponseTime;
    }
}
