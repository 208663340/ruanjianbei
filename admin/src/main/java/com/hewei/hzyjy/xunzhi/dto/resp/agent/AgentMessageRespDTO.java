package com.hewei.hzyjy.xunzhi.dto.resp.agent;

import lombok.Data;

/**
 * 智能体消息响应DTO
 */
@Data
public class AgentMessageRespDTO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 智能体id
     */
    private Long agentId;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 智能体消息
     */
    private String chatMessage;

    /**
     * 消息序号
     */
    private Integer messageSeq;

    /**
     * Token消耗数量
     */
    private Integer tokenCount;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;

    /**
     * 是否成功
     */
    private Integer isSuccess = 1;

    /**
     * 错误信息
     */
    private String errorMessage;
}