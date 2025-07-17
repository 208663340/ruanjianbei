package com.hewei.hzyjy.xunzhi.service.tool;

import cn.xfyun.api.IatClient;
import cn.xfyun.api.RtasrClient;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.model.response.iat.Text;
import cn.xfyun.model.response.rtasr.RtasrResponse;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import cn.xfyun.service.rta.AbstractRtasrWebSocketListener;
import com.alibaba.fastjson2.JSONObject;
import com.hewei.hzyjy.xunzhi.config.xunfei.XunfeiLatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;

import org.apache.commons.codec.binary.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞音频转文字服务
 * 支持实时语音识别和语音听写功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XunfeiAudioService {
    
    private final XunfeiLatProperties xunfeiLatPropertiesConfig;

    private IatClient iatClient;
    private RtasrClient rtasrClient;

    @PostConstruct
    public void init() {
        try {
            // 初始化语音听写客户端
            iatClient = new IatClient.Builder()
                    .signature(xunfeiLatPropertiesConfig.getAppId(),
                              xunfeiLatPropertiesConfig.getApiKey(),
                              xunfeiLatPropertiesConfig.getApiSecret())
                    .dwa("wpgs") // 动态修正功能
                    .build();
            
            // 初始化实时语音转写客户端
//            rtasrClient = new RtasrClient.Builder()
//                    .signature(xunfeiLatPropertiesConfig.getAppId(),
//                              xunfeiLatPropertiesConfig.getRtaAPIKey())
//                    .build();
                    
            log.info("讯飞音频服务初始化成功");
        } catch (Exception e) {
            log.error("讯飞音频服务初始化失败", e);
            throw new RuntimeException("讯飞音频服务初始化失败", e);
        }
    }

    /**
     * 音频文件转文字
     * @param audioFile 音频文件
     * @return 转换结果
     */
    public CompletableFuture<String> convertAudioToText(MultipartFile audioFile) {
        CompletableFuture<String> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);
        List<Text> resultSegments = new ArrayList<>();
        
        AbstractIatWebSocketListener listener = new AbstractIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
                 //处理错误
                if (iatResponse.getCode() != 0) {
                    log.warn("语音听写错误：code={}, message={}, sid={}", 
                            iatResponse.getCode(), iatResponse.getMessage(), iatResponse.getSid());
                    future.completeExceptionally(new RuntimeException("语音听写失败：" + iatResponse.getMessage()));
                    latch.countDown();
                    return;
                }
                
                if (iatResponse.getData() != null && iatResponse.getData().getResult() != null) {
                    IatResult result = iatResponse.getData().getResult();
                    Text textObject = result.getText();
                    if (textObject != null) {
                        handleResultText(textObject, resultSegments);
                        log.info("中间识别结果：{}", getFinalResult(resultSegments));
                    }
                    
                    // 检查是否为最终结果
                    if (iatResponse.getData().getStatus() == 2) {
                        log.info("识别完成，最终结果：{}", getFinalResult(resultSegments));
                        future.complete(getFinalResult(resultSegments));
                        latch.countDown();
                    }
                }
            }
            
            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                log.error("语音听写失败", t);
                future.completeExceptionally(t);
                latch.countDown();
            }
        };
        
        try {
            // 将MultipartFile转换为临时文件（使用resources目录）
            Path tempDir = Path.of("src", "main", "resources", "temp", "audio");
            Files.createDirectories(tempDir);
            
            String fileName = "audio_" + System.currentTimeMillis() + "_" + 
                             Thread.currentThread().getId() + ".tmp";
            Path tempFilePath = tempDir.resolve(fileName);
            
            try (var inputStream = audioFile.getInputStream()) {
                Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            File tempFile = tempFilePath.toFile();
            
            iatClient.send(tempFile, listener);
            
            // 等待结果，最多等待60秒
            if (!latch.await(60, TimeUnit.SECONDS)) {
                future.completeExceptionally(new RuntimeException("语音听写超时"));
            }
            
            // 清理临时文件（使用改进的删除策略）
            cleanupTempFile(tempFilePath);
            
        } catch (Exception e) {
            log.error("发送音频数据失败", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    /**
     * 处理返回结果（包括全量返回与流式返回（结果修正））
     */
    private static void handleResultText(Text textObject,List<Text> resultSegments) {
        // 处理流式返回的替换结果
        if (StringUtils.equals(textObject.getPgs(), "rpl") && textObject.getRg() != null && textObject.getRg().length == 2) {
            // 返回结果序号sn字段的最小值为1
            int start = textObject.getRg()[0] - 1;
            int end = textObject.getRg()[1] - 1;

            // 将指定区间的结果设置为删除状态
            for (int i = start; i <= end && i < resultSegments.size(); i++) {
                resultSegments.get(i).setDeleted(true);
            }
            // logger.info("替换操作，服务端返回结果为：" + textObject);
        }

        // 通用逻辑，添加当前文本到结果列表
        resultSegments.add(textObject);
    }

    /**
     * 获取最终结果
     */
    private static String getFinalResult(List<Text> resultSegments) {
        StringBuilder finalResult = new StringBuilder();
        for (Text text : resultSegments) {
            if (text != null && !text.isDeleted()) {
                finalResult.append(text.getText());
            }
        }
        return finalResult.toString();
    }


    /**
     * 实时音频流转文字
     * @param audioInputStream 音频输入流
     * @param callback 实时结果回调
     * @return 完整识别结果
     */
    public CompletableFuture<String> realTimeAudioToText(InputStream audioInputStream, AudioResultCallback callback) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuffer finalResult = new StringBuffer();
        CountDownLatch latch = new CountDownLatch(1);

        AbstractRtasrWebSocketListener listener = new AbstractRtasrWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, String text) {
                try {
                    RtasrResponse response = JSONObject.parseObject(text, RtasrResponse.class);
                    String tempResult = handleAndReturnContent(response.getData());
                    
                    if (tempResult != null && !tempResult.isEmpty()) {
                        finalResult.append(tempResult);
                        
                        // 实时回调
                        if (callback != null) {
                            callback.onResult(finalResult.toString());
                        }
                        
                        log.info("实时转写结果：{}", finalResult.toString());
                    }
                } catch (Exception e) {
                    log.error("解析实时转写结果失败", e);
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, @Nullable Response response) {
                log.error("实时转写失败", t);
                future.completeExceptionally(new RuntimeException("实时转写失败", t));
                latch.countDown();
            }

            @Override
            public void onBusinessFail(WebSocket webSocket, String text) {
                log.error("实时转写业务异常：{}", text);
                future.completeExceptionally(new RuntimeException("实时转写业务异常：" + text));
                latch.countDown();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("实时转写连接失败", t);
                future.completeExceptionally(new RuntimeException("实时转写连接失败", t));
                latch.countDown();
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                future.complete(finalResult.toString());
                latch.countDown();
            }

            @Override
            public void onClosed() {
                future.complete(finalResult.toString());
                latch.countDown();
            }
        };

        try {
            rtasrClient.send(audioInputStream, listener);
            // 等待转写完成或超时
            latch.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("实时转写失败", e);
            future.completeExceptionally(e);
        }

        return future;
    }



    /**
     * 处理实时转写数据
     */
    private String handleAndReturnContent(Object data) {
        // 这里需要根据实际的RtasrResponse.Data结构来实现
        // 简化处理，实际使用时需要根据讯飞SDK的具体实现来调整
        if (data != null) {
            return data.toString();
        }
        return "";
    }

    /**
     * 清理临时文件（简化版本）
     */
    private void cleanupTempFile(Path tempFile) {
        if (tempFile == null || !Files.exists(tempFile)) {
            return;
        }
        
        try {
            Files.delete(tempFile);
            log.debug("临时文件删除成功: {}", tempFile);
        } catch (Exception e) {
            log.warn("临时文件删除失败: {}, 原因: {}", tempFile, e.getMessage());
            // 简单标记为JVM退出时删除
            tempFile.toFile().deleteOnExit();
        }
    }

    /**
     * 音频识别结果回调接口
     */
    public interface AudioResultCallback {
        void onResult(String result);
    }
}