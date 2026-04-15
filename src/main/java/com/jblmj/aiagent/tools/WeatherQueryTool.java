package com.jblmj.aiagent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 天气查询工具
 * 通过 CLI 调用和风天气 API
 */
@Slf4j
public class WeatherQueryTool {

    private final String apiKey;
    private final String apiHost;
    private final String cliPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherQueryTool(String apiKey, String apiHost, String cliPath) {
        this.apiKey = apiKey;
        this.apiHost = apiHost;
        this.cliPath = cliPath;
    }

    @Tool(description = "查询指定城市的实时天气信息，包括温度、天气状况、风力、湿度等。适用于出差前了解目的地天气情况。")
    public String queryWeather(@ToolParam(description = "城市名称，例如：北京、上海、杭州") String city) {
        try {
            log.info("开始查询天气: {}", city);

            // 构建命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "node",
                    cliPath,
                    city
            );

            // 设置环境变量
            Map<String, String> env = processBuilder.environment();
            env.put("QWEATHER_API_KEY", apiKey);
            env.put("QWEATHER_API_HOST", apiHost);

            // 执行命令
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // 读取错误输出
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line);
                }
            }

            // 等待进程结束
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                log.error("天气查询超时: {}", city);
                return "查询超时，请稍后重试";
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("天气查询失败: {}, 错误: {}", city, errorOutput);
                return "查询失败: " + errorOutput;
            }

            // 解析 JSON 响应
            String jsonResponse = output.toString();
            log.info("天气查询响应: {}", jsonResponse);

            Map<String, Object> result = objectMapper.readValue(jsonResponse, Map.class);
            Boolean success = (Boolean) result.get("success");

            if (Boolean.TRUE.equals(success)) {
                String summary = (String) result.get("summary");
                return summary;
            } else {
                String error = (String) result.get("error");
                return "查询失败: " + error;
            }

        } catch (Exception e) {
            log.error("天气查询异常: {}", city, e);
            return "查询异常: " + e.getMessage();
        }
    }
}
