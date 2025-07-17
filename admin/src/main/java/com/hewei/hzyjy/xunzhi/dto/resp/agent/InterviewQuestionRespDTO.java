package com.hewei.hzyjy.xunzhi.dto.resp.agent;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 面试题响应DTO
 */
@Data
public class InterviewQuestionRespDTO {

    /**
     * ID
     */
    private String id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 面试题Map（题号 -> 题目内容）
     */
    private Map<String, String> questions;

    /**
     * 建议Map（编号 -> 建议内容）
     */
    private Map<String, String> suggestions;

    /**
     * 面试类型（如：backend、frontend等）
     */
    private String interviewType;

    /**
     * 简历文件URL
     */
    private String resumeFileUrl;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;

    /**
     * Token消耗数量
     */
    private Integer tokenCount;

    /**
     * 简历评分（0-100）
     */
    private Integer resumeScore;
    
    /**
     * 生成的面试题数量
     */
    private Integer questionCount;
    
    /**
     * 建议数量
     */
    private Integer suggestionCount;
    
    /**
     * 是否成功
     */
    private Integer isSuccess = 1;

    /**
     * 错误信息（如果处理失败）
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}