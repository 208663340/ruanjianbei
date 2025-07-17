package com.hewei.hzyjy.xunzhi.dto.req.agent;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 面试题回答请求DTO
 */
@Data
public class InterviewAnswerReqDTO {
    
    /**
     * 题号
     */
    private String questionNumber;
    
    /**
     * 用户回答内容（文字形式，与录音文件二选一）
     */
    private String answerContent;
    
    /**
     * 用户回答录音文件（与文字内容二选一）
     */
    private MultipartFile audioFile;
    
    /**
     * 会话ID（可选，用于关联面试会话）
     */
    private String sessionId;
    
    /**
     * Agent ID（可选，用于指定评分的Agent）
     */
    private Long agentId;
}