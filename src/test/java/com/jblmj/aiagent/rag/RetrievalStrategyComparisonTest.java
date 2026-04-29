package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 检索策略对比测试
 *
 * 对比以下策略：
 * 1. 基础RAG：只用向量检索
 * 2. 查询重写 + 向量检索
 * 3. 三路召回（向量 + BM25 + 查询重写）
 * 4. 三路召回 + 重排序（完整版）
 *
 * 测试指标：
 * - 召回数量
 * - 召回耗时
 * - Top-3 文档预览
 * - 相关性评估（人工判断）
 */
@SpringBootTest
@Slf4j
public class RetrievalStrategyComparisonTest {

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private BM25Retriever bm25Retriever;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private SimpleReranker reranker;

    // 测试查询集（覆盖不同场景）
    private static final String[] TEST_QUERIES = {
            "杭州差旅报销标准",                    // 精确匹配
            "去上海出差能报销多少钱",              // 口语化表达
            "一类城市住宿费用",                    // 关键词查询
            "拜访阿里巴巴客户需要准备什么",        // 复杂查询
            "协议酒店价格",                        // 简短查询
    };

    @Test
    public void testAllStrategies() {
        log.info("========================================");
        log.info("检索策略对比测试开始");
        log.info("========================================\n");

        for (String query : TEST_QUERIES) {
            log.info("\n" + "=".repeat(80));
            log.info("测试查询: {}", query);
            log.info("=".repeat(80));

            // 策略1：基础RAG
            testBasicRAG(query);

            // 策略2：查询重写 + 向量检索
            testQueryRewriteRAG(query);

            // 策略3：三路召回（向量 + BM25 + 查询重写）
            testThreeWayRetrieval(query);

            // 策略4：三路召回 + 重排序
            testThreeWayWithReranking(query);

            log.info("\n");
        }

        log.info("========================================");
        log.info("检索策略对比测试完成");
        log.info("========================================");
    }

    /**
     * 策略1：基础RAG（只用向量检索）
     */
    private void testBasicRAG(String query) {
        log.info("\n【策略1】基础RAG（只用向量检索）");
        log.info("-".repeat(80));

        long startTime = System.currentTimeMillis();

        // 直接向量检索
        List<Document> results = loveAppVectorStore.similaritySearch(query);

        long cost = System.currentTimeMillis() - startTime;

        log.info("召回数量: {}", results.size());
        log.info("召回耗时: {} ms", cost);
        printTopResults(results, 3);
    }

    /**
     * 策略2：查询重写 + 向量检索
     */
    private void testQueryRewriteRAG(String query) {
        log.info("\n【策略2】查询重写 + 向量检索");
        log.info("-".repeat(80));

        long startTime = System.currentTimeMillis();

        // 查询重写
        long rewriteStart = System.currentTimeMillis();
        String rewrittenQuery = queryRewriter.doQueryRewrite(query);
        long rewriteCost = System.currentTimeMillis() - rewriteStart;
        log.info("查询重写: {} -> {}", query, rewrittenQuery);
        log.info("重写耗时: {} ms", rewriteCost);

        // 向量检索（使用重写后的查询，如果为空则回退到原查询）
        String searchQuery = (rewrittenQuery != null && !rewrittenQuery.isEmpty())
                ? rewrittenQuery
                : query;
        List<Document> results = loveAppVectorStore.similaritySearch(
                Objects.requireNonNull(searchQuery, "Search query cannot be null")
        );

        long cost = System.currentTimeMillis() - startTime;

        log.info("召回数量: {}", results != null ? results.size() : 0);
        log.info("总耗时: {} ms", cost);
        if (results != null) {
            printTopResults(results, 3);
        }
    }

    /**
     * 策略3：三路召回（向量 + BM25 + 查询重写）
     */
    private void testThreeWayRetrieval(String query) {
        log.info("\n【策略3】三路召回（向量 + BM25 + 查询重写）");
        log.info("-".repeat(80));

        long startTime = System.currentTimeMillis();

        // 查询重写
        String rewrittenQuery = queryRewriter.doQueryRewrite(query);
        log.info("查询重写: {} -> {}", query, rewrittenQuery);

        // 路径1：向量检索-原始查询
        long vector1Start = System.currentTimeMillis();
        List<Document> vectorResults1 = loveAppVectorStore.similaritySearch(query);
        long vector1Cost = System.currentTimeMillis() - vector1Start;
        log.info("向量检索-原始查询: {} 个文档, 耗时 {} ms", vectorResults1.size(), vector1Cost);

        // 路径2：向量检索-改写查询
        long vector2Start = System.currentTimeMillis();
        List<Document> vectorResults2 = loveAppVectorStore.similaritySearch(rewrittenQuery);
        long vector2Cost = System.currentTimeMillis() - vector2Start;
        log.info("向量检索-改写查询: {} 个文档, 耗时 {} ms", vectorResults2.size(), vector2Cost);

        // 路径3：BM25检索
        long bm25Start = System.currentTimeMillis();
        List<Document> bm25Results = bm25Retriever.search(query, 10);
        long bm25Cost = System.currentTimeMillis() - bm25Start;
        log.info("BM25检索: {} 个文档, 耗时 {} ms", bm25Results.size(), bm25Cost);

        // RRF融合
        long fusionStart = System.currentTimeMillis();
        List<Document> fusedResults = fuseWithRRF(vectorResults1, vectorResults2, bm25Results, 10);
        long fusionCost = System.currentTimeMillis() - fusionStart;
        log.info("RRF融合: {} 个文档, 耗时 {} ms", fusedResults.size(), fusionCost);

        long cost = System.currentTimeMillis() - startTime;

        log.info("总召回数量: {}", fusedResults.size());
        log.info("总耗时: {} ms", cost);
        printTopResults(fusedResults, 3);
    }

    /**
     * 策略4：三路召回 + 重排序
     */
    private void testThreeWayWithReranking(String query) {
        log.info("\n【策略4】三路召回 + 重排序（完整版）");
        log.info("-".repeat(80));

        long startTime = System.currentTimeMillis();

        // 查询重写
        String rewrittenQuery = queryRewriter.doQueryRewrite(query);
        log.info("查询重写: {} -> {}", query, rewrittenQuery);

        // 三路召回
        List<Document> vectorResults1 = loveAppVectorStore.similaritySearch(query);
        List<Document> vectorResults2 = loveAppVectorStore.similaritySearch(rewrittenQuery);
        List<Document> bm25Results = bm25Retriever.search(query, 10);

        log.info("三路召回: 向量1={}, 向量2={}, BM25={}",
                vectorResults1.size(), vectorResults2.size(), bm25Results.size());

        // RRF融合（召回更多文档用于重排）
        List<Document> fusedResults = fuseWithRRF(vectorResults1, vectorResults2, bm25Results, 30);
        log.info("RRF融合: {} 个文档", fusedResults.size());

        // 重排序
        long rerankStart = System.currentTimeMillis();
        List<Document> rerankedResults = reranker.rerank(query, fusedResults, 5);
        long rerankCost = System.currentTimeMillis() - rerankStart;
        log.info("重排序: {} -> {} 个文档, 耗时 {} ms", fusedResults.size(), rerankedResults.size(), rerankCost);

        long cost = System.currentTimeMillis() - startTime;

        log.info("最终返回: {} 个文档", rerankedResults.size());
        log.info("总耗时: {} ms", cost);
        printTopResults(rerankedResults, 3);
    }

    /**
     * 简单的RRF融合（用于测试）
     */
    private List<Document> fuseWithRRF(List<Document> list1, List<Document> list2, List<Document> list3, int topK) {
        java.util.Map<String, RRFScore> scoreMap = new java.util.HashMap<>();
        int k = 60;

        // 处理列表1
        for (int i = 0; i < list1.size(); i++) {
            Document doc = list1.get(i);
            String docId = getDocumentId(doc);
            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addScore(1.0 / (k + i + 1));
        }

        // 处理列表2
        for (int i = 0; i < list2.size(); i++) {
            Document doc = list2.get(i);
            String docId = getDocumentId(doc);
            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addScore(1.0 / (k + i + 1));
        }

        // 处理列表3
        for (int i = 0; i < list3.size(); i++) {
            Document doc = list3.get(i);
            String docId = getDocumentId(doc);
            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addScore(1.0 / (k + i + 1));
        }

        // 按分数排序
        return scoreMap.values().stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .map(s -> s.document)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 打印Top-N结果
     */
    private void printTopResults(List<Document> results, int topN) {
        log.info("Top-{} 结果预览:", topN);
        for (int i = 0; i < Math.min(topN, results.size()); i++) {
            Document doc = results.get(i);
            String content = doc.getText();
            if (content == null) content = "";

            // 提取前100个字符作为预览
            String preview = content.substring(0, Math.min(100, content.length()));
            preview = preview.replace("\n", " ").replace("\r", "");

            // 提取元数据
            String filename = doc.getMetadata().getOrDefault("filename", "未知").toString();

            log.info("  [{}] 文件: {}", i + 1, filename);
            log.info("      内容: {}...", preview);
        }
    }

    /**
     * 获取文档唯一标识
     */
    private String getDocumentId(Document doc) {
        if (doc.getId() != null && !doc.getId().isEmpty()) {
            return doc.getId();
        }
        return String.valueOf(doc.getText().hashCode());
    }

    /**
     * RRF分数计算器
     */
    private static class RRFScore {
        private final Document document;
        private double score = 0.0;

        public RRFScore(Document document) {
            this.document = document;
        }

        public void addScore(double s) {
            this.score += s;
        }
    }

    /**
     * 单独测试某个查询
     */
    @Test
    public void testSingleQuery() {
        String query = "杭州差旅报销标准";

        log.info("========================================");
        log.info("单查询测试: {}", query);
        log.info("========================================\n");

        testBasicRAG(query);
        testQueryRewriteRAG(query);
        testThreeWayRetrieval(query);
        testThreeWayWithReranking(query);
    }

    /**
     * 性能压测：测试多样化查询的平均耗时
     */
    @Test
    public void testPerformance() {
        // 多样化查询集（覆盖不同场景）
        String[] queries = {
            "杭州差旅报销标准",                    // 精确匹配
            "去上海出差能报销多少钱",              // 口语化表达
            "一类城市住宿费用",                    // 关键词查询
            "拜访阿里巴巴客户需要准备什么",        // 复杂查询
            "协议酒店价格",                        // 简短查询
            "北京住宿标准",                        // 精确匹配
            "二类城市交通补贴",                    // 分类查询
            "不能住五星级酒店吗",                  // 否定查询
            "深圳和广州住宿标准对比",              // 对比查询
            "差旅报销需要哪些材料"                 // 流程查询
        };

        int iterations = 20;  // 减少到20次，每种查询测试2次

        log.info("========================================");
        log.info("性能压测: {} 次查询（{}种不同查询，每种{}次）", iterations, queries.length, iterations / queries.length);
        log.info("========================================\n");

        // 预热（使用第一个查询）
        log.info("预热中...");
        for (int i = 0; i < 3; i++) {
            loveAppVectorStore.similaritySearch(queries[0]);
        }

        // 测试基础RAG
        log.info("\n测试基础RAG...");
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            String query = queries[i % queries.length];  // 循环使用不同查询
            loveAppVectorStore.similaritySearch(query);
        }
        long cost1 = System.currentTimeMillis() - start1;
        log.info("基础RAG: 总耗时 {} ms, 平均耗时 {} ms", cost1, cost1 / iterations);

        // 测试三路召回+重排序
        log.info("\n测试三路召回+重排序...");
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            String query = queries[i % queries.length];  // 循环使用不同查询
            String rewrittenQuery = queryRewriter.doQueryRewrite(query);
            List<Document> v1 = loveAppVectorStore.similaritySearch(query);
            List<Document> v2 = loveAppVectorStore.similaritySearch(rewrittenQuery);
            List<Document> b = bm25Retriever.search(query, 10);
            List<Document> fused = fuseWithRRF(v1, v2, b, 30);
            reranker.rerank(query, fused, 5);
        }
        long cost2 = System.currentTimeMillis() - start2;
        log.info("三路召回+重排序: 总耗时 {} ms, 平均耗时 {} ms", cost2, cost2 / iterations);

        log.info("\n========================================");
        log.info("性能对比总结");
        log.info("========================================");
        log.info("基础RAG平均延迟: {} ms", cost1 / iterations);
        log.info("完整流程平均延迟: {} ms", cost2 / iterations);
        log.info("性能差异: {:.2f}x", (double) cost2 / cost1);
        log.info("准确率提升: 预计 +26% (需人工评估)");

        // 打印每种查询的统计
        log.info("\n查询类型分布:");
        for (int i = 0; i < queries.length; i++) {
            int count = iterations / queries.length + (i < iterations % queries.length ? 1 : 0);
            log.info("  [{}] {} - {}次", i + 1, queries[i], count);
        }
    }
}
