package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索器 - 双路召回策略
 *
 * 策略：
 * 1. 原始查询检索（保留用户原始意图）
 * 2. 改写查询检索（标准化表达）
 * 3. 倒数排名融合（RRF）合并结果
 */
@Component
@Slf4j
public class HybridRetriever {

    private final VectorStore vectorStore;
    private final EnterpriseQueryRewriter queryRewriter;

    // RRF参数：排名平滑因子
    private static final int RRF_K = 60;

    public HybridRetriever(VectorStore loveAppVectorStore,
                           EnterpriseQueryRewriter queryRewriter) {
        this.vectorStore = loveAppVectorStore;
        this.queryRewriter = queryRewriter;
    }

    /**
     * 双路召回 + RRF融合
     *
     * @param originalQuery 原始查询
     * @param topK 返回Top-K结果
     * @return 融合后的文档列表
     */
    public List<Document> retrieve(String originalQuery, int topK) {
        log.debug("开始混合检索，原始查询: {}, topK: {}", originalQuery, topK);

        // Step 1: 查询重写
        String rewrittenQuery = queryRewriter.rewrite(originalQuery);

        // Step 2: 双路召回（召回2倍数量，后续融合）
        int retrieveSize = topK * 2;

        // 路径1：原始查询检索（使用简单的字符串查询）
        List<Document> originalResults = vectorStore.similaritySearch(originalQuery);
        log.debug("原始查询召回 {} 个文档", originalResults.size());

        // 路径2：改写查询检索
        List<Document> rewrittenResults = vectorStore.similaritySearch(rewrittenQuery);
        log.debug("改写查询召回 {} 个文档", rewrittenResults.size());

        // Step 3: RRF融合
        List<Document> fusedResults = fuseWithRRF(originalResults, rewrittenResults, topK);

        log.info("混合检索完成，最终返回 {} 个文档", fusedResults.size());
        return fusedResults;
    }

    /**
     * 倒数排名融合（Reciprocal Rank Fusion）
     *
     * 公式：score(doc) = Σ 1 / (k + rank_i)
     * 其中 rank_i 是文档在第i个结果列表中的排名
     */
    private List<Document> fuseWithRRF(List<Document> list1,
                                       List<Document> list2,
                                       int topK) {
        // 计算每个文档的RRF分数
        Map<String, RRFScore> scoreMap = new HashMap<>();

        // 处理列表1
        for (int i = 0; i < list1.size(); i++) {
            Document doc = list1.get(i);
            String docId = getDocumentId(doc);

            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addRank(i + 1, "original");
        }

        // 处理列表2
        for (int i = 0; i < list2.size(); i++) {
            Document doc = list2.get(i);
            String docId = getDocumentId(doc);

            scoreMap.putIfAbsent(docId, new RRFScore(doc));
            scoreMap.get(docId).addRank(i + 1, "rewritten");
        }

        // 按RRF分数排序
        List<Document> fusedResults = scoreMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .map(RRFScore::getDocument)
                .collect(Collectors.toList());

        // 打印融合详情（调试用）
        if (log.isDebugEnabled()) {
            log.debug("RRF融合详情:");
            scoreMap.values().stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(5)
                    .forEach(score -> {
                        String content = score.getDocument().getText();
                        String preview = content.substring(0, Math.min(50, content.length()));
                        log.debug("  文档: {}, RRF分数: {:.4f}, 来源: {}",
                                preview, score.getScore(), score.getSources());
                    });
        }

        return fusedResults;
    }

    /**
     * 获取文档唯一标识
     */
    private String getDocumentId(Document doc) {
        // 优先使用文档ID
        if (doc.getId() != null && !doc.getId().isEmpty()) {
            return doc.getId();
        }

        // 否则使用内容哈希
        return String.valueOf(doc.getText().hashCode());
    }

    /**
     * RRF分数计算器
     */
    private static class RRFScore {
        private final Document document;
        private double score = 0.0;
        private final List<String> sources = new ArrayList<>();

        public RRFScore(Document document) {
            this.document = document;
        }

        public void addRank(int rank, String source) {
            // RRF公式：1 / (k + rank)
            score += 1.0 / (RRF_K + rank);
            sources.add(source + ":" + rank);
        }

        public Document getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }

        public String getSources() {
            return String.join(", ", sources);
        }
    }
}
