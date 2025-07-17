package com.hewei.hzyjy.xunzhi.dto.resp.agent;

import lombok.Data;

import java.util.Date;

/**
 * 智能体会话响应DTO
 */
@Data
public class AgentConversationRespDTO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 智能体名称
     */
    private String agentName;
    
    /**
     * 会话标题
     */
    private String conversationTitle;
    
    /**
     * 消息总数
     */
    private Integer messageCount;
    
    /**
     * 总Token消耗
     */
    private Integer totalTokens;
    
    /**
     * 会话状态：1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 最后更新时间
     */
    private Date updateTime;
}