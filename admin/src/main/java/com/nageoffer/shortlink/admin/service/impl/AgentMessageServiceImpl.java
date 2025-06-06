package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.dao.entity.AgentMessage;
import com.nageoffer.shortlink.admin.dao.entity.AgentPropertiesDO;
import com.nageoffer.shortlink.admin.dao.repository.AgentMessageRepository;
import com.nageoffer.shortlink.admin.dto.req.user.UserMessageReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.agent.AgentMessageHistoryRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.agent.AgentMessageRespDTO;

import com.nageoffer.shortlink.admin.service.AgentConversationService;
import com.nageoffer.shortlink.admin.service.AgentMessageService;

import com.nageoffer.shortlink.admin.service.tool.RedisSessionCacheService;
import com.nageoffer.shortlink.admin.toolkit.ai.AIContentAccumulator;
import com.nageoffer.shortlink.admin.toolkit.ai.AgentPropertiesLoader;
import com.nageoffer.shortlink.admin.toolkit.ai.XingChenAIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;

/**
* @author 20866
* @description 针对表【agent_message_0】的数据库操作Service实现
* @createDate 2025-05-27 13:33:37
*/
@Service
@RequiredArgsConstructor
public class AgentMessageServiceImpl implements AgentMessageService{

    private final AgentMessageRepository agentMessageRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final XingChenAIClient xingChenAIClient;
    private final AgentPropertiesLoader agentPropertiesLoader;
    private final AgentConversationService agentConversationService;
    private final RedisSessionCacheService redisSessionCacheService;
    private final SimpleAsyncTaskExecutor asyncTask = new SimpleAsyncTaskExecutor();



    /**
     * 通过用户名获取用户ID的通用方法
     */
    @Override
    public Long getUserIdByUsername(String username) {
        // 根据用户名获取登录信息
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY + username);
        if (CollUtil.isEmpty(hasLoginMap)) {
            throw new ClientException("用户未登录或登录已过期");
        }
        
        // 续期登录状态
        stringRedisTemplate.expire(USER_LOGIN_KEY + username, 30L, TimeUnit.MINUTES);
        
        // 获取token对应的用户信息JSON字符串
        String token = hasLoginMap.keySet().stream()
                .findFirst()
                .map(Object::toString)
                .orElseThrow(() -> new ClientException("用户登录错误"));
        
        String userInfoJson = (String) hasLoginMap.get(token);
        if (userInfoJson == null) {
            throw new ClientException("用户信息不存在");
        }
        
        // 解析用户信息，获取userId
        com.alibaba.fastjson2.JSONObject userInfo = com.alibaba.fastjson2.JSON.parseObject(userInfoJson);
        return userInfo.getLong("id");
    }

    @Override
    public void saveMessage(AgentMessageRespDTO agentMessageResp) {
        AgentMessage agentMessage = new AgentMessage();
        BeanUtils.copyProperties(agentMessageResp, agentMessage);

        agentMessageRepository.save(agentMessage);
    }

    @Override
    public List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId) {
        // 先尝试从Redis缓存中获取
        List<AgentMessageHistoryRespDTO> cachedMessages = redisSessionCacheService.getMessagesFromCache(sessionId);
        if (CollUtil.isNotEmpty(cachedMessages)) {
            return cachedMessages;
        }
        
        // 缓存中没有则从MongoDB查询
        List<AgentMessage> messages = agentMessageRepository
            .findBySessionIdAndDelFlagOrderByMessageSeqAsc(sessionId, 0);
        
        return messages.stream()
            .map(message -> {
                AgentMessageHistoryRespDTO dto = new AgentMessageHistoryRespDTO();
                BeanUtils.copyProperties(message, dto);
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(String username, String sessionId, Integer current, Integer size) {
        // 通过用户名获取用户ID
        Long userId = getUserIdByUsername(username);
        
        Pageable pageable = PageRequest.of(current - 1, size);
        org.springframework.data.domain.Page<AgentMessage> messagePage;
        
        if (StrUtil.isNotBlank(sessionId)) {
            messagePage = agentMessageRepository
                .findBySessionIdAndDelFlagOrderByCreateTimeAsc(sessionId, 0, pageable);
        } else {
            messagePage = agentMessageRepository
                .findByDelFlagOrderByCreateTimeDesc(0, pageable);
        }
        
        // 转换为MyBatis-Plus的IPage格式以保持接口兼容性
        Page<AgentMessageHistoryRespDTO> resultPage = new Page<>(current, size);
        resultPage.setTotal(messagePage.getTotalElements());
        resultPage.setRecords(
            messagePage.getContent().stream()
                .map(message -> {
                    AgentMessageHistoryRespDTO respDTO = new AgentMessageHistoryRespDTO();
                    BeanUtils.copyProperties(message, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList())
        );
        
        return resultPage;
    }

    /**
     * 保存用户消息
     */
    private void saveUserMessage(String sessionId, Long userId, Long agentId, String userMessage) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(sessionId);
        message.setMessageType(1); // 用户消息
        message.setMessageContent(userMessage);
        message.setMessageSeq(getNextMessageSeq(sessionId));
        message.setDelFlag(0);
        
        agentMessageRepository.save(message);
    }

    /**
     * 保存AI回复消息
     */
    private void saveAiMessage(String sessionId, Long userId, Long agentId, String aiMessage, Integer responseTime) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(sessionId);
        message.setMessageType(2); // AI回复
        message.setMessageContent(aiMessage);
        message.setMessageSeq(getNextMessageSeq(sessionId));
        message.setResponseTime(responseTime);
        message.setDelFlag(0);
        
        agentMessageRepository.save(message);
    }

    /**
     * 获取下一个消息序号
     */
    private Integer getNextMessageSeq(String sessionId) {
        return redisSessionCacheService.getNextMessageSeq(sessionId);
    }

    @Override
    public SseEmitter agentChatSse(UserMessageReqDTO requestParam) {
        // sessionId现在必须通过参数传入
        String sessionId = requestParam.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            throw new IllegalArgumentException("sessionId不能为空");
        }
        
        SseEmitter emitter = new SseEmitter(18000L);
        String userName = requestParam.getUserName() == null ? "默认" : requestParam.getUserName();
        Long agentId = requestParam.getAgentId() == null ? 1345345L : requestParam.getAgentId();
        String userMessage = requestParam.getInputMessage() == null ? "没有输入" : requestParam.getInputMessage();
        
        // 新增内容累积器
        AIContentAccumulator accumulator = new AIContentAccumulator();
        
        asyncTask.submit(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 1. 处理历史消息
                List<AgentMessageHistoryRespDTO> historyMessages = getConversationHistory(sessionId);
                // 将 historyMessages 转换为 XingChenAIClient 需要的 JSON 格式
                List<java.util.HashMap<String, String>> historyList = new ArrayList<>();
                if (CollUtil.isNotEmpty(historyMessages)) {
                    historyList = historyMessages.stream().map(h -> {
                        java.util.HashMap<String, String> map = new java.util.HashMap<>();
                        map.put("role", h.getMessageType() == 1 ? "user" : "assistant");
                        map.put("content_type", "text");
                        map.put("content", h.getMessageContent());
                        return map;
                    }).collect(Collectors.toList());
                }
                // 统一转换为JSON字符串，确保不为null
                String historyJson = JSON.toJSONString(historyList);
                int nextMessageSeq = getNextMessageSeq(sessionId);

                // 2. 保存用户消息到Redis缓存
                AgentMessage userMsg = new AgentMessage();
                userMsg.setSessionId(sessionId);
                userMsg.setMessageType(1); // 用户消息
                userMsg.setMessageContent(userMessage);
                userMsg.setMessageSeq(nextMessageSeq);
                userMsg.setCreateTime(new Date());
                userMsg.setDelFlag(0);
                // 添加到Redis缓存，会自动异步同步到数据库
                redisSessionCacheService.addMessageToCache(sessionId, userMsg);
                
                // 3. 根据智能体ID获取对应的配置
                AgentPropertiesDO agentProperties = agentPropertiesLoader.getAgentPropertiesMap().get(agentId);
                
                if (agentProperties == null) {
                    throw new ClientException("智能体配置不存在");
                }
                
                // 4. 调用AI接口进行流式传输
                xingChenAIClient.chat(
                        userMessage,
                        sessionId,
                        historyJson,    // 传递 history
                        true,
                        new OutputStream() {
                            @Override
                            public void write(int b) { /* 不需要实现 */ }

                            @Override
                            public void write(byte[] b, int off, int len) throws IOException {
                                // 发送数据到前端
                                String jsonChunk = new String(b, off, len);
                                emitter.send(SseEmitter.event().data(jsonChunk));

                                // 累积内容到字符串
                                accumulator.appendChunk(b);
                            }

                            @Override
                            public void flush() { /* 确保数据发送 */ }
                        },data -> {
                        },
                        agentProperties.getApiKey(),
                        agentProperties.getApiSecret(),
                        agentProperties.getApiFlowId()
                );

                // 5. 流式传输完成后，保存AI回复消息到Redis缓存
                String fullContent = accumulator.getFullContent();
                long responseTime = System.currentTimeMillis() - startTime;
                
                AgentMessage aiMsg = new AgentMessage();
                aiMsg.setSessionId(sessionId); // sessionId 已经在前面被赋值为 currentSessionId
                aiMsg.setMessageType(2); // AI回复
                aiMsg.setMessageContent(fullContent);
                // 如果是新会话的第一次AI回复，序号应为2，否则通过 getNextMessageSeq 获取
                int aiMessageSeq = (nextMessageSeq == 1) ? 2 : getNextMessageSeq(sessionId);
                aiMsg.setMessageSeq(aiMessageSeq);
                aiMsg.setResponseTime((int) responseTime);
                aiMsg.setCreateTime(new Date());
                aiMsg.setDelFlag(0);
                // 添加到Redis缓存，会自动异步同步到数据库
                redisSessionCacheService.addMessageToCache(sessionId, aiMsg);
                
                // 6. 更新会话信息
                agentConversationService.updateConversation(sessionId, aiMessageSeq, null); // 更新消息总数

                // 7. 发送完成信号
                emitter.send(SseEmitter.event().name("end").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                // 保存错误消息到Redis缓存
                if (sessionId != null) {
                    AgentMessage errorMsg = new AgentMessage();
                    errorMsg.setSessionId(sessionId);
                    errorMsg.setMessageType(2); // AI回复
                    errorMsg.setMessageContent("抱歉，处理您的请求时出现了错误");
                    errorMsg.setMessageSeq(getNextMessageSeq(sessionId));
                    errorMsg.setResponseTime((int) responseTime);
                    errorMsg.setErrorMessage(e.getMessage());
                    errorMsg.setCreateTime(new Date());
                    errorMsg.setDelFlag(0);
                    // 添加到Redis缓存，会自动异步同步到数据库
                    redisSessionCacheService.addMessageToCache(sessionId, errorMsg);
                }
                
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            emitter.complete();
        });

        return emitter;
    }
}





