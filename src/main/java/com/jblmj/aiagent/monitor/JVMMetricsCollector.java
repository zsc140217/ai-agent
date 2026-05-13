package com.jblmj.aiagent.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.LinkedList;
import java.util.List;

/**
 * JVM指标收集器
 * 每10秒收集一次JVM指标，保留最近1小时的历史数据（360条）
 */
@Component
public class JVMMetricsCollector {

    private static final int MAX_HISTORY_SIZE = 360; // 1小时的数据（10秒 * 360 = 3600秒）

    private final LinkedList<JVMMetrics> metricsHistory = new LinkedList<>();

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    private final List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

    /**
     * 每10秒收集一次指标
     */
    @Scheduled(fixedRate = 10000)
    public void collectMetrics() {
        JVMMetrics metrics = new JVMMetrics();

        // 收集堆内存信息
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        metrics.setHeapUsed(heapMemory.getUsed());
        metrics.setHeapMax(heapMemory.getMax());
        metrics.setHeapCommitted(heapMemory.getCommitted());
        metrics.setHeapUsagePercent(heapMemory.getMax() > 0
            ? (double) heapMemory.getUsed() / heapMemory.getMax() * 100
            : 0);

        // 收集非堆内存信息
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        metrics.setNonHeapUsed(nonHeapMemory.getUsed());
        metrics.setNonHeapMax(nonHeapMemory.getMax());
        metrics.setNonHeapCommitted(nonHeapMemory.getCommitted());

        // 收集GC信息
        long youngGCCount = 0;
        long youngGCTime = 0;
        long oldGCCount = 0;
        long oldGCTime = 0;

        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            String gcName = gcBean.getName().toLowerCase();
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();

            if (count < 0) count = 0;
            if (time < 0) time = 0;

            // Young GC: G1 Young Generation, PS Scavenge, Copy, ParNew
            if (gcName.contains("young") || gcName.contains("scavenge") ||
                gcName.contains("copy") || gcName.contains("parnew")) {
                youngGCCount += count;
                youngGCTime += time;
            }
            // Old GC: G1 Old Generation, PS MarkSweep, MarkSweepCompact, ConcurrentMarkSweep
            else if (gcName.contains("old") || gcName.contains("marksweep") ||
                     gcName.contains("cms") || gcName.contains("tenured")) {
                oldGCCount += count;
                oldGCTime += time;
            }
        }

        metrics.setYoungGCCount(youngGCCount);
        metrics.setYoungGCTime(youngGCTime);
        metrics.setOldGCCount(oldGCCount);
        metrics.setOldGCTime(oldGCTime);

        // 收集线程信息
        metrics.setThreadCount(threadMXBean.getThreadCount());
        metrics.setPeakThreadCount(threadMXBean.getPeakThreadCount());
        metrics.setDaemonThreadCount(threadMXBean.getDaemonThreadCount());

        // 收集类加载信息
        metrics.setLoadedClassCount(classLoadingMXBean.getLoadedClassCount());
        metrics.setTotalLoadedClassCount(classLoadingMXBean.getTotalLoadedClassCount());
        metrics.setUnloadedClassCount(classLoadingMXBean.getUnloadedClassCount());

        // 添加到历史记录
        synchronized (metricsHistory) {
            metricsHistory.addLast(metrics);
            if (metricsHistory.size() > MAX_HISTORY_SIZE) {
                metricsHistory.removeFirst();
            }
        }
    }

    /**
     * 获取当前JVM指标
     */
    public JVMMetrics getCurrentMetrics() {
        synchronized (metricsHistory) {
            return metricsHistory.isEmpty() ? null : metricsHistory.getLast();
        }
    }

    /**
     * 获取历史指标数据
     */
    public List<JVMMetrics> getMetricsHistory() {
        synchronized (metricsHistory) {
            return new LinkedList<>(metricsHistory);
        }
    }

    /**
     * 获取最近N条指标数据
     */
    public List<JVMMetrics> getRecentMetrics(int count) {
        synchronized (metricsHistory) {
            int size = metricsHistory.size();
            int fromIndex = Math.max(0, size - count);
            return new LinkedList<>(metricsHistory.subList(fromIndex, size));
        }
    }
}
