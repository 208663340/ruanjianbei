package com.hewei.hzyjy.xunzhi.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.dao.entity.AgentConversation;
import com.hewei.hzyjy.xunzhi.dao.repository.AgentConversationRepository;
import com.hewei.hzyjy.xunzhi.dto.req.agent.AgentConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentConversationRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.service.AgentConversationService;
import com.hewei.hzyjy.xunzhi.common.util.SaTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * 智能体会话服务实现类
 */
@Service
@RequiredArgsConstructor
public class AgentConversationServiceImpl implements AgentConversationService {

    private final AgentConversationRepository agentConversationRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final SaTokenUtil saTokenUtil;

    @Override
    public String createConversation(String username, Long agentId, String firstMessage) {
        // 通过用户名获取用户ID
        Long userId = getUserIdByUsername(username);
        
        String sessionId = IdUtil.getSnowflakeNextIdStr();
        
        AgentConversation conversation = new AgentConversation();
        conversation.setSessionId(sessionId);
        conversation.setUserId(userId);
        conversation.setAgentId(agentId);
        conversation.setConversationTitle(generateTitle(firstMessage));
        conversation.setMessageCount(0);
        conversation.setTotalTokens(0);
        conversation.setStatus(1); // 进行中
        conversation.setDelFlag(0);
        
        agentConversationRepository.save(conversation);
        return sessionId;
    }

    @Override
    public AgentSessionCreateRespDTO createConversationWithTitle(String username, Long agentId, String firstMessage) {
        String sessionId = createConversation(username, agentId, firstMessage);
        String title = generateTitle(firstMessage);
        return new AgentSessionCreateRespDTO(sessionId, title);
    }

    @Override
    public IPage<AgentConversationRespDTO> pageConversations(String username, AgentConversationPageReqDTO reqDTO) {
        // 通过用户名获取用户ID
        Long userId = getUserIdByUsername(username);
        
        Pageable pageable = PageRequest.of(reqDTO.getCurrent() - 1, reqDTO.getSize());
        org.springframework.data.domain.Page<AgentConversation> conversationPage;
        
        if (reqDTO.getKeyword() != null && !reqDTO.getKeyword().trim().isEmpty()) {
            // 有关键词搜索
            conversationPage = agentConversationRepository
                .findByUserIdAndAgentIdAndStatusAndDelFlagAndTitleContaining(
                    userId, reqDTO.getAgentId(), reqDTO.getStatus(), 0, reqDTO.getKeyword(), pageable);
        } else {
            // 无关键词搜索
            conversationPage = agentConversationRepository
                .findByUserIdAndAgentIdAndStatusAndDelFlagOrderByUpdateTimeDesc(
                    userId, reqDTO.getAgentId(), reqDTO.getStatus(), 0, pageable);
        }
        
        // 转换为MyBatis-Plus的IPage格式以保持接口兼容性
        Page<AgentConversationRespDTO> resultPage = new Page<>(reqDTO.getCurrent(), reqDTO.getSize());
        resultPage.setTotal(conversationPage.getTotalElements());
        resultPage.setRecords(
            conversationPage.getContent().stream()
                .map(conversation -> {
                    AgentConversationRespDTO respDTO = new AgentConversationRespDTO();
                    BeanUtils.copyProperties(conversation, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList())
        );
        
        return resultPage;
    }

    @Override
    public void updateConversation(String sessionId, Integer messageCount, Integer totalTokens) {
        agentConversationRepository.findBySessionIdAndDelFlag(sessionId, 0)
            .ifPresent(conversation -> {
                conversation.setMessageCount(messageCount);
                conversation.setTotalTokens(totalTokens);
                agentConversationRepository.save(conversation);
            });
    }

    @Override
    public void endConversation(String sessionId) {
        agentConversationRepository.findBySessionIdAndDelFlag(sessionId, 0)
            .ifPresent(conversation -> {
                conversation.setStatus(2); // 已结束
                agentConversationRepository.save(conversation);
            });
    }

    /**
     * 根据首条消息生成会话标题
     */
    private String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.trim().isEmpty()) {
            return "新对话";
        }
        
        String title = firstMessage.trim();
        if (title.length() > 20) {
            title = title.substring(0, 20) + "...";
        }
        
        return title;
    }

    /**
     * 通过用户名获取用户ID的通用方法
     */
    private Long getUserIdByUsername(String username) {
        return saTokenUtil.getUserIdByUsername(username);
    }
}