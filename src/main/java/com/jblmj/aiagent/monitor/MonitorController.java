package com.jblmj.aiagent.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JVM监控API控制器
 */
@RestController
@RequestMapping("/monitor/jvm")
public class MonitorController {

    @Autowired
    private JVMMetricsCollector metricsCollector;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    /**
     * 获取当前JVM状态
     */
    @GetMapping("/status")
    public Map<String, Object> getJVMStatus() {
        JVMMetrics current = metricsCollector.getCurrentMetrics();

        Map<String, Object> status = new HashMap<>();

        if (current != null) {
            status.put("timestamp", current.getTimestamp());

            // 堆内存
            Map<String, Object> heap = new HashMap<>();
            heap.put("used", formatBytes(current.getHeapUsed()));
            heap.put("max", formatBytes(current.getHeapMax()));
            heap.put("committed", formatBytes(current.getHeapCommitted()));
            heap.put("usagePercent", String.format("%.2f%%", current.getHeapUsagePercent()));
            status.put("heap", heap);

            // 非堆内存
            Map<String, Object> nonHeap = new HashMap<>();
            nonHeap.put("used", formatBytes(current.getNonHeapUsed()));
            nonHeap.put("max", current.getNonHeapMax() > 0 ? formatBytes(current.getNonHeapMax()) : "unlimited");
            nonHeap.put("committed", formatBytes(current.getNonHeapCommitted()));
            status.put("nonHeap", nonHeap);

            // GC信息
            Map<String, Object> gc = new HashMap<>();
            gc.put("youngGCCount", current.getYoungGCCount());
            gc.put("youngGCTime", current.getYoungGCTime() + "ms");
            gc.put("oldGCCount", current.getOldGCCount());
            gc.put("oldGCTime", current.getOldGCTime() + "ms");
            gc.put("totalGCTime", (current.getYoungGCTime() + current.getOldGCTime()) + "ms");
            status.put("gc", gc);

            // 线程信息
            Map<String, Object> threads = new HashMap<>();
            threads.put("current", current.getThreadCount());
            threads.put("peak", current.getPeakThreadCount());
            threads.put("daemon", current.getDaemonThreadCount());
            status.put("threads", threads);

            // 类加载信息
            Map<String, Object> classes = new HashMap<>();
            classes.put("loaded", current.getLoadedClassCount());
            classes.put("totalLoaded", current.getTotalLoadedClassCount());
            classes.put("unloaded", current.getUnloadedClassCount());
            status.put("classes", classes);
        }

        return status;
    }

    /**
     * 获取历史指标数据
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics(
            @RequestParam(defaultValue = "60") int count) {

        List<JVMMetrics> metrics = metricsCollector.getRecentMetrics(count);

        Map<String, Object> result = new HashMap<>();
        result.put("count", metrics.size());
        result.put("metrics", metrics);

        return result;
    }

    /**
     * 触发GC（仅用于测试）
     */
    @PostMapping("/gc")
    public Map<String, Object> triggerGC() {
        long beforeUsed = memoryMXBean.getHeapMemoryUsage().getUsed();

        System.gc();

        try {
            Thread.sleep(1000); // 等待GC完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long afterUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long freed = beforeUsed - afterUsed;

        Map<String, Object> result = new HashMap<>();
        result.put("beforeUsed", formatBytes(beforeUsed));
        result.put("afterUsed", formatBytes(afterUsed));
        result.put("freed", formatBytes(freed));
        result.put("message", "GC triggered (for testing only)");

        return result;
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
