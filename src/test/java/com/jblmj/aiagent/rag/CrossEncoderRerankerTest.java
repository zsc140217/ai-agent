package com.jblmj.aiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-Encoder重排序测试
 *
 * 测试目标：
 * 1. 验证交叉编码器重排序效果
 * 2. 对比重排序前后的准确率
 * 3. 性能基准测试
 */
@SpringBootTest
public class CrossEncoderRerankerTest {

    @Autowired
    private CrossEncoderReranker reranker;

    @Test
    public void testRerankerAccuracy() {
        System.out.println("\n========== 测试1：重排序准确率 ==========\n");

        // 测试用例
        String[] queries = {
                "北京住宿标准",
                "上海出差住宿能报多少",
                "深圳一类城市住宿费用",
                "杭州住宿标准是多少",
                "广州出差住宿报销标准"
        };

        for (String query : queries) {
            System.out.println("\n查询: " + query);
            System.out.println("----------------------------------------");

            // 模拟召回的文档（包含相关和不相关的）
            List<Document> documents = createMockDocuments(query);

            // 重排序
            List<Document> reranked = reranker.rerank(query, documents, 3);

            // 打印结果
            System.out.println("重排序Top-3:");
            for (int i = 0; i < reranked.size(); i++) {
                String text = reranked.get(i).getText();
                if (text == null) text = "";
                String preview = text.substring(0, Math.min(60, text.length()));
                System.out.println((i + 1) + ". " + preview + "...");
            }
        }
    }

    @Test
    public void testRerankerPerformance() {
        System.out.println("\n========== 测试2：重排序性能 ==========\n");

        String query = "北京住宿标准";
        List<Document> documents = createMockDocuments(query);

        // 预热
        reranker.rerank(query, documents, 5);

        // 性能测试
        int rounds = 5;
        long totalTime = 0;

        for (int i = 0; i < rounds; i++) {
            long start = System.currentTimeMillis();
            reranker.rerank(query, documents, 5);
            long cost = System.currentTimeMillis() - start;
            totalTime += cost;
            System.out.println("第" + (i + 1) + "轮耗时: " + cost + "ms");
        }

        System.out.println("\n平均耗时: " + (totalTime / rounds) + "ms");
        System.out.println("文档数: " + documents.size());
    }

    @Test
    public void testCrossEncoderVsBiEncoder() {
        System.out.println("\n========== 测试3：交叉编码器 vs 双塔模型 ==========\n");

        String query = "北京住宿标准是多少";

        // 相关文档
        Document doc1 = new Document("一类城市住宿标准：北京500元，上海600元");
        // 部分相关
        Document doc2 = new Document("北京是一类城市，住宿费用较高");
        // 不相关
        Document doc3 = new Document("申请流程需要提前审批");

        List<Document> documents = List.of(doc1, doc2, doc3);

        System.out.println("查询: " + query);
        System.out.println("\n候选文档:");
        System.out.println("Doc1: " + doc1.getText());
        System.out.println("Doc2: " + doc2.getText());
        System.out.println("Doc3: " + doc3.getText());

        // 交叉编码器重排序
        List<Document> reranked = reranker.rerank(query, documents, 3);

        System.out.println("\n交叉编码器排序结果:");
        for (int i = 0; i < reranked.size(); i++) {
            System.out.println((i + 1) + ". " + reranked.get(i).getText());
        }

        System.out.println("\n预期: Doc1 > Doc2 > Doc3");
        System.out.println("原因: 交叉编码器能捕捉到'多少'对应'500元'的精确匹配");
    }

    /**
     * 创建模拟文档
     */
    private List<Document> createMockDocuments(String query) {
        List<Document> docs = new ArrayList<>();

        // 相关文档
        docs.add(new Document("一类城市住宿标准：北京、上海、广州、深圳，住宿费用500元/天"));
        docs.add(new Document("二类城市住宿标准：杭州、南京、成都，住宿费用400元/天"));
        docs.add(new Document("三类城市住宿标准：其他城市，住宿费用300元/天"));

        // 部分相关
        docs.add(new Document("出差住宿费用报销需要提供发票和审批单"));
        docs.add(new Document("超标准住宿需要提前申请并说明原因"));
        docs.add(new Document("展会期间酒店价格上浮，可适当放宽标准"));

        // 不相关
        docs.add(new Document("差旅费预支申请流程：填写申请表→部门审批→财务审核"));
        docs.add(new Document("民宿和Airbnb不在报销范围内"));
        docs.add(new Document("出差期间的餐费标准为100元/天"));

        return docs;
    }
}
