package com.hewei.hzyjy.xunzhi.service.tool;

import cn.xfyun.api.TupApiClient;
import cn.xfyun.config.TupApiEnum;
import com.alibaba.fastjson2.JSON;
import com.hewei.hzyjy.xunzhi.config.xunfei.XunfeiLatProperties;
import com.hewei.hzyjy.xunzhi.dto.resp.xunfei.ExpressionRecognitionResponse;
import com.hewei.hzyjy.xunzhi.dto.resp.xunfei.ExpressionRecognitionResult;
import com.hewei.hzyjy.xunzhi.enums.ExpressionType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * 讯飞表情识别服务
 * 支持人脸表情识别功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class XunfeiFaceService {
    
    private final XunfeiLatProperties xunfeiLatPropertiesConfig;

    private TupApiClient tupApiClient;

    @PostConstruct
    public void init() {
        try {
            // 初始化表情识别客户端
            tupApiClient = new TupApiClient
                    .Builder(xunfeiLatPropertiesConfig.getAppId(),
                            xunfeiLatPropertiesConfig.getApiKey(),
                            TupApiEnum.EXPRESSION)
                    .build();
                    
            log.info("讯飞表情识别服务初始化成功");
        } catch (Exception e) {
            log.error("讯飞表情识别服务初始化失败", e);
            throw new RuntimeException("讯飞表情识别服务初始化失败", e);
        }
    }

    /**
     * 表情识别
     * @param image 人脸图片
     * @return 表情识别结果
     */
    public CompletableFuture<ExpressionRecognitionResult> detectExpression(MultipartFile image) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] imageBytes = image.getBytes();
                // 使用TupApiClient进行表情识别
                String response = tupApiClient.recognition(image.getOriginalFilename(), imageBytes);
                
                log.info("表情识别响应：{}", response);
                
                if (response != null && !response.trim().isEmpty()) {
                    // 解析JSON响应
                    ExpressionRecognitionResponse apiResponse = JSON.parseObject(response, ExpressionRecognitionResponse.class);
                    
                    if (apiResponse != null && apiResponse.getCode() != null && apiResponse.getCode() == 0) {
                        return parseExpressionResult(apiResponse, image.getOriginalFilename());
                    } else {
                        log.warn("表情识别失败，响应码：{}, 描述：{}", 
                                apiResponse != null ? apiResponse.getCode() : "null", 
                                apiResponse != null ? apiResponse.getDesc() : "null");
                        return new ExpressionRecognitionResult(false, null, 0.0, 
                                "表情识别失败：" + (apiResponse != null ? apiResponse.getDesc() : "未知错误"), 
                                image.getOriginalFilename(), 
                                apiResponse != null ? apiResponse.getSid() : null);
                    }
                } else {
                    log.warn("表情识别响应为空");
                    return new ExpressionRecognitionResult(false, null, 0.0, "表情识别响应为空", 
                            image.getOriginalFilename(), null);
                }
            } catch (Exception e) {
                log.error("表情识别异常", e);
                return new ExpressionRecognitionResult(false, null, 0.0, 
                        "表情识别异常：" + e.getMessage(), 
                        image.getOriginalFilename(), null);
            }
        });
    }


    /**
     * 解析表情识别结果
     * @param apiResponse API响应对象
     * @param fileName 文件名
     * @return 表情识别结果
     */
    private ExpressionRecognitionResult parseExpressionResult(ExpressionRecognitionResponse apiResponse, String fileName) {
        try {
            if (apiResponse.getData() != null && 
                apiResponse.getData().getFileList() != null && 
                !apiResponse.getData().getFileList().isEmpty()) {
                
                ExpressionRecognitionResponse.FileResult fileResult = apiResponse.getData().getFileList().get(0);
                
                // 根据label获取表情类型
                ExpressionType expressionType = ExpressionType.getByCode(fileResult.getLabel());
                
                log.info("表情识别成功 - 文件：{}, 表情：{}, 置信度：{}", 
                        fileName, expressionType.getDescription(), fileResult.getRate());
                
                return new ExpressionRecognitionResult(
                        true,
                        expressionType.getDescription(),
                        fileResult.getRate(),
                        "表情识别成功",
                        fileName,
                        apiResponse.getSid()
                );
            } else {
                log.warn("表情识别响应数据为空或无文件结果");
                return new ExpressionRecognitionResult(
                        false,
                        null,
                        0.0,
                        "表情识别响应数据为空",
                        fileName,
                        apiResponse.getSid()
                );
            }
        } catch (Exception e) {
            log.error("解析表情识别结果异常", e);
            return new ExpressionRecognitionResult(
                    false,
                    null,
                    0.0,
                    "解析结果异常：" + e.getMessage(),
                    fileName,
                    apiResponse.getSid()
            );
        }
    }

}