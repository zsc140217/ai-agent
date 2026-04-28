package com.jblmj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BM25检索器
 *
 * 算法：BM25（Best Matching 25）
 * 公式：score(D,Q) = Σ IDF(qi) · (f(qi,D) · (k1+1)) / (f(qi,D) + k1·(1-b+b·|D|/avgdl))
 *
 * 特点：
 * - 基于词频的相关性打分
 * - 精确匹配能力强
 * - 速度快（倒排索引）
 *
 * 企业级优化：
 * - 中文分词（简单实现：按字分词）
 * - 停用词过滤
 * - 倒排索引加速
 */
@Component
@Slf4j
public class BM25Retriever {

    // BM25参数
    private static final double K1 = 1.5;  // 词频饱和度参数
    private static final double B = 0.75;  // 文档长度归一化参数

    // 停用词表
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这"
    );

    // 文档库
    private List<Document> documents;

    // 倒排索引：词 -> List<(文档ID, 词频)>
    private Map<String, List<TermFreq>> invertedIndex;

    // 文档长度
    private Map<String, Integer> docLengths;

    // 平均文档长度
    private double avgDocLength;

    // IDF缓存
    private Map<String, Double> idfCache;

    // 索引存储路径
    private static final String INDEX_DIR = "data/bm25_index";
    private static final String INDEX_FILE = "bm25_index.ser";

    /**
     * 初始化索引（支持持久化）
     *
     * @param documents 文档列表
     * @param forceRebuild 是否强制重建索引（true=重建，false=尝试加载已有索引）
     */
    public void buildIndex(List<Document> documents, boolean forceRebuild) {
        // 尝试加载已有索引
        if (!forceRebuild && loadIndex(documents)) {
            log.info("成功加载已有BM25索引");
            return;
        }

        // 构建新索引
        buildIndexInternal(documents);

        // 保存索引
        saveIndex();
    }

    /**
     * 初始化索引（默认尝试加载已有索引）
     */
    public void buildIndex(List<Document> documents) {
        buildIndex(documents, false);
    }

    /**
     * 保存索引到磁盘
     */
    private void saveIndex() {
        try {
            // 创建目录
            Path indexPath = Paths.get(INDEX_DIR);
            if (!Files.exists(indexPath)) {
                Files.createDirectories(indexPath);
            }

            // 序列化索引数据
            IndexData indexData = new IndexData(
                    documents,
                    invertedIndex,
                    docLengths,
                    avgDocLength,
                    idfCache
            );

            Path filePath = indexPath.resolve(INDEX_FILE);
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(filePath.toFile()))) {
                oos.writeObject(indexData);
            }

            log.info("BM25索引已保存到: {}", filePath.toAbsolutePath());
        } catch (Exception e) {
            log.error("保存BM25索引失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从磁盘加载索引
     *
     * @param documents 当前文档列表（用于验证索引是否匹配）
     * @return 是否成功加载
     */
    private boolean loadIndex(List<Document> documents) {
        try {
            Path filePath = Paths.get(INDEX_DIR, INDEX_FILE);
            if (!Files.exists(filePath)) {
                log.info("BM25索引文件不存在，需要构建新索引");
                return false;
            }

            // 反序列化索引数据
            IndexData indexData;
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(filePath.toFile()))) {
                indexData = (IndexData) ois.readObject();
            }

            // 验证文档数量是否匹配
            if (indexData.documents.size() != documents.size()) {
                log.warn("索引文档数量不匹配（索引: {}, 当前: {}），需要重建",
                        indexData.documents.size(), documents.size());
                return false;
            }

            // 恢复索引数据
            this.documents = indexData.documents;
            this.invertedIndex = indexData.invertedIndex;
            this.docLengths = indexData.docLengths;
            this.avgDocLength = indexData.avgDocLength;
            this.idfCache = indexData.idfCache;

            log.info("成功加载BM25索引，文档数: {}, 词表大小: {}",
                    documents.size(), invertedIndex.size());
            return true;

        } catch (Exception e) {
            log.error("加载BM25索引失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 内部构建索引方法
     */
    private void buildIndexInternal(List<Document> documents) {
        log.info("开始构建BM25索引，文档数量: {}", documents.size());
        long startTime = System.currentTimeMillis();

        this.documents = documents;
        this.invertedIndex = new HashMap<>();
        this.docLengths = new HashMap<>();
        this.idfCache = new HashMap<>();

        // 构建倒排索引
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String docId = getDocumentId(doc, i);

            // 分词
            List<String> tokens = tokenize(doc.getText());

            // 记录文档长度
            docLengths.put(docId, tokens.size());

            // 统计词频
            Map<String, Integer> termFreqs = new HashMap<>();
            for (String token : tokens) {
                termFreqs.merge(token, 1, Integer::sum);
            }

            // 构建倒排索引
            for (Map.Entry<String, Integer> entry : termFreqs.entrySet()) {
                String term = entry.getKey();
                int freq = entry.getValue();

                invertedIndex.computeIfAbsent(term, k -> new ArrayList<>())
                        .add(new TermFreq(docId, freq));
            }
        }

        // 计算平均文档长度
        avgDocLength = docLengths.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        // 预计算IDF
        for (String term : invertedIndex.keySet()) {
            idfCache.put(term, calculateIDF(term));
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.info("BM25索引构建完成，耗时: {}ms, 词表大小: {}, 平均文档长度: {:.2f}",
                costTime, invertedIndex.size(), avgDocLength);
    }

    /**
     * BM25检索
     *
     * @param query 查询
     * @param topK 返回Top-K结果
     * @return 文档列表（按BM25分数降序）
     */
    public List<Document> search(String query, int topK) {
        if (documents == null || documents.isEmpty()) {
            log.warn("BM25索引未初始化或文档为空");
            return Collections.emptyList();
        }

        // 分词
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            log.warn("查询分词后为空: {}", query);
            return Collections.emptyList();
        }

        log.debug("BM25检索，查询: {}, 分词: {}", query, queryTokens);

        // 计算每个文档的BM25分数
        Map<String, Double> scores = new HashMap<>();

        for (String token : queryTokens) {
            if (!invertedIndex.containsKey(token)) {
                continue;  // 词不在索引中
            }

            double idf = idfCache.getOrDefault(token, 0.0);
            List<TermFreq> postings = invertedIndex.get(token);

            for (TermFreq posting : postings) {
                String docId = posting.docId;
                int termFreq = posting.freq;
                int docLength = docLengths.get(docId);

                // BM25公式
                double score = idf * (termFreq * (K1 + 1)) /
                        (termFreq + K1 * (1 - B + B * docLength / avgDocLength));

                scores.merge(docId, score, Double::sum);
            }
        }

        // 按分数排序，返回Top-K
        List<Document> results = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(entry -> {
                    String docId = entry.getKey();
                    double score = entry.getValue();
                    Document doc = findDocumentById(docId);
                    log.debug("  文档ID: {}, BM25分数: {:.4f}, 内容: {}",
                            docId, score, doc.getText().substring(0, Math.min(50, doc.getText().length())));
                    return doc;
                })
                .collect(Collectors.toList());

        log.info("BM25检索完成，召回 {} 个文档", results.size());
        return results;
    }

    /**
     * 中文分词（简单实现：按字分词 + 停用词过滤）
     *
     * 企业级实现应该用：
     * - HanLP
     * - jieba
     * - IK Analyzer
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();

        // 按字分词
        for (int i = 0; i < text.length(); i++) {
            String token = String.valueOf(text.charAt(i));

            // 过滤停用词、标点、空格
            if (STOP_WORDS.contains(token) ||
                    token.matches("[\\p{Punct}\\s]")) {
                continue;
            }

            tokens.add(token);
        }

        return tokens;
    }

    /**
     * 计算IDF（逆文档频率）
     *
     * IDF(t) = log((N - df(t) + 0.5) / (df(t) + 0.5))
     * 其中：
     * - N: 文档总数
     * - df(t): 包含词t的文档数
     */
    private double calculateIDF(String term) {
        int N = documents.size();
        int df = invertedIndex.get(term).size();  // 文档频率

        return Math.log((N - df + 0.5) / (df + 0.5) + 1.0);
    }

    /**
     * 获取文档ID
     */
    private String getDocumentId(Document doc, int index) {
        if (doc.getId() != null && !doc.getId().isEmpty()) {
            return doc.getId();
        }
        return "doc_" + index;
    }

    /**
     * 根据ID查找文档
     */
    private Document findDocumentById(String docId) {
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String id = getDocumentId(doc, i);
            if (id.equals(docId)) {
                return doc;
            }
        }
        return null;
    }

    /**
     * 词频记录
     */
    private static class TermFreq implements Serializable {
        private static final long serialVersionUID = 1L;
        String docId;
        int freq;

        TermFreq(String docId, int freq) {
            this.docId = docId;
            this.freq = freq;
        }
    }

    /**
     * 索引数据（用于序列化）
     */
    private static class IndexData implements Serializable {
        private static final long serialVersionUID = 1L;

        List<Document> documents;
        Map<String, List<TermFreq>> invertedIndex;
        Map<String, Integer> docLengths;
        double avgDocLength;
        Map<String, Double> idfCache;

        IndexData(List<Document> documents,
                  Map<String, List<TermFreq>> invertedIndex,
                  Map<String, Integer> docLengths,
                  double avgDocLength,
                  Map<String, Double> idfCache) {
            this.documents = documents;
            this.invertedIndex = invertedIndex;
            this.docLengths = docLengths;
            this.avgDocLength = avgDocLength;
            this.idfCache = idfCache;
        }
    }
}
