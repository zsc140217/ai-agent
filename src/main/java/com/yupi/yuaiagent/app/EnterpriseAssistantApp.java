package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 企业出差/外勤智能助手
 * 整合了 RAG 知识库、MCP 地图服务和多轮对话能力
 */
@Component
@Slf4j
public class EnterpriseAssistantApp {

    private final ChatClient chatClient;

    // 1. 修改为人设：专业、高效、合规的出差管家
    private static final String SYSTEM_PROMPT = """
            你是一个专业且高效的【企业出差管家】。你的目标是协助员工规划外勤行程、查询报销政策及客户信息。
            你的职责包括：
            1. 根据公司制度（RAG）告知用户差旅补贴标准、协议酒店价格。
            2. 根据客户信息（RAG）提供客户地址及联系人。
            3. 结合地图工具（MCP）规划通勤路线，推荐最靠近客户公司的协议酒店。
            
            开场请礼貌地询问用户出差的目的地或需要拜访的客户。
            回复必须基于事实（知识库），严禁虚构报销金额或酒店价格。
            """;

    /**
     * 初始化 ChatClient
     */
    public EnterpriseAssistantApp(ChatModel dashscopeChatModel) {
        // 初始化基于内存的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * SSE 流式传输
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    // 2. 将 LoveReport 改造为 TripPlan (行程规划结构化对象)
    public record TripPlan(String destination, String hotelRecommendation, String totalBudget, List<String> reminders) {
    }

    /**
     * 生成结构化的出差行程简报
     */
    public TripPlan doChatWithTripPlan(String message, String chatId) {
        TripPlan plan = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "请根据对话内容生成一份出差简报，包含目的地、酒店推荐、预算预估和注意事项。")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TripPlan.class);
        log.info("TripPlan generated: {}", plan);
        return plan;
    }

    // --- RAG 与工具注入 ---

    @Resource
    private VectorStore loveAppVectorStore; // 建议在 VectorStoreConfig 里将 Bean 名改为 corporateVectorStore

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 结合内部知识库（差旅制度、客户清单）进行对话
     */
    public String doChatWithCorporateKnowledge(String message, String chatId) {
        // 查询重写：将“杭州差旅报销”改写为更适合检索的“企业在杭州的一类城市出差补贴标准”
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore)) // 注入本地知识
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallbackProvider toolCallbackProvider; // MCP 高德地图工具提供者

    /**
     * 终极功能：结合 RAG 知识库 + MCP 实时地图
     * 面试重点：演示“先查公司制度，再查地图距离”的多能力协同
     */
    public String doComprehensiveChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                // 1. 注入内部知识库 Advisor
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 2. 注入外部工具（计算器等）
                .toolCallbacks(allTools)
                // 3. 注入 MCP 地图工具（关键！）
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();

        return chatResponse.getResult().getOutput().getText();
    }
}