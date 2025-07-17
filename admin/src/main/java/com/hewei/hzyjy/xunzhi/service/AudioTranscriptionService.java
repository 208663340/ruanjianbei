package com.hewei.hzyjy.xunzhi.service;

import cn.xfyun.config.SparkIatModelEnum;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.SparkIatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 音频转写服务
 * 负责处理音频转文字的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTranscriptionService {
    
    private final SparkIatUtil sparkIatUtil;
    
    /**
     * 异步音频转文字
     * @param audioFile 音频文件
     * @param partialResultCallback 中间结果回调（可选）
     * @return 转写结果的异步任务
     */
    public CompletableFuture<String> transcribeAsync(MultipartFile audioFile, 
                                                    Consumer<String> partialResultCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return transcribeSync(audioFile, partialResultCallback);
            } catch (Exception e) {
                log.error("异步音频转写失败", e);
                throw new RuntimeException("音频转写失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 简单的音频转文字方法（用于面试题回答）
     * @param audioFile 音频文件
     * @return 转写结果
     * @throws Exception 转写异常
     */
    public String transcribeAudio(MultipartFile audioFile) throws Exception {
        return transcribeSync(audioFile, null);
    }
    
    /**
     * 同步音频转文字
     * @param audioFile 音频文件
     * @param partialResultCallback 中间结果回调（可选）
     * @return 转写结果
     * @throws Exception 转写异常
     */
    public String transcribeSync(MultipartFile audioFile, 
                                Consumer<String> partialResultCallback) throws Exception {
        validateAudioFile(audioFile);
        
        Path tempFile = createTempAudioFile(audioFile);
        
        try {
            SparkIatUtil.IatConfig config = buildTranscriptionConfig();
            String result = sparkIatUtil.transcribeSync(tempFile.toFile(), config, partialResultCallback);
            
            log.info("音频转写完成，文件: {}, 结果长度: {}", 
                    audioFile.getOriginalFilename(), result.length());
            return result;
            
        } finally {
            cleanupTempFile(tempFile);
        }
    }
    
    /**
     * 异步音频转文字（带回调）
     * @param audioFile 音频文件
     * @param callback 转写回调
     */
    public void transcribeWithCallback(MultipartFile audioFile, 
                                      AudioTranscriptionCallback callback) {
        try {
            validateAudioFile(audioFile);
            Path tempFile = createTempAudioFile(audioFile);
            
            SparkIatUtil.IatConfig config = buildTranscriptionConfig();
            
            sparkIatUtil.transcribeAsync(tempFile.toFile(), config, new SparkIatUtil.IatCallback() {
                @Override
                public void onSuccess(String result) {
                    cleanupTempFile(tempFile);
                    callback.onSuccess(result);
                }
                
                @Override
                public void onError(Exception error) {
                    cleanupTempFile(tempFile);
                    callback.onError(error);
                }
                
                @Override
                public void onPartialResult(String partialResult) {
                    callback.onPartialResult(partialResult);
                }
            });
            
        } catch (Exception e) {
            log.error("启动异步音频转写失败", e);
            callback.onError(e);
        }
    }
    
    /**
     * 验证音频文件
     */
    private void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }
        
        // 检查文件大小（例如限制为50MB）
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (audioFile.getSize() > maxSize) {
            throw new IllegalArgumentException("音频文件大小不能超过50MB");
        }
        
        // 检查文件类型
        String filename = audioFile.getOriginalFilename();
        if (filename != null) {
            String extension = getFileExtension(filename).toLowerCase();
            if (!isSupportedAudioFormat(extension)) {
                // 特别提示m4a和aac格式不被支持
                if (".m4a".equals(extension) || ".aac".equals(extension)) {
                    throw new IllegalArgumentException("不支持的音频格式: " + extension + 
                        "。讯飞星火语音听写仅支持: pcm, wav, mp3, flac。请使用音频转换工具（如FFmpeg）将" + extension + "格式转换为wav或mp3格式后重试。");
                }
                throw new IllegalArgumentException("不支持的音频格式: " + extension + "，仅支持: pcm, wav, mp3, flac");
            }
        }
    }
    
    /**
     * 检查是否为支持的音频格式
     */
    private boolean isSupportedAudioFormat(String extension) {
        // 讯飞星火语音听写（SparkIat）支持的音频格式：pcm、wav、mp3、flac
        // 注意：m4a、aac格式不被支持，需要先转换为支持的格式
        return extension.equals(".pcm") || extension.equals(".wav") || 
               extension.equals(".mp3") || extension.equals(".flac");
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    /**
     * 创建临时音频文件（在项目内部目录）
     */
    private Path createTempAudioFile(MultipartFile audioFile) throws IOException {
        String extension = getFileExtension(audioFile.getOriginalFilename());
        
        // 创建临时音频文件（使用resources目录）
        Path tempDir = Path.of("src", "main", "resources", "temp", "audio");
        Files.createDirectories(tempDir);
        
        // 生成唯一的文件名
        String fileName = "audio_transcription_" + System.currentTimeMillis() + "_" + 
                         Thread.currentThread().getId() + extension;
        Path tempFile = tempDir.resolve(fileName);
        
        try (var inputStream = audioFile.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.debug("创建临时音频文件: {}, 大小: {} bytes", tempFile, audioFile.getSize());
        return tempFile;
    }
    
    /**
     * 构建转写配置（从yml配置文件读取）
     */
    private SparkIatUtil.IatConfig buildTranscriptionConfig() {
        return sparkIatUtil.createDefaultConfig();
    }
    
    /**
     * 构建自定义转写配置
     * @param model 语音模型
     * @param dwa 动态修正参数
     * @param timeoutSeconds 超时时间
     * @return 转写配置
     */
    private SparkIatUtil.IatConfig buildCustomTranscriptionConfig(SparkIatModelEnum model, 
                                                                  String dwa, 
                                                                  int timeoutSeconds) {
        return sparkIatUtil.createCustomConfig(model, dwa, timeoutSeconds);
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
     * 音频转写回调接口
     */
    public interface AudioTranscriptionCallback {
        /**
         * 转写成功回调
         * @param result 转写结果
         */
        void onSuccess(String result);
        
        /**
         * 转写失败回调
         * @param error 错误信息
         */
        void onError(Exception error);
        
        /**
         * 中间结果回调
         * @param partialResult 中间结果
         */
        default void onPartialResult(String partialResult) {
            // 默认空实现
        }
    }
}