package com.jblmj.aiagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.jblmj.aiagent.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 鱼皮的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 * 增强版：实现完整的 ReAct 循环（Thought → Action → Observation → Reflection）
 */
@Component
@Slf4j
public class JblmjManus extends ToolCallAgent {

    public JblmjManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("jblmjManus");
        String SYSTEM_PROMPT = """
                You are YuManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.

                You follow a complete ReAct cycle:
                1. Thought: Analyze the current situation and decide the next action
                2. Action: Execute the chosen tool or operation
                3. Observation: Extract key information from the execution results
                4. Reflection: Determine if strategy adjustment is needed based on observations

                Always be aware of your execution results and adjust your strategy accordingly.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    /**
     * 增强的观察方法：提取工具调用的详细信息
     */
    @Override
    protected String observe() {
        // 调用父类的观察方法
        String baseObservation = super.observe();

        // 提取更详细的工具执行信息
        if (getToolCallChatResponse() != null && getToolCallChatResponse().hasToolCalls()) {
            Message lastMessage = CollUtil.getLast(getMessageList());
            if (lastMessage instanceof ToolResponseMessage) {
                ToolResponseMessage toolResponse = (ToolResponseMessage) lastMessage;

                // 统计成功和失败的工具调用
                long successCount = toolResponse.getResponses().stream()
                        .filter(r -> !r.responseData().contains("错误")
                                && !r.responseData().contains("失败")
                                && !r.responseData().contains("error"))
                        .count();

                long failureCount = toolResponse.getResponses().size() - successCount;

                // 提取关键结果
                StringBuilder detailedObservation = new StringBuilder();
                detailedObservation.append(String.format("观察到：执行了 %d 个工具调用，",
                        toolResponse.getResponses().size()));
                detailedObservation.append(String.format("%d 个成功，%d 个失败。",
                        successCount, failureCount));

                // 如果有失败，记录失败原因
                if (failureCount > 0) {
                    String failureReasons = toolResponse.getResponses().stream()
                            .filter(r -> r.responseData().contains("错误")
                                    || r.responseData().contains("失败")
                                    || r.responseData().contains("error"))
                            .map(r -> r.name() + ": " + extractKeyInfo(r.responseData()))
                            .reduce((a, b) -> a + "; " + b)
                            .orElse("未知错误");
                    detailedObservation.append(" 失败原因：").append(failureReasons);
                }

                return detailedObservation.toString();
            }
        }

        return baseObservation;
    }

    /**
     * 增强的反思方法：根据观察结果智能调整策略
     */
    @Override
    protected String reflect() {
        String observation = getCurrentObservation();

        if (observation == null) {
            return "无观察结果，继续执行";
        }

        // 1. 检查是否有失败的工具调用
        if (observation.contains("失败") && !observation.contains("0 个失败")) {
            log.warn("检测到工具调用失败，需要调整策略");
            return "工具调用失败，需要分析失败原因并调整策略（可能需要更换工具或修改参数）";
        }

        // 2. 检查是否所有工具都成功执行
        if (observation.contains("成功") && observation.contains("0 个失败")) {
            log.info("所有工具调用成功");
            return "工具调用成功，继续执行下一步或准备返回结果";
        }

        // 3. 检查是否达到终止条件
        if (observation.contains("terminate") || observation.contains("完成")) {
            log.info("检测到终止信号");
            return "任务已完成，准备终止";
        }

        // 4. 默认：继续执行
        return super.reflect();
    }
}

