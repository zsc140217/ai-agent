package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 重排序测试
 *
 * 测试目标：
 * 1. 验证重排序能否正常工作
 * 2. 对比重排前后的召回效果
 * 3. 测试重排序性能
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class RerankerTest {

    @Resource
    private EnterpriseHybridRetriever enterpriseHybridRetriever;

    @Resource
    private CrossEncoderReranker reranker;

    /**
     * 测试1：重排序前后对比
     */
    @Test
    public void testRerankingEffect() {
        log.info("========== 测试重排序效果 ==========");

        String query = "北京出差住宿标准";

        // 获取重排后的结果
        List<Document> results = enterpriseHybridRetriever.retrieve(query, 5);

        log.info("查询: {}", query);
        log.info("重排后Top-5:");

        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String content = doc.getText();
            if (content == null) content = "";
            String preview = content.substring(0, Math.min(100, content.length()));
            log.info("  [{}] {}", i + 1, preview);
        }

        assert results.size() > 0 : "重排序应该返回至少1个文档";
    }

    /**
     * 测试2：重排序性能
     */
    @Test
    public void testRerankingPerformance() {
        log.info("========== 测试重排序性能 ==========");

        String query = "上海住宿标准";
        int rounds = 5;

        long totalTime = 0;
        for (int i = 0; i < rounds; i++) {
            long start = System.currentTimeMillis();
            List<Document> results = enterpriseHybridRetriever.retrieve(query, 5);
            long cost = System.currentTimeMillis() - start;

            totalTime += cost;
            log.info("第{}轮，耗时: {}ms, 召回数量: {}", i + 1, cost, results.size());
        }

        long avgTime = totalTime / rounds;
        log.info("平均耗时: {}ms", avgTime);

        assert avgTime < 10000 : "平均耗时应该小于10秒";
    }

    /**
     * 测试3：对比不同查询类型
     */
    @Test
    public void testDifferentQueryTypes() {
        log.info("========== 测试不同查询类型的重排序效果 ==========");

        String[] queries = {
                "北京住宿标准",                    // 精确查询
                "去魔都出差住宿能报多少",          // 口语化查询
                "北京不能住五星级酒店吗",          // 否定查询
                "北京和上海的住宿标准哪个高"       // 对比查询
        };

        for (String query : queries) {
            log.info("\n查询: {}", query);

            List<Document> results = enterpriseHybridRetriever.retrieve(query, 3);

            log.info("重排后Top-3:");
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                String content = doc.getText();
                if (content == null) content = "";
                String preview = content.substring(0, Math.min(80, content.length()));
                log.info("  [{}] {}", i + 1, preview);
            }
        }
    }

    /**
     * 测试4：单独测试Reranker组件
     */
    @Test
    public void testRerankerComponent() {
        log.info("========== 测试Reranker组件 ==========");

        String query = "北京住宿标准";

        // 构造测试文档
        List<Document> testDocs = List.of(
                new Document("北京一类城市住宿标准500元"),
                new Document("上海一类城市住宿标准500元"),
                new Document("北京交通标准高铁二等座"),
                new Document("北京伙食补助标准100元每天"),
                new Document("深圳住宿标准400元")
        );

        log.info("查询: {}", query);
        log.info("待重排文档数: {}", testDocs.size());

        // 执行重排序
        List<Document> rerankedDocs = reranker.rerank(query, testDocs, 3);

        log.info("重排后Top-3:");
        for (int i = 0; i < rerankedDocs.size(); i++) {
            Document doc = rerankedDocs.get(i);
            log.info("  [{}] {}", i + 1, doc.getText());
        }

        assert rerankedDocs.size() > 0 : "重排序应该返回至少1个文档";
    }

    /**
     * 测试5：压力测试
     */
    @Test
    public void testRerankingStress() {
        log.info("========== 重排序压力测试 ==========");

        String[] queries = {
                "北京住宿标准",
                "上海交通标准",
                "深圳伙食补助",
                "杭州出差政策",
                "广州住宿费用"
        };

        long totalTime = 0;
        int totalQueries = queries.length * 3;  // 每个查询执行3次

        for (int round = 0; round < 3; round++) {
            for (String query : queries) {
                long start = System.currentTimeMillis();
                List<Document> results = enterpriseHybridRetriever.retrieve(query, 5);
                long cost = System.currentTimeMillis() - start;
                totalTime += cost;

                log.info("查询: {}, 耗时: {}ms, 结果数: {}", query, cost, results.size());
            }
        }

        long avgTime = totalTime / totalQueries;
        log.info("========== 压力测试完成 ==========");
        log.info("总查询数: {}", totalQueries);
        log.info("总耗时: {}ms", totalTime);
        log.info("平均耗时: {}ms", avgTime);
        log.info("QPS: {}", 1000.0 / avgTime);

        assert avgTime < 10000 : "平均耗时应该小于10秒";
    }
}
