package com.nageoffer.shortlink.xunzhi.dto.resp.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent会话创建响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionCreateRespDTO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话标题
     */
    private String conversationTitle;
}