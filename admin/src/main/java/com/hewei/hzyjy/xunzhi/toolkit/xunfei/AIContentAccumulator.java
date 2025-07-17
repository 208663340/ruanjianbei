package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AIContentAccumulator {
    private final StringBuilder contentBuilder = new StringBuilder();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析 JSON 并累积内容
     */
    public void appendChunk(byte[] chunk) {
        try {
            String chunkStr = new String(chunk);
            
            // 处理SSE格式：移除"data: "前缀
            String jsonStr = chunkStr;
            if (chunkStr.startsWith("data: ")) {
                jsonStr = chunkStr.substring(6); // 移除"data: "前缀
            }
            
            // 跳过空行或非JSON数据
            jsonStr = jsonStr.trim();
            if (jsonStr.isEmpty() || !jsonStr.startsWith("{")) {
                return;
            }
            
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode choices = root.path("choices");
            if (choices.isArray()) {
                for (JsonNode choice : choices) {
                    JsonNode delta = choice.path("delta");
                    JsonNode content = delta.path("content");
                    if (content.isTextual()) {
                        contentBuilder.append(content.asText());
                    }
                }
            }
        } catch (IOException e) {
            // 记录解析错误（用于调试）
            System.err.println("JSON解析错误: " + e.getMessage() + ", 数据: " + new String(chunk));
        }
    }

    /**
     * 获取完整内容
     */
    public String getFullContent() {
        return contentBuilder.toString();
    }

    /**
     * 重置累积器
     */
    public void reset() {
        contentBuilder.setLength(0);
    }
}