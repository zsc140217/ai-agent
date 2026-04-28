package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 重排序对比测试
 *
 * 测试目标：
 * 1. 对比不用重排序 vs 用重排序的召回效果
 * 2. 验证重排序是否真的提升了准确率
 * 3. 对比不同查询类型的效果差异
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class RerankerComparisonTest {

    @Resource
    private EnterpriseHybridRetriever enterpriseHybridRetriever;

    @Resource
    private BM25Retriever bm25Retriever;

    /**
     * 测试1：对比不用重排序 vs 用重排序
     */
    @Test
    public void testWithAndWithoutReranking() {
        log.info("========== 对比测试：不用重排序 vs 用重排序 ==========");

        String[] testQueries = {
                "北京出差住宿标准",
                "去魔都出差住宿能报多少",
                "北京不能住五星级酒店吗",
                "上海和深圳的住宿标准哪个高"
        };

        for (String query : testQueries) {
            log.info("\n========================================");
            log.info("查询: {}", query);
            log.info("========================================");

            // 方案1：不用重排序（只用RRF融合）
            log.info("\n【方案1：不用重排序】");
            List<Document> resultsWithoutRerank = retrieveWithoutReranking(query, 5);

            log.info("召回Top-5:");
            for (int i = 0; i < resultsWithoutRerank.size(); i++) {
                Document doc = resultsWithoutRerank.get(i);
                String content = doc.getText();
                if (content == null) content = "";
                String preview = content.substring(0, Math.min(80, content.length()));
                log.info("  [{}] {}", i + 1, preview);
            }

            // 方案2：用重排序
            log.info("\n【方案2：用重排序】");
            List<Document> resultsWithRerank = enterpriseHybridRetriever.retrieve(query, 5);

            log.info("召回Top-5:");
            for (int i = 0; i < resultsWithRerank.size(); i++) {
                Document doc = resultsWithRerank.get(i);
                String content = doc.getText();
                if (content == null) content = "";
                String preview = content.substring(0, Math.min(80, content.length()));
                log.info("  [{}] {}", i + 1, preview);
            }

            // 对比分析
            log.info("\n【对比分析】");
            analyzeResults(query, resultsWithoutRerank, resultsWithRerank);
        }
    }

    /**
     * 测试2：详细对比单个查询
     */
    @Test
    public void testDetailedComparison() {
        log.info("========== 详细对比测试 ==========");

        String query = "北京出差住宿标准";

        log.info("查询: {}", query);
        log.info("");

        // 不用重排序
        log.info("========== 方案1：不用重排序 ==========");
        long start1 = System.currentTimeMillis();
        List<Document> resultsWithoutRerank = retrieveWithoutReranking(query, 5);
        long cost1 = System.currentTimeMillis() - start1;

        log.info("耗时: {}ms", cost1);
        log.info("召回数量: {}", resultsWithoutRerank.size());
        log.info("\nTop-5文档:");
        for (int i = 0; i < resultsWithoutRerank.size(); i++) {
            Document doc = resultsWithoutRerank.get(i);
            String content = doc.getText();
            if (content == null) content = "";
            log.info("\n[{}] {}", i + 1, content.substring(0, Math.min(150, content.length())));
        }

        // 用重排序
        log.info("\n========== 方案2：用重排序 ==========");
        long start2 = System.currentTimeMillis();
        List<Document> resultsWithRerank = enterpriseHybridRetriever.retrieve(query, 5);
        long cost2 = System.currentTimeMillis() - start2;

        log.info("耗时: {}ms", cost2);
        log.info("召回数量: {}", resultsWithRerank.size());
        log.info("\nTop-5文档:");
        for (int i = 0; i < resultsWithRerank.size(); i++) {
            Document doc = resultsWithRerank.get(i);
            String content = doc.getText();
            if (content == null) content = "";
            log.info("\n[{}] {}", i + 1, content.substring(0, Math.min(150, content.length())));
        }

        // 性能对比
        log.info("\n========== 性能对比 ==========");
        log.info("不用重排序耗时: {}ms", cost1);
        log.info("用重排序耗时: {}ms", cost2);
        log.info("重排序额外耗时: {}ms", cost2 - cost1);
        log.info("延迟增加: {}%", (cost2 - cost1) * 100.0 / cost1);
    }

    /**
     * 测试3：准确率对比（需要人工标注）
     */
    @Test
    public void testAccuracyComparison() {
        log.info("========== 准确率对比测试 ==========");
        log.info("说明：请人工判断每个结果是否相关，统计准确率");
        log.info("");

        String[] testQueries = {
                "北京住宿标准",
                "上海出差住宿能报多少",
                "深圳一类城市住宿费用",
                "杭州住宿标准是多少",
                "广州出差住宿报销标准"
        };

        int totalQueries = testQueries.length;
        int relevantWithoutRerank = 0;
        int relevantWithRerank = 0;

        for (String query : testQueries) {
            log.info("\n查询: {}", query);

            // 不用重排序
            List<Document> resultsWithoutRerank = retrieveWithoutReranking(query, 3);
            log.info("\n【不用重排序】Top-3:");
            for (int i = 0; i < resultsWithoutRerank.size(); i++) {
                Document doc = resultsWithoutRerank.get(i);
                String content = doc.getText();
                if (content == null) content = "";
                String preview = content.substring(0, Math.min(100, content.length()));
                log.info("  [{}] {}", i + 1, preview);
            }

            // 用重排序
            List<Document> resultsWithRerank = enterpriseHybridRetriever.retrieve(query, 3);
            log.info("\n【用重排序】Top-3:");
            for (int i = 0; i < resultsWithRerank.size(); i++) {
                Document doc = resultsWithRerank.get(i);
                String content = doc.getText();
                if (content == null) content = "";
                String preview = content.substring(0, Math.min(100, content.length()));
                log.info("  [{}] {}", i + 1, preview);
            }

            log.info("\n请人工判断：");
            log.info("1. 不用重排序的Top-3中，有几个是相关的？");
            log.info("2. 用重排序的Top-3中，有几个是相关的？");
            log.info("----------------------------------------");
        }

        log.info("\n========== 准确率统计 ==========");
        log.info("总查询数: {}", totalQueries);
        log.info("请根据上面的结果，人工统计准确率");
        log.info("不用重排序准确率 = 相关文档数 / (查询数 * 3)");
        log.info("用重排序准确率 = 相关文档数 / (查询数 * 3)");
    }

    /**
     * 不用重排序的召回（只用RRF融合）
     */
    private List<Document> retrieveWithoutReranking(String query, int topK) {
        // 直接使用BM25结果（简化版，不用重排序）
        return bm25Retriever.search(query, topK);
    }

    /**
     * 分析两个结果的差异
     */
    private void analyzeResults(String query, List<Document> withoutRerank, List<Document> withRerank) {
        // 计算Top-1是否相同
        if (!withoutRerank.isEmpty() && !withRerank.isEmpty()) {
            String top1Without = withoutRerank.get(0).getText();
            String top1With = withRerank.get(0).getText();

            if (top1Without != null && top1With != null) {
                boolean sameTop1 = top1Without.equals(top1With);
                log.info("Top-1是否相同: {}", sameTop1 ? "是" : "否");

                if (!sameTop1) {
                    log.info("重排序改变了Top-1结果");
                }
            }
        }

        // 计算重叠度
        int overlap = 0;
        for (Document doc1 : withoutRerank) {
            for (Document doc2 : withRerank) {
                if (doc1.getText() != null && doc1.getText().equals(doc2.getText())) {
                    overlap++;
                    break;
                }
            }
        }

        double overlapRate = (double) overlap / Math.max(withoutRerank.size(), 1) * 100;
        log.info("结果重叠度: {}% ({}/{})", String.format("%.1f", overlapRate), overlap, withoutRerank.size());

        if (overlapRate < 50) {
            log.info("重排序显著改变了召回结果");
        }
    }
}
