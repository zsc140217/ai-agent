package com.jblmj.aiagent.monitor;

import java.time.LocalDateTime;

/**
 * JVM指标数据模型
 */
public class JVMMetrics {
    private LocalDateTime timestamp;

    // 堆内存
    private long heapUsed;
    private long heapMax;
    private long heapCommitted;
    private double heapUsagePercent;

    // 非堆内存
    private long nonHeapUsed;
    private long nonHeapMax;
    private long nonHeapCommitted;

    // GC信息
    private long youngGCCount;
    private long youngGCTime;
    private long oldGCCount;
    private long oldGCTime;

    // 线程信息
    private int threadCount;
    private int peakThreadCount;
    private int daemonThreadCount;

    // 类加载
    private long loadedClassCount;
    private long totalLoadedClassCount;
    private long unloadedClassCount;

    public JVMMetrics() {
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getHeapUsed() {
        return heapUsed;
    }

    public void setHeapUsed(long heapUsed) {
        this.heapUsed = heapUsed;
    }

    public long getHeapMax() {
        return heapMax;
    }

    public void setHeapMax(long heapMax) {
        this.heapMax = heapMax;
    }

    public long getHeapCommitted() {
        return heapCommitted;
    }

    public void setHeapCommitted(long heapCommitted) {
        this.heapCommitted = heapCommitted;
    }

    public double getHeapUsagePercent() {
        return heapUsagePercent;
    }

    public void setHeapUsagePercent(double heapUsagePercent) {
        this.heapUsagePercent = heapUsagePercent;
    }

    public long getNonHeapUsed() {
        return nonHeapUsed;
    }

    public void setNonHeapUsed(long nonHeapUsed) {
        this.nonHeapUsed = nonHeapUsed;
    }

    public long getNonHeapMax() {
        return nonHeapMax;
    }

    public void setNonHeapMax(long nonHeapMax) {
        this.nonHeapMax = nonHeapMax;
    }

    public long getNonHeapCommitted() {
        return nonHeapCommitted;
    }

    public void setNonHeapCommitted(long nonHeapCommitted) {
        this.nonHeapCommitted = nonHeapCommitted;
    }

    public long getYoungGCCount() {
        return youngGCCount;
    }

    public void setYoungGCCount(long youngGCCount) {
        this.youngGCCount = youngGCCount;
    }

    public long getYoungGCTime() {
        return youngGCTime;
    }

    public void setYoungGCTime(long youngGCTime) {
        this.youngGCTime = youngGCTime;
    }

    public long getOldGCCount() {
        return oldGCCount;
    }

    public void setOldGCCount(long oldGCCount) {
        this.oldGCCount = oldGCCount;
    }

    public long getOldGCTime() {
        return oldGCTime;
    }

    public void setOldGCTime(long oldGCTime) {
        this.oldGCTime = oldGCTime;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getPeakThreadCount() {
        return peakThreadCount;
    }

    public void setPeakThreadCount(int peakThreadCount) {
        this.peakThreadCount = peakThreadCount;
    }

    public int getDaemonThreadCount() {
        return daemonThreadCount;
    }

    public void setDaemonThreadCount(int daemonThreadCount) {
        this.daemonThreadCount = daemonThreadCount;
    }

    public long getLoadedClassCount() {
        return loadedClassCount;
    }

    public void setLoadedClassCount(long loadedClassCount) {
        this.loadedClassCount = loadedClassCount;
    }

    public long getTotalLoadedClassCount() {
        return totalLoadedClassCount;
    }

    public void setTotalLoadedClassCount(long totalLoadedClassCount) {
        this.totalLoadedClassCount = totalLoadedClassCount;
    }

    public long getUnloadedClassCount() {
        return unloadedClassCount;
    }

    public void setUnloadedClassCount(long unloadedClassCount) {
        this.unloadedClassCount = unloadedClassCount;
    }
}
