package com.jblmj.aiagent.service;

import com.jblmj.aiagent.model.SubTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 任务分解器测试
 *
 * 测试场景：
 * 1. 简单查询（单个任务）
 * 2. 中等复杂查询（多个独立任务，可并行）
 * 3. 复杂查询（有依赖关系的任务）
 * 4. 循环依赖检测
 */
@SpringBootTest
@Slf4j
public class TaskDecomposerTest {

    @Resource
    private TaskDecomposer taskDecomposer;

    /**
     * 测试场景 1：简单查询
     * 用户："去北京出差，住宿标准是多少"
     * 预期：1 个任务（查询住宿标准）
     */
    @Test
    public void testSimpleQuery() {
        String query = "去北京出差，住宿标准是多少";

        log.info("========== 测试场景 1：简单查询 ==========");
        log.info("用户查询: {}", query);

        List<SubTask> tasks = taskDecomposer.decompose(query);

        log.info("分解结果: {} 个任务", tasks.size());
        for (SubTask task : tasks) {
            log.info("  - 任务 {}: {} (类型: {}, 依赖: {})",
                task.getId(), task.getDescription(), task.getTaskType(), task.getDependsOn());
        }

        // 验证
        assert tasks.size() >= 1 : "应该至少有 1 个任务";
    }

    /**
     * 测试场景 2：中等复杂查询（多个独立任务）
     * 用户："明天去杭州出差，查一下天气，还要查一下住宿标准"
     * 预期：2 个任务（查天气、查住宿标准），无依赖关系，可并行
     */
    @Test
    public void testMediumQuery() {
        String query = "明天去杭州出差，查一下天气，还要查一下住宿标准";

        log.info("========== 测试场景 2：中等复杂查询 ==========");
        log.info("用户查询: {}", query);

        List<SubTask> tasks = taskDecomposer.decompose(query);

        log.info("分解结果: {} 个任务", tasks.size());
        for (SubTask task : tasks) {
            log.info("  - 任务 {}: {} (类型: {}, 依赖: {})",
                task.getId(), task.getDescription(), task.getTaskType(), task.getDependsOn());
        }

        // 拓扑排序
        List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
        log.info("排序结果: {} 批次", batches.size());
        for (int i = 0; i < batches.size(); i++) {
            log.info("  批次 {}: {} 个任务可并行执行", i, batches.get(i).size());
        }

        // 验证
        assert tasks.size() >= 2 : "应该至少有 2 个任务";
        assert batches.size() == 1 : "应该只有 1 批次（所有任务可并行）";
    }

    /**
     * 测试场景 3：复杂查询（有依赖关系）
     * 用户："明天去杭州出差，要拜访阿里巴巴，帮我规划一下路线"
     * 预期：3 个任务
     *   - 任务 0：查询杭州天气（无依赖）
     *   - 任务 1：查询阿里巴巴地址（无依赖）
     *   - 任务 2：查询路线（依赖任务 1）
     */
    @Test
    public void testComplexQuery() {
        String query = "明天去杭州出差，要拜访阿里巴巴，帮我规划一下路线";

        log.info("========== 测试场景 3：复杂查询（有依赖关系） ==========");
        log.info("用户查询: {}", query);

        List<SubTask> tasks = taskDecomposer.decompose(query);

        log.info("分解结果: {} 个任务", tasks.size());
        for (SubTask task : tasks) {
            log.info("  - 任务 {}: {} (类型: {}, 依赖: {})",
                task.getId(), task.getDescription(), task.getTaskType(), task.getDependsOn());
        }

        // 拓扑排序
        List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
        log.info("排序结果: {} 批次", batches.size());
        for (int i = 0; i < batches.size(); i++) {
            log.info("  批次 {}: {} 个任务可并行执行", i, batches.get(i).size());
            for (SubTask task : batches.get(i)) {
                log.info("    - 任务 {}: {}", task.getId(), task.getDescription());
            }
        }

        // 验证
        assert tasks.size() >= 2 : "应该至少有 2 个任务";
        assert batches.size() >= 2 : "应该至少有 2 批次（有依赖关系）";
    }

    /**
     * 测试场景 4：超级复杂查询
     * 用户："我要去北京出差3天，第一天拜访未来工业集团，第二天拜访字节跳动，
     *       帮我查一下天气、推荐酒店、规划路线，还要查一下住宿和伙食的报销标准"
     * 预期：多个任务，有复杂的依赖关系
     */
    @Test
    public void testSuperComplexQuery() {
        String query = "我要去北京出差3天，第一天拜访未来工业集团，第二天拜访字节跳动，" +
                      "帮我查一下天气、推荐酒店、规划路线，还要查一下住宿和伙食的报销标准";

        log.info("========== 测试场景 4：超级复杂查询 ==========");
        log.info("用户查询: {}", query);

        List<SubTask> tasks = taskDecomposer.decompose(query);

        log.info("分解结果: {} 个任务", tasks.size());
        for (SubTask task : tasks) {
            log.info("  - 任务 {}: {} (类型: {}, 依赖: {})",
                task.getId(), task.getDescription(), task.getTaskType(), task.getDependsOn());
        }

        // 拓扑排序
        List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
        log.info("排序结果: {} 批次", batches.size());
        for (int i = 0; i < batches.size(); i++) {
            log.info("  批次 {}: {} 个任务可并行执行", i, batches.get(i).size());
            for (SubTask task : batches.get(i)) {
                log.info("    - 任务 {}: {}", task.getId(), task.getDescription());
            }
        }

        // 验证
        assert tasks.size() >= 3 : "应该至少有 3 个任务";
    }

    /**
     * 测试场景 5：天气对比查询（可并行）
     * 用户："上海和广州哪个天气更好"
     * 预期：2 个任务（查上海天气、查广州天气），无依赖关系，可并行
     */
    @Test
    public void testWeatherComparisonQuery() {
        String query = "上海和广州哪个天气更好";

        log.info("========== 测试场景 5：天气对比查询 ==========");
        log.info("用户查询: {}", query);

        List<SubTask> tasks = taskDecomposer.decompose(query);

        log.info("分解结果: {} 个任务", tasks.size());
        for (SubTask task : tasks) {
            log.info("  - 任务 {}: {} (类型: {}, 依赖: {})",
                task.getId(), task.getDescription(), task.getTaskType(), task.getDependsOn());
        }

        // 拓扑排序
        List<List<SubTask>> batches = taskDecomposer.sortTasksByDependency(tasks);
        log.info("排序结果: {} 批次", batches.size());
        for (int i = 0; i < batches.size(); i++) {
            log.info("  批次 {}: {} 个任务可并行执行", i, batches.get(i).size());
        }

        // 验证
        assert tasks.size() >= 2 : "应该至少有 2 个任务";
        assert batches.size() == 1 : "应该只有 1 批次（两个天气查询可并行）";
    }
}
