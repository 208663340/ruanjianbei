package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 星火AI客户端
 * 基于星火大模型4.0Ultra的对话功能
 */
@Component
public class SparkAIClient {

    private static final Gson gson = new Gson(); // Keep if RoleContent or other parsing is needed, otherwise can be removed.

    /**
     * 发送对话请求（支持流式和非流式响应）
     * @param input 用户输入
     * @param history 历史对话记录 (JSON字符串格式)
     * @param stream 是否启用流式响应
     * @param outputStream 输出流，用于写入AI响应
     * @param callback 回调函数，处理每个接收到的数据块
     * @param customApiKey Spark API密钥
     * @param model 使用的AI模型 (例如 "generalv3.5", "4.0Ultra")
     * @throws Exception 请求或处理过程中发生异常
     */
    public void chatStream(String input, String history, boolean stream, OutputStream outputStream,
                     Consumer<String> callback, String customApiKey, String model) throws Exception {
        String API_URL = "https://spark-api-open.xf-yun.com/v1/chat/completions";
        URL url = new URL(API_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30_000); // 连接超时30秒
        conn.setReadTimeout(300_000); // 读取超时5分钟，适应长时间输出

        //请求头
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", stream ? "text/event-stream" : "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + customApiKey);
        conn.setDoOutput(true);

        // 创建请求的JSON对象
        JSONObject payload = new JSONObject();
        payload.put("model", model);

        JSONArray messagesArray = new JSONArray();
        // 处理历史消息
        if (history != null && !history.trim().isEmpty()) {
            try {
                JSONArray historyJsonArray = JSON.parseArray(history);
                for (Object msg : historyJsonArray) {
                    if (msg instanceof JSONObject) {
                        messagesArray.add(msg);
                    } else if (msg instanceof String) {

                    }
                }
            } catch (Exception e) {
                System.err.println("[SparkAIClient] Error parsing history JSON: " + e.getMessage());
                // Decide how to handle malformed history, e.g., log and continue or throw
            }
        }

        // 添加当前用户输入
        JSONObject currentUserMessage = new JSONObject();
        currentUserMessage.put("role", "user");
        currentUserMessage.put("content", input);
        messagesArray.add(currentUserMessage);

        payload.put("messages", messagesArray);
        payload.put("stream", stream);
        // Optional: Add other parameters like max_tokens, temperature if needed and made configurable
        // payload.put("max_tokens", 8192);
        // payload.put("temperature", 0.1);

        System.out.println("[SparkAIClient] Request Payload: " + payload.toString());

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
            }
            throw new RuntimeException("HTTP请求失败，响应码: " + responseCode + ", 响应体: " + errorResponse.toString());
        }

        if (stream) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // 过滤空行和无效数据
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 检查连接是否被中断
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("[SparkAIClient] 检测到线程中断，停止数据读取");
                        break;
                    }
                    // 处理SSE格式数据和结束标记
                    String processedLine = null;

                    if (line.startsWith("data: ")) {
                        // 提取SSE数据部分
                        String dataContent = line.substring(6).trim();
                        if (dataContent.startsWith("{") || dataContent.equals("[DONE]")) {
                            processedLine = dataContent;
                        }
                    } else if (line.startsWith("{") || line.equals("[DONE]")) {
                        // 直接的JSON数据或结束标记
                        processedLine = line;
                    }
                    if (processedLine != null) {
                        callback.accept(processedLine);
                        // 打印到控制台
                        System.out.println("[AI数据接收] " + processedLine);

                        // 发送处理后的数据
                        outputStream.write(processedLine.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    } else {
                        System.out.println("[数据过滤] 跳过无效行: " + line);
                    }
                }
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String response = br.readLine();
                callback.accept(response);
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }

        conn.disconnect();
    }
}