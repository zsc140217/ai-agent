package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 三路召回测试
 *
 * 测试目标：
 * 1. 验证BM25检索能否正常工作
 * 2. 验证三路召回能否正常融合
 * 3. 对比单路、双路、三路的召回效果
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class ThreeWayRetrievalTest {

    @Resource
    private BM25Retriever bm25Retriever;

    @Resource
    private EnterpriseHybridRetriever enterpriseHybridRetriever;

    /**
     * 测试1：BM25检索
     */
    @Test
    public void testBM25Retrieval() {
        log.info("========== 测试BM25检索 ==========");

        String query = "北京出差住宿标准";
        List<Document> results = bm25Retriever.search(query, 5);

        log.info("查询: {}", query);
        log.info("召回数量: {}", results.size());

        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String preview = doc.getText().substring(0, Math.min(100, doc.getText().length()));
            log.info("  [{}] {}", i + 1, preview);
        }

        assert results.size() > 0 : "BM25检索应该召回至少1个文档";
    }

    /**
     * 测试2：三路召回
     */
    @Test
    public void testThreeWayRetrieval() {
        log.info("========== 测试三路召回 ==========");

        String query = "去魔都出差住宿能报多少";
        List<Document> results = enterpriseHybridRetriever.retrieve(query, 5);

        log.info("查询: {}", query);
        log.info("最终召回数量: {}", results.size());

        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String preview = doc.getText().substring(0, Math.min(100, doc.getText().length()));
            log.info("  [{}] {}", i + 1, preview);
        }

        assert results.size() > 0 : "三路召回应该召回至少1个文档";
    }

    /**
     * 测试3：对比不同查询类型
     */
    @Test
    public void testDifferentQueryTypes() {
        log.info("========== 测试不同查询类型 ==========");

        String[] queries = {
                "北京住宿标准",                    // 精确查询
                "去魔都出差住宿能报多少",          // 口语化查询
                "北京不能住五星级酒店吗",          // 否定查询
                "出差30天伙食补助总共多少",        // 计算查询
                "北京和上海的住宿标准哪个高"       // 对比查询
        };

        for (String query : queries) {
            log.info("\n查询: {}", query);

            List<Document> results = enterpriseHybridRetriever.retrieve(query, 3);

            log.info("召回数量: {}", results.size());
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                String preview = doc.getText().substring(0, Math.min(80, doc.getText().length()));
                log.info("  [{}] {}", i + 1, preview);
            }
        }
    }

    /**
     * 测试4：性能测试
     */
    @Test
    public void testPerformance() {
        log.info("========== 性能测试 ==========");

        String query = "北京出差住宿标准";
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

        assert avgTime < 5000 : "平均耗时应该小于5秒";
    }
}
