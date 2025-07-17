package com.hewei.hzyjy.xunzhi.dto.resp.agent;

import lombok.Data;

/**
 * 面试题回答响应DTO
 */
@Data
public class InterviewAnswerRespDTO {
    
    /**
     * 题号
     */
    private String questionNumber;
    
    /**
     * 题目内容
     */
    private String questionContent;
    
    /**
     * 本次得分
     */
    private Integer score;
    
    /**
     * 累计总分
     */
    private Integer totalScore;
    
    /**
     * 是否成功
     */
    private Boolean isSuccess;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * AI评价内容（可选）
     */
    private String feedback;
}