package com.hewei.hzyjy.xunzhi.dto.req.agent;

import lombok.Data;

/**
 * Agent会话创建请求DTO
 */
@Data
public class AgentSessionCreateReqDTO {

    /**
     * 用户名
     */
    private String userName;

    /**
     * AgentID
     */
    private Long agentId;

    /**
     * 首条消息内容
     */
    private String firstMessage;
}