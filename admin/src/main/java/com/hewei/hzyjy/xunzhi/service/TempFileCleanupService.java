package com.hewei.hzyjy.xunzhi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 临时文件清理服务
 * 定期清理过期的临时文件
 */
@Slf4j
@Service
public class TempFileCleanupService {
    
    private static final String TEMP_DIR = "temp";
    private static final long FILE_EXPIRE_HOURS = 24; // 24小时后过期
    
    /**
     * 定时清理临时文件
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cleanupExpiredTempFiles() {
        log.debug("开始清理过期临时文件...");
        
        try {
            Path tempDir = Path.of(TEMP_DIR);
            if (!Files.exists(tempDir)) {
                return;
            }
            
            cleanupDirectory(tempDir);
            log.debug("临时文件清理完成");
            
        } catch (Exception e) {
            log.error("清理临时文件时发生错误", e);
        }
    }
    
    /**
     * 递归清理目录中的过期文件
     */
    private void cleanupDirectory(Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    // 递归清理子目录
                    cleanupDirectory(path);
                    // 如果子目录为空，删除它
                    if (isDirectoryEmpty(path)) {
                        try {
                            Files.delete(path);
                            log.debug("删除空目录: {}", path);
                        } catch (IOException e) {
                            log.debug("删除空目录失败: {}", path);
                        }
                    }
                } else {
                    // 检查文件是否过期
                    if (isFileExpired(path)) {
                        try {
                            Files.delete(path);
                            log.debug("删除过期临时文件: {}", path);
                        } catch (IOException e) {
                            log.debug("删除过期临时文件失败: {}, 原因: {}", path, e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("清理目录失败: {}", directory, e);
        }
    }
    
    /**
     * 检查文件是否过期
     */
    private boolean isFileExpired(Path file) {
        try {
            Instant lastModified = Files.getLastModifiedTime(file).toInstant();
            Instant expireTime = Instant.now().minus(FILE_EXPIRE_HOURS, ChronoUnit.HOURS);
            return lastModified.isBefore(expireTime);
        } catch (IOException e) {
            log.debug("获取文件修改时间失败: {}", file);
            return false;
        }
    }
    
    /**
     * 检查目录是否为空
     */
    private boolean isDirectoryEmpty(Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 手动清理所有临时文件（用于测试或紧急清理）
     */
    public void forceCleanupAllTempFiles() {
        log.info("开始强制清理所有临时文件...");
        
        try {
            Path tempDir = Path.of(TEMP_DIR);
            if (Files.exists(tempDir)) {
                deleteDirectoryRecursively(tempDir);
                log.info("强制清理临时文件完成");
            }
        } catch (Exception e) {
            log.error("强制清理临时文件失败", e);
        }
    }
    
    /**
     * 递归删除目录及其所有内容
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    deleteDirectoryRecursively(path);
                }
                Files.delete(path);
            }
        }
        Files.delete(directory);
    }
}