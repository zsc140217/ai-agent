package com.jblmj.aiagent.performance;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量库压力测试
 * 模拟加载大量文档，观察内存使用和GC行为
 */
@SpringBootTest
public class VectorStoreLoadTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    @Test
    public void testLoadLargeDocuments() {
        System.out.println("========== 向量库压力测试开始 ==========");

        // 记录初始内存
        printMemoryUsage("测试开始");

        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        List<Document> documents = new ArrayList<>();

        // 模拟加载500个文档（每个文档约1KB）
        int documentCount = 500;
        System.out.println("开始生成 " + documentCount + " 个文档...");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < documentCount; i++) {
            String content = generateLargeContent(i);
            Document doc = new Document(content, Map.of("id", String.valueOf(i)));
            documents.add(doc);

            // 每100个文档打印一次进度
            if ((i + 1) % 100 == 0) {
                System.out.println("已生成 " + (i + 1) + " 个文档");
                printMemoryUsage("生成文档中");
            }
        }

        long generateTime = System.currentTimeMillis() - startTime;
        System.out.println("文档生成完成，耗时: " + generateTime + "ms");
        printMemoryUsage("文档生成完成");

        // 添加到向量库
        System.out.println("开始添加文档到向量库...");
        startTime = System.currentTimeMillis();

        vectorStore.add(documents);

        long addTime = System.currentTimeMillis() - startTime;
        System.out.println("文档添加完成，耗时: " + addTime + "ms");
        printMemoryUsage("文档添加完成");

        // 触发GC观察内存回收
        System.out.println("触发GC...");
        System.gc();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        printMemoryUsage("GC后");

        System.out.println("========== 向量库压力测试结束 ==========");
        System.out.println("总文档数: " + documentCount);
        System.out.println("生成耗时: " + generateTime + "ms");
        System.out.println("添加耗时: " + addTime + "ms");
    }

    @Test
    public void testMemoryLeak() {
        System.out.println("========== 内存泄漏检测测试 ==========");

        printMemoryUsage("测试开始");

        // 模拟多次加载和释放
        for (int round = 1; round <= 5; round++) {
            System.out.println("\n第 " + round + " 轮加载...");

            SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
            List<Document> documents = new ArrayList<>();

            for (int i = 0; i < 2000; i++) {
                String content = generateLargeContent(i);
                Document doc = new Document(content, Map.of("id", String.valueOf(i)));
                documents.add(doc);
            }

            vectorStore.add(documents);
            printMemoryUsage("第 " + round + " 轮加载完成");

            // 清空引用
            documents.clear();
            vectorStore = null;

            // 触发GC
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            printMemoryUsage("第 " + round + " 轮GC后");
        }

        System.out.println("\n========== 内存泄漏检测结束 ==========");
    }

    /**
     * 生成大文本内容（约1KB）
     */
    private String generateLargeContent(int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("文档编号: ").append(index).append("\n");
        sb.append("这是一个测试文档，用于模拟向量库的压力测试。");
        sb.append("内容包含了大量的文本数据，用于观察内存使用情况。");

        // 重复内容达到约1KB
        String baseContent = "企业差旅政策规定：员工出差需要提前申请，住宿标准根据城市等级确定。" +
                "一线城市住宿标准为500元/晚，二线城市为300元/晚，三线城市为200元/晚。" +
                "交通费用实报实销，但需要提供发票。餐饮补贴为100元/天。";

        for (int i = 0; i < 10; i++) {
            sb.append(baseContent);
        }

        return sb.toString();
    }

    /**
     * 打印内存使用情况
     */
    private void printMemoryUsage(String stage) {
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        long usedMB = heapMemory.getUsed() / (1024 * 1024);
        long maxMB = heapMemory.getMax() / (1024 * 1024);
        double usagePercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;

        System.out.printf("[%s] 堆内存: %d MB / %d MB (%.2f%%)%n",
                stage, usedMB, maxMB, usagePercent);
    }
}
