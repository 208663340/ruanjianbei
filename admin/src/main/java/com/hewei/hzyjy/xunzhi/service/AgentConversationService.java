package com.hewei.hzyjy.xunzhi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.dto.req.agent.AgentConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentConversationRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentSessionCreateRespDTO;

/**
 * 智能体会话服务接口
 */
public interface AgentConversationService {

    /**
     * 创建新会话
     */
    String createConversation(String username, Long agentId, String firstMessage);

    /**
     * 创建新会话并生成标题
     */
    AgentSessionCreateRespDTO createConversationWithTitle(String username, Long agentId, String firstMessage);

    /**
     * 分页查询用户会话列表
     */
    IPage<AgentConversationRespDTO> pageConversations(String username, AgentConversationPageReqDTO reqDTO);

    /**
     * 更新会话信息
     */
    void updateConversation(String sessionId, Integer messageCount, Integer totalTokens);

    /**
     * 结束会话
     */
    void endConversation(String sessionId);
}