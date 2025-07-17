package com.hewei.hzyjy.xunzhi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.dao.entity.AiConversation;
import com.hewei.hzyjy.xunzhi.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.dao.repository.AiConversationRepository;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiConversationRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.service.AiConversationService;
import com.hewei.hzyjy.xunzhi.service.AiPropertiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI会话Service实现类
 * @author nageoffer
 */
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl implements AiConversationService {
    
    private final AiConversationRepository aiConversationRepository;
    private final AiPropertiesService aiPropertiesService;
    
    @Override
    public String createConversation(String username, Long aiId, String firstMessage) {
        // 验证AI配置是否存在且启用
        AiPropertiesDO aiProperties = aiPropertiesService.getById(aiId);
        if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() == 0) {
            throw new ClientException("AI配置不存在或已禁用");
        }
        
        // 生成会话ID
        String sessionId = IdUtil.getSnowflakeNextIdStr();
        
        // 生成会话标题（取用户消息的前20个字符）
        String title = generateTitle(firstMessage);
        
        // 创建会话记录
        AiConversation conversation = new AiConversation();
        conversation.setSessionId(sessionId);
        conversation.setUsername(username);
        conversation.setAiId(aiId);
        conversation.setTitle(title);
        conversation.setStatus(1); // 进行中
        conversation.setMessageCount(0);
        conversation.setLastMessageTime(new Date());
        conversation.setCreateTime(new Date());
        conversation.setUpdateTime(new Date());
        conversation.setDelFlag(0);
        
        aiConversationRepository.save(conversation);
        
        return sessionId;
    }
    
    @Override
    public AiSessionCreateRespDTO createConversationWithTitle(String username, Long aiId, String firstMessage) {
        // 验证AI配置是否存在且启用
        AiPropertiesDO aiProperties = aiPropertiesService.getById(aiId);
        if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() == 0) {
            throw new ClientException("AI配置不存在或已禁用");
        }
        
        // 生成会话ID
        String sessionId = IdUtil.getSnowflakeNextIdStr();
        
        // 生成会话标题（取用户消息的前20个字符）
        String title = generateTitle(firstMessage);
        
        // 创建会话记录
        AiConversation conversation = new AiConversation();
        conversation.setSessionId(sessionId);
        conversation.setUsername(username);
        conversation.setAiId(aiId);
        conversation.setTitle(title);
        conversation.setStatus(1); // 进行中
        conversation.setMessageCount(0);
        conversation.setLastMessageTime(new Date());
        conversation.setCreateTime(new Date());
        conversation.setUpdateTime(new Date());
        conversation.setDelFlag(0);
        
        aiConversationRepository.save(conversation);
        
        // 返回结果
        AiSessionCreateRespDTO respDTO = new AiSessionCreateRespDTO();
        respDTO.setSessionId(sessionId);
        respDTO.setConversationTitle(title);
        
        return respDTO;
    }
    
    @Override
    public IPage<AiConversationRespDTO> pageConversations(String username, AiConversationPageReqDTO requestParam) {
        Pageable pageable = PageRequest.of(requestParam.getCurrent() - 1, requestParam.getSize());
        
        org.springframework.data.domain.Page<AiConversation> conversationPage;
        
        if (requestParam.getAiId() != null) {
            conversationPage = aiConversationRepository
                    .findByUsernameAndAiIdAndDelFlagOrderByCreateTimeDesc(username, requestParam.getAiId(), 0, pageable);
        } else {
            conversationPage = aiConversationRepository
                    .findByUsernameAndDelFlagOrderByCreateTimeDesc(username, 0, pageable);
        }
        
        // 转换为MyBatis-Plus的IPage格式
        Page<AiConversationRespDTO> resultPage = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        resultPage.setTotal(conversationPage.getTotalElements());
        
        List<AiConversationRespDTO> records = conversationPage.getContent().stream()
                .map(conversation -> {
                    AiConversationRespDTO respDTO = new AiConversationRespDTO();
                    BeanUtil.copyProperties(conversation, respDTO);
                    
                    // 获取AI名称
                    AiPropertiesDO aiProperties = aiPropertiesService.getById(conversation.getAiId());
                    if (aiProperties != null) {
                        respDTO.setAiName(aiProperties.getAiName());
                    }
                    
                    return respDTO;
                })
                .collect(Collectors.toList());
        
        resultPage.setRecords(records);
        return resultPage;
    }
    
    @Override
    public void updateConversation(String sessionId, Integer messageSeq, String title) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (!conversationOpt.isPresent()) {
            throw new ClientException("会话不存在");
        }
        
        AiConversation conversation = conversationOpt.get();
        
        if (messageSeq != null) {
            conversation.setMessageCount(messageSeq);
        }
        
        if (StrUtil.isNotBlank(title)) {
            conversation.setTitle(title);
        }
        
        conversation.setLastMessageTime(new Date());
        conversation.setUpdateTime(new Date());
        
        aiConversationRepository.save(conversation);
    }
    
    @Override
    public void endConversation(String sessionId) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (!conversationOpt.isPresent()) {
            throw new ClientException("会话不存在");
        }
        
        AiConversation conversation = conversationOpt.get();
        conversation.setStatus(2); // 已结束
        conversation.setUpdateTime(new Date());
        
        aiConversationRepository.save(conversation);
    }
    
    @Override
    public void deleteConversation(String sessionId) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (!conversationOpt.isPresent()) {
            throw new ClientException("会话不存在");
        }
        
        AiConversation conversation = conversationOpt.get();
        conversation.setDelFlag(1);
        conversation.setUpdateTime(new Date());
        
        aiConversationRepository.save(conversation);
    }
    
    @Override
    public AiConversationRespDTO getConversationBySessionId(String sessionId) {
        Optional<AiConversation> conversationOpt = aiConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (!conversationOpt.isPresent()) {
            throw new ClientException("会话不存在");
        }
        
        AiConversation conversation = conversationOpt.get();
        AiConversationRespDTO respDTO = new AiConversationRespDTO();
        BeanUtil.copyProperties(conversation, respDTO);
        
        // 获取AI名称
        AiPropertiesDO aiProperties = aiPropertiesService.getById(conversation.getAiId());
        if (aiProperties != null) {
            respDTO.setAiName(aiProperties.getAiName());
        }
        
        return respDTO;
    }
    
    /**
     * 生成会话标题
     */
    private String generateTitle(String firstMessage) {
        if (StrUtil.isBlank(firstMessage)) {
            return "新会话";
        }
        
        // 取前20个字符作为标题
        if (firstMessage.length() <= 20) {
            return firstMessage;
        }
        
        return firstMessage.substring(0, 20) + "...";
    }
}