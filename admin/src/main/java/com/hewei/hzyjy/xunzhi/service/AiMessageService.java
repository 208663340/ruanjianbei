package com.hewei.hzyjy.xunzhi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiMessageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiMessageHistoryRespDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI消息Service接口
 * @author nageoffer
 */
public interface AiMessageService {
    
    /**
     * AI聊天SSE接口
     */
    SseEmitter aiChatSse(AiMessageReqDTO requestParam);
    
    /**
     * 查询会话历史消息
     */
    List<AiMessageHistoryRespDTO> getConversationHistory(String sessionId);
    
    /**
     * 分页查询历史消息
     */
    IPage<AiMessageHistoryRespDTO> pageHistoryMessages(String username, String sessionId, Integer current, Integer size);
    
    /**
     * 根据用户名获取用户ID
     */
    Long getUserIdByUsername(String username);
}