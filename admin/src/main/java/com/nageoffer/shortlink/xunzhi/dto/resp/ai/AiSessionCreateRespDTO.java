package com.nageoffer.shortlink.xunzhi.dto.resp.ai;

import lombok.Data;

/**
 * AI会话创建响应DTO
 * @author nageoffer
 */
@Data
public class AiSessionCreateRespDTO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话标题
     */
    private String conversationTitle;
}