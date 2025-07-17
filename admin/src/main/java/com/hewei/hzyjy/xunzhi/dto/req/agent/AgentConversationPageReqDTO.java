package com.hewei.hzyjy.xunzhi.dto.req.agent;

import lombok.Data;

/**
 * 智能体会话分页查询请求DTO
 */
@Data
public class AgentConversationPageReqDTO {
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 智能体ID（可选）
     */
    private Long agentId;
    
    /**
     * 会话状态（可选）：1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 搜索关键词（可选）
     */
    private String keyword;
}