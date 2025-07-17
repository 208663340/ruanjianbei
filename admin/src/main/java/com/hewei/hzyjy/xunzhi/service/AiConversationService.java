package com.hewei.hzyjy.xunzhi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiConversationRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiSessionCreateRespDTO;

/**
 * AI会话Service接口
 * @author nageoffer
 */
public interface AiConversationService {
    
    /**
     * 创建AI会话
     */
    String createConversation(String username, Long aiId, String firstMessage);
    
    /**
     * 创建AI会话并生成标题
     */
    AiSessionCreateRespDTO createConversationWithTitle(String username, Long aiId, String firstMessage);
    
    /**
     * 分页查询用户会话列表
     */
    IPage<AiConversationRespDTO> pageConversations(String username, AiConversationPageReqDTO requestParam);
    
    /**
     * 更新会话信息
     */
    void updateConversation(String sessionId, Integer messageSeq, String title);
    
    /**
     * 结束会话
     */
    void endConversation(String sessionId);
    
    /**
     * 删除会话
     */
    void deleteConversation(String sessionId);
    
    /**
     * 根据会话ID获取会话信息
     */
    AiConversationRespDTO getConversationBySessionId(String sessionId);
}