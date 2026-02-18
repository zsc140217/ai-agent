package com.yupi.yuaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(@ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");

        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            if (StrUtil.isBlank(response)) {
                return "搜索结果为空";
            }

            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");

            // --- 核心修复开始 ---
            if (organicResults == null || organicResults.isEmpty()) {
                return "未找到相关搜索结果";
            }

            // 取实际长度和 5 的较小值，防止越界
            int limit = Math.min(organicResults.size(), 5);
            List<Object> objects = organicResults.subList(0, limit);
            // --- 核心修复结束 ---

            // 拼接搜索结果
            return objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                // 建议只提取 title, link 和 snippet，减少交给 AI 的冗余数据，节省 Token
                Map<String, Object> cleanResult = new HashMap<>();
                cleanResult.put("title", tmpJSONObject.getStr("title"));
                cleanResult.put("link", tmpJSONObject.getStr("link"));
                cleanResult.put("snippet", tmpJSONObject.getStr("snippet"));
                return JSONUtil.toJsonStr(cleanResult);
            }).collect(Collectors.joining(","));

        } catch (Exception e) {
            // 打印堆栈以便排查具体的网络或解析问题
            e.printStackTrace();
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
