package com.hewei.hzyjy.xunzhi.dto.resp.file;

import lombok.Data;

/**
 * 文件上传结果封装类
 */
@Data
public class FileUploadResult {
    private String originalFilename;
    private String contentType;
    private long size;
    private String extension;
    private byte[] fileData;

    private FileUploadResult() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final FileUploadResult result = new FileUploadResult();

        public Builder originalFilename(String originalFilename) {
            result.originalFilename = originalFilename;
            return this;
        }

        public Builder contentType(String contentType) {
            result.contentType = contentType;
            return this;
        }

        public Builder size(long size) {
            result.size = size;
            return this;
        }

        public Builder extension(String extension) {
            result.extension = extension;
            return this;
        }

        public Builder fileData(byte[] fileData) {
            result.fileData = fileData;
            return this;
        }

        public FileUploadResult build() {
            return result;
        }
    }
}