package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

@Slf4j
@Component
public class XingChenAIClient {


    public void chat(String input,String chatId,String history, boolean stream, OutputStream outputStream, Consumer<String> callback,
                     String customApiKey, String customApiSecret, String customFlowId) throws Exception {
        chat(input, chatId, history, stream, outputStream, callback, customApiKey, customApiSecret, customFlowId, null);
    }
    
    /**
     * 带文件URL的聊天方法
     * @param input 用户输入
     * @param chatId 聊天ID
     * @param history 历史记录
     * @param stream 是否流式
     * @param outputStream 输出流
     * @param callback 回调函数
     * @param customApiKey API密钥
     * @param customApiSecret API密钥
     * @param customFlowId 工作流ID
     * @param fileUrl 文件URL（可选）
     * @throws Exception
     */
    public void chat(String input,String chatId,String history, boolean stream, OutputStream outputStream, Consumer<String> callback,
                     String customApiKey, String customApiSecret, String customFlowId, String fileUrl) throws Exception {
        String urlString = "https://xingchen-api.xf-yun.com/workflow/v1/chat/completions";
        URL url = new URL(urlString);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();


        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Authorization", "Bearer " + customApiKey + ":" + customApiSecret);
        conn.setDoOutput(true);

        // 使用JSONObject构建请求体，避免字符串转义问题
        JSONObject requestBody = new JSONObject();
        requestBody.put("flow_id", customFlowId);
        requestBody.put("uid", "123");
        requestBody.put("stream", stream);
        requestBody.put("chat_id", chatId);
        
        // 解析history字符串为JSON数组
        Object historyObj;
        try {
            historyObj = JSON.parse(history);
            // 如果解析结果不是数组，则转换为空数组
            if (!(historyObj instanceof java.util.List)) {
                log.warn("history不是数组格式，转换为空数组");
                historyObj = new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            log.warn("解析history失败，使用空数组: {}", e.getMessage());
            historyObj = new java.util.ArrayList<>();
        }
        requestBody.put("history", historyObj);
        
        // 构建parameters对象
        JSONObject parameters = new JSONObject();
        parameters.put("AGENT_USER_INPUT", input);
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            parameters.put("USER_FILE", fileUrl);
        }
        requestBody.put("parameters", parameters);
        
        String payload = requestBody.toJSONString();
        System.out.println(payload);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] inputBytes = payload.getBytes(StandardCharsets.UTF_8);
            os.write(inputBytes);
        }

        // 检查响应状态码
        int responseCode = conn.getResponseCode();
        log.info("讯飞API响应码: {}", responseCode);
        
        if (responseCode != 200) {
            // 读取错误响应
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                String errorBody = errorResponse.toString();
                log.error("讯飞API错误响应 [{}]: {}", responseCode, errorBody);
                throw new IOException("API请求失败，状态码: " + responseCode + ", 错误信息: " + errorBody);
            }
        }

        if (stream) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // 过滤空行和无效数据
                    if (line.trim().isEmpty()) {
                        continue;
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
    
    /**
     * 上传文件到讯飞服务器
     * @param file 要上传的文件
     * @param apiKey 讯飞API密钥
     * @param apiSecret 讯飞API密钥
     * @return 上传成功后返回的文件URL
     * @throws Exception 上传过程中的异常
     */
    public String uploadFile(MultipartFile file, String apiKey, String apiSecret) throws Exception {
        String urlString = "https://xingchen-api.xf-yun.com/workflow/v1/upload_file";
        URL url = new URL(urlString);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        
        // 设置请求头
        conn.setRequestProperty("Host", "xingchen-api.xf-yun.com");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey + ":" + apiSecret);

        // 设置multipart/form-data边界
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (OutputStream outputStream = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {
            
            // 写入文件数据，按照curl示例格式
            writer.append("--").append(boundary).append("\r\n");
            
            // 根据文件扩展名设置Content-Type
            String contentType = getContentTypeByFileName(file.getOriginalFilename());
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getOriginalFilename()).append("\"; type=").append(contentType).append("\r\n");
            writer.append("Content-Type: ").append(contentType).append("\r\n");
            writer.append("\r\n");
            writer.flush();
            
            // 写入文件内容
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            outputStream.flush();
            
            // 写入结束边界
            writer.append("\r\n");
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.flush();
        }
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        log.info("讯飞文件上传响应码: {}", responseCode);
        
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                String responseBody = response.toString();
                log.info("讯飞文件上传响应: {}", responseBody);
                
                // 解析响应获取文件URL
                return parseFileUrlFromResponse(responseBody);
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                log.error("讯飞文件上传失败: {}", errorResponse.toString());
                throw new RuntimeException("文件上传失败: " + errorResponse.toString());
            }
        }

    }
    
    /**
     * 根据文件名获取Content-Type
     * @param fileName 文件名
     * @return Content-Type
     */
    private String getContentTypeByFileName(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }
    
    /**
     * 从响应中解析文件URL
     * @param responseBody 响应体
     * @return 文件URL
     */
    private String parseFileUrlFromResponse(String responseBody) {
        try {
            // 使用fastjson2解析JSON响应
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            // 检查响应状态码
            Integer code = jsonResponse.getInteger("code");
            if (code == null || code != 0) {
                String message = jsonResponse.getString("message");
                throw new RuntimeException("文件上传失败，错误码: " + code + ", 错误信息: " + message);
            }
            
            // 从data字段中获取url
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                throw new RuntimeException("响应中缺少data字段: " + responseBody);
            }
            
            String url = data.getString("url");
            if (url == null || url.trim().isEmpty()) {
                throw new RuntimeException("响应中缺少url字段: " + responseBody);
            }
            
            log.info("解析到文件URL: {}", url);
            return url;
            
        } catch (Exception e) {
            log.error("解析文件URL失败，响应内容: {}", responseBody, e);
            throw new RuntimeException("解析文件URL失败: " + e.getMessage());
        }
    }
}