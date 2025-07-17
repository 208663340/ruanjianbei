package com.hewei.hzyjy.xunzhi.dto.req.agent;

import lombok.Data;

/**
 * 智能体聊天请求DTO
 */
@Data
public class AgentChatReqDTO {
    
    /**
     * 会话ID（可选，新会话时为空）
     */
    private String sessionId;
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 用户输入消息
     */
    private String inputMessage;
    
    /**
     * 用户名
     */
    private String userName;
}