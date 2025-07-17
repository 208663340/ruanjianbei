package com.hewei.hzyjy.xunzhi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.config.sse.SseConfig;
import com.hewei.hzyjy.xunzhi.dao.entity.AiMessage;
// 移除AgentMessage导入，不再需要
import com.hewei.hzyjy.xunzhi.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.dao.repository.AiMessageRepository;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiMessageReqDTO;
// 移除AgentMessageHistoryRespDTO导入，不再需要
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.service.AiConversationService;
import com.hewei.hzyjy.xunzhi.service.AiMessageService;
import com.hewei.hzyjy.xunzhi.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.service.UserService;
// 移除Redis缓存服务导入
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.RoleContent;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.SparkAIClient;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hewei.hzyjy.xunzhi.common.constant.RedisCacheConstant.USER_LOGIN_KEY;

/**
 * AI消息Service实现类
 * @author nageoffer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageServiceImpl implements AiMessageService {
    
    private final AiMessageRepository aiMessageRepository;
    private final AiPropertiesService aiPropertiesService;
    private final AiConversationService aiConversationService;
    // 移除Redis缓存服务注入
    private final UserService userService;
    private final SparkAIClient sparkAIClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final SseConfig sseConfig;
    private final SimpleAsyncTaskExecutor asyncTask = new SimpleAsyncTaskExecutor();
    @Override
    public SseEmitter aiChatSse(AiMessageReqDTO requestParam) {
        String sessionId = requestParam.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            throw new IllegalArgumentException("sessionId不能为空");
        }

        SseEmitter emitter = new SseEmitter(sseConfig.getTimeout());
        String userName = requestParam.getUserName() == null ? "默认" : requestParam.getUserName();
        Long aiId = requestParam.getAiId();
        String userMessage = requestParam.getInputMessage();
        
        // 内容累积器
        AIContentAccumulator accumulator = new AIContentAccumulator();
        
        asyncTask.submit(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 1. 验证AI配置
                AiPropertiesDO aiProperties = aiPropertiesService.getById(aiId);
                if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() == 0) {
                    throw new ClientException("AI配置不存在或已禁用");
                }
                
                // 2. 处理历史消息
                List<AiMessageHistoryRespDTO> historyMessages = getConversationHistory(sessionId);
                List<RoleContent> historyList = new ArrayList<>();
                
                if (CollUtil.isNotEmpty(historyMessages)) {
                    historyList = historyMessages.stream().map(h -> {
                        String role = h.getMessageType() == 1 ? "user" : "assistant";
                        return new RoleContent(role, h.getMessageContent());
                    }).collect(Collectors.toList());
                }
                // 统一转换为JSON字符串，确保不为null
                String historyJson = JSON.toJSONString(historyList);

                int nextMessageSeq = getNextMessageSeq(sessionId);
                
                // 3. 保存用户消息到数据库
                AiMessage userMsg = new AiMessage();
                userMsg.setSessionId(sessionId);
                userMsg.setMessageType(1); // 用户消息
                userMsg.setMessageContent(userMessage);
                userMsg.setMessageSeq(nextMessageSeq);
                userMsg.setCreateTime(new Date());
                userMsg.setDelFlag(0);
                // 直接保存到数据库
                aiMessageRepository.save(userMsg);
                
                // 4. 调用AI接口进行流式传输
                String aiResponse = "";
                
                if ("generalv3.5".equals(aiProperties.getAiType())) {
                    // 使用星火AI
                    sparkAIClient.chatStream(
                            userMessage,
                            historyJson,
                            true,
                            new OutputStream() {
                                @Override
                                public void write(int b) { /* 不需要实现 */ }

                                @Override
                                public void write(byte[] b, int off, int len) throws IOException {
                                    try {
                                        // 发送数据到前端
                                        String jsonChunk = new String(b, off, len);
                                        emitter.send(SseEmitter.event().data(jsonChunk));

                                        // 累积内容到字符串
                                        accumulator.appendChunk(b);
                                        
                                        // 发送心跳保持连接活跃
                                        if (sseConfig.getEnableHeartbeat() && System.currentTimeMillis() % sseConfig.getHeartbeatInterval() < 100) {
                                            emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                                        }
                                    } catch (IOException e) {
                                        log.error("SSE数据发送失败", e);
                                        throw e;
                                    }
                                }

                                @Override
                                public void flush() { /* 确保数据发送 */ }
                            },data -> {
                            },
                            aiProperties.getApiKey(),
                            aiProperties.getAiType()
                    );

                } else {
                    // 其他AI类型的处理逻辑
                    aiResponse = "暂不支持该AI类型";
                    emitter.send(SseEmitter.event().data(aiResponse));
                    accumulator.appendChunk(aiResponse.getBytes());
                }
                
                // 5. 保存AI回复消息
                String fullContent = accumulator.getFullContent();
                long responseTime = System.currentTimeMillis() - startTime;
                
                AiMessage aiMsg = new AiMessage();
                aiMsg.setSessionId(sessionId);
                aiMsg.setMessageType(2); // AI回复
                aiMsg.setMessageContent(fullContent);
                aiMsg.setMessageSeq(getNextMessageSeq(sessionId));
                aiMsg.setResponseTime((int) responseTime);
                aiMsg.setCreateTime(new Date());
                aiMsg.setDelFlag(0);
                aiMessageRepository.save(aiMsg);
                
                // 6. 更新会话信息
                aiConversationService.updateConversation(sessionId, aiMsg.getMessageSeq(), null);
                
                // 7. 发送完成信号
                emitter.send(SseEmitter.event().name("end").data("[DONE]"));
                emitter.complete();
                
            } catch (Exception e) {
                log.error("AI聊天处理异常", e);
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                // 保存错误消息
                AiMessage errorMsg = new AiMessage();
                errorMsg.setSessionId(sessionId);
                errorMsg.setMessageType(2); // AI回复
                errorMsg.setMessageContent("抱歉，处理您的请求时出现了错误");
                errorMsg.setMessageSeq(getNextMessageSeq(sessionId));
                errorMsg.setResponseTime((int) responseTime);
                errorMsg.setErrorMessage(e.getMessage());
                errorMsg.setCreateTime(new Date());
                errorMsg.setDelFlag(0);
                aiMessageRepository.save(errorMsg);

                try {
                    emitter.send(SseEmitter.event().data("抱歉，处理您的请求时出现了错误"));
                } catch (IOException ioException) {
                    log.error("发送错误消息失败", ioException);
                }
                
                emitter.completeWithError(e);
            }
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时，sessionId: {}", sessionId);
            try {
                emitter.send(SseEmitter.event().name("timeout").data("连接超时"));
            } catch (IOException e) {
                log.error("发送超时消息失败", e);
            }
            emitter.complete();
        });
        
        emitter.onError(throwable -> {
            log.error("SSE连接异常，sessionId: {}", sessionId, throwable);
            try {
                emitter.send(SseEmitter.event().name("error").data("连接异常: " + throwable.getMessage()));
            } catch (IOException e) {
                log.error("发送错误消息失败", e);
            }
            emitter.completeWithError(throwable);
        });
        
        return emitter;
    }
    
    @Override
    public List<AiMessageHistoryRespDTO> getConversationHistory(String sessionId) {
        // 直接从数据库查询历史消息
        List<AiMessage> messages = aiMessageRepository
                .findBySessionIdAndDelFlagOrderByMessageSeqAsc(sessionId, 0);
        
        return messages.stream()
                .map(message -> {
                    AiMessageHistoryRespDTO dto = new AiMessageHistoryRespDTO();
                    BeanUtils.copyProperties(message, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public IPage<AiMessageHistoryRespDTO> pageHistoryMessages(String username, String sessionId, Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        org.springframework.data.domain.Page<AiMessage> messagePage;
        
        if (StrUtil.isNotBlank(sessionId)) {
            messagePage = aiMessageRepository
                    .findBySessionIdAndDelFlagOrderByCreateTimeAsc(sessionId, 0, pageable);
        } else {
            messagePage = aiMessageRepository
                    .findByDelFlagOrderByCreateTimeDesc(0, pageable);
        }
        
        // 转换为MyBatis-Plus的IPage格式
        Page<AiMessageHistoryRespDTO> resultPage = new Page<>(current, size);
        resultPage.setTotal(messagePage.getTotalElements());
        resultPage.setRecords(
                messagePage.getContent().stream()
                        .map(message -> {
                            AiMessageHistoryRespDTO respDTO = new AiMessageHistoryRespDTO();
                            BeanUtils.copyProperties(message, respDTO);
                            return respDTO;
                        })
                        .collect(Collectors.toList())
        );
        
        return resultPage;
    }
    
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
        com.alibaba.fastjson2.JSONObject userInfo = JSON.parseObject(userInfoJson);
        return userInfo.getLong("id");
    }
    
    /**
     * 获取下一个消息序号
     */
    private Integer getNextMessageSeq(String sessionId) {
        AiMessage lastMessage = aiMessageRepository.findTopBySessionIdAndDelFlagOrderByMessageSeqDesc(sessionId, 0);
        if (lastMessage == null) {
            return 1;
        }
        return lastMessage.getMessageSeq() + 1;
    }
}