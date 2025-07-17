package com.hewei.hzyjy.xunzhi.common.util;

import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.dto.resp.file.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传工具类
 * 提供文件类型验证、大小限制等功能
 */
public class FileUploadUtil {

    /**
     * 支持的文件类型枚举
     */
    public enum FileType {
        PDF("application/pdf", Arrays.asList(".pdf"), 20 * 1024 * 1024), // 20MB
        IMAGE("image/", Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp"), 10 * 1024 * 1024), // 10MB
        AUDIO("audio/", Arrays.asList(".mp3", ".wav", ".pcm", ".m4a"), 50 * 1024 * 1024), // 50MB
        VIDEO("video/", Arrays.asList(".mp4", ".avi", ".mov", ".wmv"), 100 * 1024 * 1024); // 100MB

        private final String mimeTypePrefix;
        private final List<String> extensions;
        private final long maxSize;

        FileType(String mimeTypePrefix, List<String> extensions, long maxSize) {
            this.mimeTypePrefix = mimeTypePrefix;
            this.extensions = extensions;
            this.maxSize = maxSize;
        }

        public String getMimeTypePrefix() {
            return mimeTypePrefix;
        }

        public List<String> getExtensions() {
            return extensions;
        }

        public long getMaxSize() {
            return maxSize;
        }
    }

    /**
     * 验证并处理文件上传
     * 
     * @param file 上传的文件
     * @param expectedType 期望的文件类型
     * @return 处理后的文件信息
     * @throws ClientException 当文件不符合要求时抛出异常
     */
    public static FileUploadResult validateAndProcessFile(MultipartFile file, FileType expectedType) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > expectedType.getMaxSize()) {
            throw new ClientException(String.format("文件大小超过限制，最大允许 %d MB", 
                    expectedType.getMaxSize() / (1024 * 1024)));
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(expectedType.getMimeTypePrefix())) {
            throw new ClientException(String.format("文件类型不正确，只支持 %s 类型文件", 
                    expectedType.name().toLowerCase()));
        }

        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!expectedType.getExtensions().contains(extension)) {
            throw new ClientException(String.format("不支持的文件扩展名，只支持：%s", 
                    String.join(", ", expectedType.getExtensions())));
        }

        try {
            return FileUploadResult.builder()
                    .originalFilename(originalFilename)
                    .contentType(contentType)
                    .size(file.getSize())
                    .extension(extension)
                    .fileData(file.getBytes())
                    .build();
        } catch (IOException e) {
            throw new ClientException("读取文件内容失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名（包含点号）
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

}