package com.jblmj.aiagent.agent;

import com.jblmj.aiagent.agent.model.AgentState;
import com.jblmj.aiagent.model.ObservationResult;
import com.jblmj.aiagent.model.ReActStep;
import com.jblmj.aiagent.model.ReflectionResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强版 ReAct Agent
 * 实现完整的 Thought → Action → Observation → Reflection 循环
 * 支持智能观察、自动重试、策略调整、经验积累
 */
@Slf4j
@Getter
public abstract class EnhancedReActAgent extends BaseAgent {

    /**
     * 当前观察结果
     */
    private String currentObservation;

    /**
     * 上一步的执行结果（用于 observe）
     */
    private String lastActionResult;

    /**
     * 执行轨迹
     */
    private final List<ReActStep> executionTrace = new ArrayList<>();

    /**
     * 重试计数器（工具名 -> 重试次数）
     */
    private final Map<String, Integer> retryCounters = new HashMap<>();

    /**
     * 经验库（工具名 -> 成功/失败经验）
     */
    private final Map<String, List<String>> experienceLibrary = new HashMap<>();

    /**
     * 任务目标
     */
    private String taskGoal;

    /**
     * 已完成的子目标
     */
    private final List<String> completedSubGoals = new ArrayList<>();

    /**
     * 完整的 ReAct 步骤（增强版）
     */
    @Override
    public String step() {
        ReActStep step = new ReActStep();
        step.setStepNumber(getCurrentStep());
        step.setTimestamp(System.currentTimeMillis());
        long startTime = System.currentTimeMillis();

        try {
            // 1. Thought：分析当前状态，决定下一步
            log.info("💭 Thinking...");
            String thought = think();
            step.setThought(thought);

            // 2. Action：执行决定的行动
            log.info("🔧 Acting...");
            String action = act();
            step.setAction(action);

            // 3. Observation：智能观察执行结果
            log.info("👁️ Observing...");
            ObservationResult observationResult = observeWithIntelligence();
            step.setObservation(observationResult.format());
            step.setObservationResult(observationResult);
            this.currentObservation = observationResult.format();

            // 4. Reflection：智能反思并决定策略
            log.info("🤔 Reflecting...");
            ReflectionResult reflectionResult = reflectWithStrategy(observationResult);
            step.setReflection(reflectionResult.format());
            step.setReflectionResult(reflectionResult);

            // 5. 处理反思结果
            handleReflectionResult(reflectionResult);

            // 记录耗时
            step.setDuration(System.currentTimeMillis() - startTime);

            // 记录执行轨迹
            executionTrace.add(step);

            // 格式化输出
            return formatStepResult(step);

        } catch (Exception e) {
            log.error("Step execution failed", e);
            step.setError(e.getMessage());
            step.setDuration(System.currentTimeMillis() - startTime);
            executionTrace.add(step);
            return handleError(e, step);
        }
    }

    /**
     * 思考：分析当前状态，决定下一步行动
     */
    protected abstract String think();

    /**
     * 行动：执行决定的行动
     */
    protected abstract String act();

    /**
     * 智能观察：深度分析执行结果
     */
    protected ObservationResult observeWithIntelligence() {
        ObservationResult result = new ObservationResult();

        if (lastActionResult == null) {
            result.setSummary("无执行结果");
            return result;
        }

        // 1. 生成观察摘要
        result.setSummary(extractKeyInfo(lastActionResult));

        // 2. 提取关键信息
        result.setKeyInfo(extractStructuredInfo(lastActionResult));

        // 3. 异常检测
        result.setAnomalies(detectAnomalies(lastActionResult));

        // 4. 多步推理（分析因果关系）
        result.setReasoning(analyzeReasoning(lastActionResult));

        // 5. 下一步建议
        result.setNextStepSuggestion(suggestNextStep(lastActionResult));

        return result;
    }

    /**
     * 智能反思：根据观察结果制定策略
     */
    protected ReflectionResult reflectWithStrategy(ObservationResult observation) {
        ReflectionResult result = new ReflectionResult();

        // 1. 判断是否成功
        boolean success = !observation.getAnomalies().contains("错误")
                       && !observation.getAnomalies().contains("失败");
        result.setSuccess(success);

        if (!success) {
            // 2. 失败分析
            result.setFailureReason(analyzeFailure(observation));

            // 3. 策略调整
            result.setStrategyAdjustment(adjustStrategy(observation));

            // 4. 重试决策
            String toolName = extractToolName(lastActionResult);
            int retryCount = retryCounters.getOrDefault(toolName, 0);
            result.setRetryCount(retryCount);
            result.setShouldRetry(retryCount < 3); // 最多重试3次

            // 5. 备用工具推荐
            result.setAlternativeTool(recommendAlternativeTool(toolName));

            // 更新重试计数
            if (result.isShouldRetry()) {
                retryCounters.put(toolName, retryCount + 1);
            }
        } else {
            // 成功时重置重试计数
            String toolName = extractToolName(lastActionResult);
            retryCounters.put(toolName, 0);
        }

        // 6. 进度检查
        result.setProgressCheck(checkProgress(observation));

        // 7. 经验积累
        result.setExperienceLearned(learnFromExperience(observation, success));

        return result;
    }

    /**
     * 处理反思结果
     */
    protected void handleReflectionResult(ReflectionResult reflection) {
        if (!reflection.isSuccess() && reflection.isShouldRetry()) {
            log.warn("⚠️ 执行失败，准备重试（第{}次）", reflection.getRetryCount() + 1);
        }

        if (reflection.getProgressCheck().contains("目标已达成")) {
            setState(AgentState.FINISHED);
            log.info("✅ 任务完成");
        }
    }

    /**
     * 观察：观察执行结果，提取关键信息
     * 默认实现：返回上一步的执行结果
     * @deprecated 使用 observeWithIntelligence() 替代
     */
    @Deprecated
    protected String observe() {
        if (lastActionResult == null) {
            return "无执行结果";
        }

        // 提取关键信息
        String observation = extractKeyInfo(lastActionResult);
        return "观察到：" + observation;
    }

    /**
     * 反思：根据观察结果，判断是否需要调整策略
     * 默认实现：简单判断是否成功
     * @deprecated 使用 reflectWithStrategy() 替代
     */
    @Deprecated
    protected String reflect() {
        if (currentObservation == null) {
            return "无观察结果，继续执行";
        }

        // 判断是否包含错误信息
        if (currentObservation.contains("错误") || currentObservation.contains("失败")) {
            return "执行失败，需要调整策略";
        }

        // 判断是否达到目标
        if (currentObservation.contains("完成") || currentObservation.contains("成功")) {
            setState(AgentState.FINISHED);
            return "目标已达成，任务完成";
        }

        return "继续执行下一步";
    }

    /**
     * 提取关键信息
     */
    protected String extractKeyInfo(String result) {
        if (result == null) {
            return "无结果";
        }

        // 限制长度，避免信息过载
        if (result.length() > 200) {
            return result.substring(0, 200) + "...";
        }

        return result;
    }

    /**
     * 提取结构化信息
     */
    protected String extractStructuredInfo(String result) {
        if (result == null || result.isEmpty()) {
            return "";
        }

        // 提取关键字段（简化版）
        StringBuilder info = new StringBuilder();

        if (result.contains("成功")) {
            info.append("状态:成功");
        } else if (result.contains("失败") || result.contains("错误")) {
            info.append("状态:失败");
        }

        return info.toString();
    }

    /**
     * 检测异常
     */
    protected String detectAnomalies(String result) {
        if (result == null) {
            return "";
        }

        if (result.contains("错误") || result.contains("error")) {
            return "错误";
        }
        if (result.contains("失败") || result.contains("failed")) {
            return "失败";
        }
        if (result.contains("超时") || result.contains("timeout")) {
            return "超时";
        }

        return "";
    }

    /**
     * 分析推理
     */
    protected String analyzeReasoning(String result) {
        if (result == null || result.isEmpty()) {
            return "";
        }

        // 简化版：基于结果推理下一步
        if (result.contains("天气")) {
            return "已获取天气信息，可以基于天气规划行程";
        }
        if (result.contains("客户")) {
            return "已获取客户信息，可以安排拜访时间";
        }

        return "";
    }

    /**
     * 建议下一步
     */
    protected String suggestNextStep(String result) {
        if (result == null || result.isEmpty()) {
            return "继续执行";
        }

        if (result.contains("天气")) {
            return "建议根据天气情况推荐酒店和交通方式";
        }
        if (result.contains("完成")) {
            return "建议终止任务";
        }

        return "继续执行下一步";
    }

    /**
     * 分析失败原因
     */
    protected String analyzeFailure(ObservationResult observation) {
        String anomalies = observation.getAnomalies();

        if (anomalies.contains("超时")) {
            return "工具调用超时，可能是网络问题或服务响应慢";
        }
        if (anomalies.contains("错误")) {
            return "工具执行出错，可能是参数不正确或服务异常";
        }
        if (anomalies.contains("失败")) {
            return "工具执行失败，需要检查输入参数";
        }

        return "未知失败原因";
    }

    /**
     * 调整策略
     */
    protected String adjustStrategy(ObservationResult observation) {
        String anomalies = observation.getAnomalies();

        if (anomalies.contains("超时")) {
            return "建议：增加超时时间或使用备用工具";
        }
        if (anomalies.contains("错误")) {
            return "建议：检查参数格式，或尝试其他工具";
        }

        return "建议：重新分析任务需求";
    }

    /**
     * 提取工具名称
     */
    protected String extractToolName(String result) {
        if (result == null || result.isEmpty()) {
            return "unknown";
        }

        // 简化版：从结果中提取工具名
        if (result.contains("queryWeather")) {
            return "queryWeather";
        }
        if (result.contains("queryCustomer")) {
            return "queryCustomer";
        }
        if (result.contains("recommendHotel")) {
            return "recommendHotel";
        }

        return "unknown";
    }

    /**
     * 推荐备用工具
     */
    protected String recommendAlternativeTool(String toolName) {
        // 简化版：基于工具名推荐备用工具
        switch (toolName) {
            case "queryWeather":
                return "可尝试使用历史天气数据";
            case "queryCustomer":
                return "可尝试使用客户数据库查询";
            default:
                return "";
        }
    }

    /**
     * 检查进度
     */
    protected String checkProgress(ObservationResult observation) {
        // 简化版：基于观察结果判断进度
        if (observation.getSummary() != null && observation.getSummary().contains("完成")) {
            return "目标已达成";
        }

        return "任务进行中";
    }

    /**
     * 从经验中学习
     */
    protected String learnFromExperience(ObservationResult observation, boolean success) {
        String toolName = extractToolName(lastActionResult);

        if (success) {
            String experience = toolName + " 在当前场景下执行成功";
            experienceLibrary.computeIfAbsent(toolName, k -> new ArrayList<>()).add(experience);
            return experience;
        } else {
            String experience = toolName + " 在当前场景下执行失败: " + observation.getAnomalies();
            experienceLibrary.computeIfAbsent(toolName, k -> new ArrayList<>()).add(experience);
            return experience;
        }
    }

    /**
     * 设置任务目标
     */
    public void setTaskGoal(String goal) {
        this.taskGoal = goal;
        this.completedSubGoals.clear();
    }

    /**
     * 添加已完成的子目标
     */
    public void addCompletedSubGoal(String subGoal) {
        this.completedSubGoals.add(subGoal);
    }

    /**
     * 计算任务进度
     */
    public double calculateProgress() {
        if (taskGoal == null || taskGoal.isEmpty()) {
            return 0.0;
        }

        // 简化版：基于步骤数估算进度
        int totalSteps = getMaxSteps();
        int currentStep = getCurrentStep();

        return Math.min(1.0, (double) currentStep / totalSteps);
    }

    /**
     * 获取经验库
     */
    public Map<String, List<String>> getExperienceLibrary() {
        return experienceLibrary;
    }

    /**
     * 格式化步骤结果
     */
    protected String formatStepResult(ReActStep step) {
        return step.format();
    }

    /**
     * 错误处理
     */
    protected String handleError(Exception e, ReActStep step) {
        setState(AgentState.ERROR);
        return step.format();
    }

    /**
     * 保存执行结果（供 observe 使用）
     */
    protected void saveActionResult(String result) {
        this.lastActionResult = result;
    }

    /**
     * 获取完整的执行轨迹
     */
    public String getExecutionTraceFormatted() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== 执行轨迹 ==========\n");
        for (ReActStep step : executionTrace) {
            sb.append(step.format());
        }
        sb.append("========== 轨迹结束 ==========\n");
        return sb.toString();
    }

    /**
     * 清理资源
     */
    @Override
    protected void cleanup() {
        // 输出完整轨迹
        if (!executionTrace.isEmpty()) {
            log.info(getExecutionTraceFormatted());
        }

        // 清理执行轨迹（但不清空，保留给外部访问）
        // executionTrace.clear(); // 注释掉，让测试可以访问

        // 重置观察结果
        this.currentObservation = null;
        this.lastActionResult = null;

        // 调用父类清理
        super.cleanup();
    }

    /**
     * 重置 Agent 状态（用于测试或重复使用）
     */
    public void reset() {
        executionTrace.clear();
        this.currentObservation = null;
        this.lastActionResult = null;
        this.retryCounters.clear();
        this.experienceLibrary.clear();
        this.taskGoal = null;
        this.completedSubGoals.clear();
        setState(AgentState.IDLE);
        setCurrentStep(0);
        getMessageList().clear();
    }
}
