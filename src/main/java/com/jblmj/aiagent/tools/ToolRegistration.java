package com.jblmj.aiagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

    @Value("Fm5HbzZy4CNkhsZGhYyL4Eir")
    private String searchApiKey;

    @Value("${qweather.api-key}")
    private String qweatherApiKey;

    @Value("${qweather.api-host}")
    private String qweatherApiHost;

    @Value("${qweather.cli-path:tools/weather-cli.js}")
    private String qweatherCliPath;

    @Bean
    public WeatherQueryTool weatherQueryTool() {
        return new WeatherQueryTool(qweatherApiKey, qweatherApiHost, qweatherCliPath);
    }

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool,
                weatherQueryTool()
        );
    }
}
