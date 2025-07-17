package com.hewei.hzyjy.xunzhi.controller;

import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.common.util.FileUploadUtil;
import com.hewei.hzyjy.xunzhi.dto.resp.file.FileUploadResult;
import com.hewei.hzyjy.xunzhi.dto.resp.xunfei.ExpressionRecognitionResult;
import com.hewei.hzyjy.xunzhi.service.AudioTranscriptionService;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.XingChenAIClient;
import com.hewei.hzyjy.xunzhi.service.tool.XunfeiAudioService;
import com.hewei.hzyjy.xunzhi.service.tool.XunfeiFaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * 讯飞AI功能控制器
 * 提供音频转文字和表情识别的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/xunzhi/v1/xunfei")
@RequiredArgsConstructor
public class XunfeiController {

    private final XunfeiAudioService xunfeiAudioService;

    private final XunfeiFaceService xunfeiFaceService;
    
    private final AudioTranscriptionService audioTranscriptionService;
    
    private final XingChenAIClient xingChenAIClient;

    /**
     * 实时音频文件转文字
     * @param audioFile 音频文件
     * @return 转换结果
     */
    @PostMapping("/audio-to-text")
    public Result<CompletableFuture<String>> convertAudioToText(
            @RequestParam("file") MultipartFile audioFile) {
        return Results.success(xunfeiAudioService.convertAudioToText(audioFile));
    }


    /**
     * 表情识别
     * @param image 人脸图片
     * @return 表情识别结果
     */
    @PostMapping("/expression-recognition")
    public Result<CompletableFuture<ExpressionRecognitionResult>> detectExpression(
            @RequestParam("image") MultipartFile image) {
        return Results.success(xunfeiFaceService.detectExpression(image));
    }

    /**
     * 音频转文字（同步）
     * @param audioFile 音频文件
     * @return 转换结果
     */
    @PostMapping("/audio-transcribe")
    public Result<String> transcribeAudio(
            @RequestParam("file") MultipartFile audioFile) {
        
        try {
            // 使用同步转写，不需要中间结果回调
            String result = audioTranscriptionService.transcribeSync(audioFile, null);
            return Results.success(result);
        } catch (Exception e) {
            log.error("音频转写失败", e);
            throw new RuntimeException("音频转写失败: " + e.getMessage(), e);
        }
    }
    


    /**
     * PDF文件上传
     * @param file PDF文件（限制：只能是PDF格式，大小不超过20MB）
     * @return 文件上传结果
     */
    @PostMapping("/upload-pdf")
    public Result<FileUploadResult> uploadPdfFile(
            @RequestParam("file") MultipartFile file) {
        // 使用工具类验证PDF文件
        FileUploadResult result = FileUploadUtil.validateAndProcessFile(file, FileUploadUtil.FileType.PDF);
        return Results.success(result);
    }

}