package com.hewei.hzyjy.xunzhi.dto.req.ai;

import lombok.Data;


/**
 * AI消息请求DTO
 * @author nageoffer
 */
@Data
public class AiMessageReqDTO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户输入消息
     */
    private String inputMessage;
    
    /**
     * AI配置ID
     */
    private Long aiId;
    
    /**
     * 消息序号
     */
    private Integer messageSeq;
    
    /**
     * 用户名
     */
    private String userName;
}