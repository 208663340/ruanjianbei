package com.hewei.hzyjy.xunzhi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.enums.AgentErrorCodeEnum;
import com.hewei.hzyjy.xunzhi.dao.entity.AgentMessage;
import com.hewei.hzyjy.xunzhi.dao.entity.AgentPropertiesDO;
import com.hewei.hzyjy.xunzhi.dao.repository.AgentMessageRepository;

import com.hewei.hzyjy.xunzhi.dto.req.agent.DemeanorEvaluationReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.agent.InterviewQuestionReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.agent.InterviewAnswerReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.user.UserMessageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.InterviewAnswerRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.InterviewQuestionRespDTO;
import com.hewei.hzyjy.xunzhi.service.*;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.XingChenAIClient;

import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AgentPropertiesLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
* @author 20866
* @description 针对表【agent_message_0】的数据库操作Service实现
* @createDate 2025-05-27 13:33:37
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentMessageServiceImpl implements AgentMessageService{

    private final AgentMessageRepository agentMessageRepository;
    private final XingChenAIClient xingChenAIClient;
    private final AgentPropertiesLoader agentPropertiesLoader;
    private final AgentConversationService agentConversationService;
    private final InterviewQuestionService interviewQuestionService;
    private final InterviewQuestionCacheService interviewQuestionCacheService;
    private final AudioTranscriptionService audioTranscriptionService;
    private final SimpleAsyncTaskExecutor asyncTask = new SimpleAsyncTaskExecutor();



    @Override
    public List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId) {
        // 直接从数据库查询历史消息
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
     * 获取下一个消息序号
     */
    private Integer getNextMessageSeq(String sessionId) {
        AgentMessage lastMessage = agentMessageRepository
            .findTopBySessionIdAndDelFlagOrderByMessageSeqDesc(sessionId, 0);
        if (lastMessage == null) {
            return 1;
        }
        return lastMessage.getMessageSeq() + 1;
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
                List<HashMap<String, String>> historyList = new ArrayList<>();
                if (CollUtil.isNotEmpty(historyMessages)) {
                    historyList = historyMessages.stream().map(h -> {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("role", h.getMessageType() == 1 ? "user" : "assistant");
                        map.put("content_type", "text");
                        map.put("content", h.getMessageContent());
                        return map;
                    }).collect(Collectors.toList());
                }
                // 统一转换为JSON字符串，确保不为null
                String historyJson = JSON.toJSONString(historyList);
                int nextMessageSeq = getNextMessageSeq(sessionId);

                // 2. 保存用户消息到数据库
                AgentMessage userMsg = new AgentMessage();
                userMsg.setSessionId(sessionId);
                userMsg.setMessageType(1); // 用户消息
                userMsg.setMessageContent(userMessage);
                userMsg.setMessageSeq(nextMessageSeq);
                userMsg.setCreateTime(new Date());
                userMsg.setDelFlag(0);
                // 直接保存到数据库
                agentMessageRepository.save(userMsg);
                
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

                // 5. 流式传输完成后，保存AI回复消息到数据库
                String fullContent = accumulator.getFullContent();
                long responseTime = System.currentTimeMillis() - startTime;
                
                AgentMessage aiMsg = new AgentMessage();
                aiMsg.setSessionId(sessionId);
                aiMsg.setMessageType(2); // AI回复
                aiMsg.setMessageContent(fullContent);
                // 如果是新会话的第一次AI回复，序号应为2，否则通过 getNextMessageSeq 获取
                int aiMessageSeq = (nextMessageSeq == 1) ? 2 : getNextMessageSeq(sessionId);
                aiMsg.setMessageSeq(aiMessageSeq);
                aiMsg.setResponseTime((int) responseTime);
                aiMsg.setCreateTime(new Date());
                aiMsg.setDelFlag(0);
                // 直接保存到数据库
                agentMessageRepository.save(aiMsg);
                
                // 6. 更新会话信息
                agentConversationService.updateConversation(sessionId, aiMessageSeq, null); // 更新消息总数

                // 7. 发送完成信号
                emitter.send(SseEmitter.event().name("end").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                // 保存错误消息到数据库
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
                    // 直接保存到数据库
                    agentMessageRepository.save(errorMsg);
                }
                
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            emitter.complete();
        });

        return emitter;
    }
    
    @Override
    public InterviewQuestionRespDTO extractInterviewQuestions(InterviewQuestionReqDTO reqDTO) {
        InterviewQuestionRespDTO response = new InterviewQuestionRespDTO();
        response.setSessionId(reqDTO.getSessionId());
        response.setUserName(reqDTO.getUserName());
        response.setAgentId(reqDTO.getAgentId());
        response.setIsSuccess(0); // 默认失败

        Long agentId = reqDTO.getAgentId() == null ? 1345345L : reqDTO.getAgentId();

        // 新增内容累积器用于收集AI响应
        AIContentAccumulator accumulator = new AIContentAccumulator();
        long startTime = System.currentTimeMillis();
        // 同步处理面试题抽取
        try {
            // 1. 上传文件到讯飞服务器获取URL
            String fileUrl = null;
            if (reqDTO.getResumePdf() != null && !reqDTO.getResumePdf().isEmpty()) {
                try {
                    // 获取智能体配置中的API密钥
                    AgentPropertiesDO agentProperties = agentPropertiesLoader.getAgentPropertiesMap().get(agentId);
                    if (agentProperties == null) {
                        response.setErrorMessage("智能体配置不存在");
                        return response;
                    }

                    // 上传文件到讯飞服务器获取URL
                    fileUrl = xingChenAIClient.uploadFile(
                        reqDTO.getResumePdf(),
                        agentProperties.getApiKey(),
                        agentProperties.getApiSecret()
                    );
                    log.info("文件上传成功，URL: {}", fileUrl);
                } catch (Exception e) {
                    log.error("文件上传失败: {}", e.getMessage());
                    response.setErrorMessage("文件上传失败，请重试");
                    return response;
                }
            }

            // 2. 构建面试题抽取的提示词
            String promptBuilder = "帮我抽取一些面试题";
            // 检查文件URL
            if (fileUrl == null) {
                response.setErrorMessage("简历不存在");
                return response;
            }

            // 3. 调用AI接口进行同步处理
            AgentPropertiesDO agentProperties = agentPropertiesLoader.getAgentPropertiesMap().get(agentId);
            if (agentProperties == null) {
                response.setErrorMessage("智能体配置不存在");
                return response;
            }
            // 4. 使用现有的XingChenAIClient进行同步处理
            StringBuilder aiResponse = new StringBuilder();
            xingChenAIClient.chat(
                promptBuilder,
                reqDTO.getSessionId(),
                "{}", // 空的历史记录
                false, // 非流式
                new OutputStream() {
                    @Override
                    public void write(int b) { /* 不需要实现 */ }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        // 累积内容到字符串
                        aiResponse.append(new String(b, off, len));
                        accumulator.appendChunk(b);
                    }

                    @Override
                    public void flush() { /* 确保数据发送 */ }
                },
                data -> {},
                agentProperties.getApiKey(),
                agentProperties.getApiSecret(),
                agentProperties.getApiFlowId(),
                fileUrl // 传递文件URL
            );

            // 5. AI响应处理完成后，保存面试题数据到MongoDB
            String fullContent = accumulator.getFullContent();
            long responseTime = System.currentTimeMillis() - startTime;

            // 设置文件URL到请求DTO中
            reqDTO.setResumeFileUrl(fileUrl);

            // 保存面试题数据
            try {
                interviewQuestionService.createFromAIResponse(
                    reqDTO,
                    fullContent,
                    (int) responseTime,
                    null // tokenCount暂时为null，可以后续从AI响应中解析
                );
                log.info("面试题数据保存成功，会话ID: {}", reqDTO.getSessionId());

                // 设置响应基本信息
                response.setResumeFileUrl(fileUrl);
                response.setResponseTime((int) responseTime);

                // 6. 解析AI响应并缓存面试题和建议到Redis
                try {
                    log.info("开始解析AI响应，原始内容: {}", fullContent);

                    // 从AI响应中提取真正的content内容
                    String extractedContent = extractContentFromInterviewResponse(fullContent);
                    log.info("提取的content内容: {}", extractedContent);

                    if (StrUtil.isNotBlank(extractedContent)) {
                        // 尝试解析提取出的content中的面试题和建议
                        Map<String, Object> responseMap = JSON.parseObject(extractedContent, Map.class);
                        if (responseMap != null) {
                            log.info("成功解析响应Map，包含字段: {}", responseMap.keySet());

                            // 缓存面试题
                            if (responseMap.containsKey("questions")) {
                                Object questionsObj = responseMap.get("questions");
                                log.info("找到questions字段，类型: {}, 值: {}", questionsObj.getClass().getSimpleName(), questionsObj);
                                if (questionsObj instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<String> questions = (List<String>) questionsObj;
                                    if (!questions.isEmpty()) {
                                        // 缓存面试题到Redis
                                        interviewQuestionCacheService.cacheInterviewQuestions(reqDTO.getUserName(), questions);
                                        log.info("面试题缓存成功，用户: {}, 题目数量: {}", reqDTO.getUserName(), questions.size());

                                        // 从缓存获取Map格式的面试题并设置到响应对象
                                        Map<String, String> questionMap = interviewQuestionCacheService.getUserInterviewQuestions(reqDTO.getUserName());
                                        response.setQuestions(questionMap);
                                        response.setQuestionCount(questions.size());
                                    }
                                }
                            } else {
                                log.warn("响应中不包含questions字段");
                            }

                                // 缓存面试建议 - 支持sugest和suggestions两种字段名
                                List<String> suggestions = null;
                                if (responseMap.containsKey("sugest")) {
                                    Object sugestObj = responseMap.get("sugest");
                                    log.info("找到sugest字段，类型: {}, 值: {}", sugestObj.getClass().getSimpleName(), sugestObj);
                                    if (sugestObj instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<String> sugestList = (List<String>) sugestObj;
                                        suggestions = sugestList;
                                    }
                                } else if (responseMap.containsKey("suggestions")) {
                                    Object suggestionsObj = responseMap.get("suggestions");
                                    log.info("找到suggestions字段，类型: {}, 值: {}", suggestionsObj.getClass().getSimpleName(), suggestionsObj);
                                    if (suggestionsObj instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<String> suggestionsList = (List<String>) suggestionsObj;
                                        suggestions = suggestionsList;
                                    }
                                }

                                if (suggestions != null && !suggestions.isEmpty()) {
                                    // 缓存面试建议到Redis
                                    interviewQuestionCacheService.cacheInterviewSuggestions(reqDTO.getUserName(), suggestions);
                                    log.info("面试建议缓存成功，用户: {}, 建议数量: {}", reqDTO.getUserName(), suggestions.size());

                                    // 从缓存获取Map格式的面试建议并设置到响应对象
                                    Map<String, String> suggestionMap = interviewQuestionCacheService.getUserInterviewSuggestions(reqDTO.getUserName());
                                    response.setSuggestions(suggestionMap);
                                    response.setSuggestionCount(suggestions.size());
                                } else {
                                    log.warn("响应中不包含有效的面试建议字段");
                                }

                            // 缓存简历评分
                            if (responseMap.containsKey("score")) {
                                Object scoreObj = responseMap.get("score");
                                log.info("找到score字段，类型: {}, 值: {}", scoreObj.getClass().getSimpleName(), scoreObj);
                                 Integer resumeScore = null;

                                 if (scoreObj instanceof Number) {
                                     resumeScore = ((Number) scoreObj).intValue();
                                 } else if (scoreObj instanceof String) {
                                     try {
                                         resumeScore = Integer.parseInt((String) scoreObj);
                                     } catch (NumberFormatException e) {
                                          log.warn("无法解析简历评分字符串: {}", scoreObj);
                                      }
                                  }

                                     if (resumeScore != null && resumeScore >= 0 && resumeScore <= 100) {
                                         // 缓存简历评分到Redis
                                         interviewQuestionCacheService.cacheResumeScore(reqDTO.getUserName(), resumeScore);
                                         log.info("简历评分缓存成功，用户: {}, 评分: {}", reqDTO.getUserName(), resumeScore);

                                         // 设置到响应对象
                                         response.setResumeScore(resumeScore);
                                     } else {
                                         log.warn("简历评分超出范围或格式不正确: {}", resumeScore);
                                     }
                             } else {
                                 log.warn("响应中不包含score字段");
                             }

                            // 重置用户分数（只有在有面试题、建议或简历评分时才重置）
                            if (responseMap.containsKey("questions") || responseMap.containsKey("sugest") || responseMap.containsKey("suggestions") || responseMap.containsKey("score")) {
                                interviewQuestionCacheService.resetUserScore(reqDTO.getUserName());
                                log.info("用户分数已重置，用户: {}", reqDTO.getUserName());
                            }
                            } else {
                                log.warn("解析AI响应失败，responseMap为null");
                            }
                        } else {
                            log.warn("提取的content内容为空");
                        }
                    } catch (Exception cacheException) {
                        log.error("缓存面试题、建议和简历评分失败，用户: {}, 错误: {}", reqDTO.getUserName(), cacheException.getMessage());
                    }

                } catch (Exception e) {
                    log.error("面试题数据保存失败，会话ID: {}, 错误: {}", reqDTO.getSessionId(), e.getMessage());
                }

                // 设置成功状态
                response.setIsSuccess(1);
                log.info("面试题抽取完成，会话ID: {}", reqDTO.getSessionId());
                return response;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("面试题抽取处理异常: {}", e.getMessage(), e);

            // 保存错误记录到MongoDB
            try {
                reqDTO.setResumeFileUrl(null); // 错误情况下文件URL可能为空
                interviewQuestionService.createFromAIResponse(
                    reqDTO,
                    "{\"error\":\"" + e.getMessage() + "\"}",
                    (int) responseTime,
                    null
                );
            } catch (Exception saveException) {
                log.error("保存错误记录失败: {}", saveException.getMessage());
            }

            response.setErrorMessage("面试题抽取失败: " + e.getMessage());
            response.setIsSuccess(0);
            return response;
        }
    }
    
    @Override
    public InterviewAnswerRespDTO answerInterviewQuestion(String username, InterviewAnswerReqDTO requestParam) {
        InterviewAnswerRespDTO response = new InterviewAnswerRespDTO();
        response.setQuestionNumber(requestParam.getQuestionNumber());
        
        try {
            // 1. 从缓存中获取题目内容
            String questionContent = interviewQuestionCacheService.getQuestionByNumber(username, requestParam.getQuestionNumber());
            if (StrUtil.isBlank(questionContent)) {
                response.setErrorMessage("题目不存在或已过期，请重新抽取面试题");
                return response;
            }
            response.setQuestionContent(questionContent);
            
            // 2. 处理用户回答内容
            String answerContent = requestParam.getAnswerContent();
            
            // 如果提供了录音文件，则使用语音转文字
            if (requestParam.getAudioFile() != null && !requestParam.getAudioFile().isEmpty()) {
                try {
                    log.info("开始处理录音文件，用户: {}, 题号: {}, 文件大小: {} bytes", 
                            username, requestParam.getQuestionNumber(), requestParam.getAudioFile().getSize());
                    
                    // 调用讯飞语音转文字服务
                    String transcribedText = audioTranscriptionService.transcribeAudio(requestParam.getAudioFile());
                    
                    if (StrUtil.isNotBlank(transcribedText)) {
                        answerContent = transcribedText;
                        log.info("语音转文字成功，用户: {}, 题号: {}, 转换结果: {}", 
                                username, requestParam.getQuestionNumber(), transcribedText);
                    } else {
                        response.setErrorMessage("语音转文字失败，请检查录音文件格式或重新录制");
                        return response;
                    }
                } catch (Exception e) {
                    log.error("语音转文字处理失败，用户: {}, 题号: {}, 错误: {}", 
                            username, requestParam.getQuestionNumber(), e.getMessage(), e);
                    response.setErrorMessage("语音转文字处理失败: " + e.getMessage());
                    return response;
                }
            }
            
            // 验证是否有有效的回答内容
            if (StrUtil.isBlank(answerContent)) {
                response.setErrorMessage("请提供文字回答或录音文件");
                return response;
            }
            
            // 3. 构建评分提示词
            String evaluationPrompt = String.format(
                "请检查用户的答案得分：\n\n" +
                "question：%s\n\n" +
                "answer：%s\n\n",
                questionContent, answerContent
            );
            
            // 4. 获取Agent配置
            Long agentId = requestParam.getAgentId() != null ? requestParam.getAgentId() : 1345345L;
            AgentPropertiesDO agentProperties = agentPropertiesLoader.getAgentPropertiesMap().get(agentId);
            if (agentProperties == null) {
                response.setErrorMessage("智能体配置不存在");
                return response;
            }
            
            // 5. 调用AI接口进行同步评分（非流式）
            StringBuilder aiResponse = new StringBuilder();
            xingChenAIClient.chat(
                evaluationPrompt,
                requestParam.getSessionId() != null ? requestParam.getSessionId() : "evaluation_" + System.currentTimeMillis(),
                "{}", // 空的历史记录
                false, // 非流式
                new OutputStream() {
                    @Override
                    public void write(int b) { /* 不需要实现 */ }
                    
                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        aiResponse.append(new String(b, off, len));
                    }
                    
                    @Override
                    public void flush() { /* 确保数据发送 */ }
                },
                data -> {},
                agentProperties.getApiKey(),
                agentProperties.getApiSecret(),
                agentProperties.getApiFlowId()
            );
            
            // 6. 解析AI响应获取分数
            String aiResponseStr = aiResponse.toString();
            log.info("AI评分响应: {}", aiResponseStr);
            
            try {
                // 首先提取嵌套在choices中的content内容
                String contentStr = extractContentFromInterviewResponse(aiResponseStr);
                log.info("提取的content内容: {}", contentStr);
                
                if (contentStr != null && !contentStr.trim().isEmpty()) {
                    // 解析content中的JSON数据
                    JSONObject contentJson = JSON.parseObject(contentStr);
                    Map<String, Object> responseMap = contentJson.toJavaObject(Map.class);
                    
                    if (responseMap != null && responseMap.containsKey("score")) {
                        Object scoreObj = responseMap.get("score");
                        Integer score = null;
                        
                        if (scoreObj instanceof Number) {
                            score = ((Number) scoreObj).intValue();
                        } else if (scoreObj instanceof String) {
                            try {
                                score = Integer.parseInt((String) scoreObj);
                            } catch (NumberFormatException e) {
                                log.warn("无法解析分数字符串: {}", scoreObj);
                            }
                        }
                        
                        if (score != null && score >= 0 && score <= 100) {
                            response.setScore(score);
                            
                            // 7. 累加分数到Redis
                            Integer totalScore = interviewQuestionCacheService.addUserScore(username, score);
                            response.setTotalScore(totalScore);
                            
                            // 8. 设置反馈内容
                            if (responseMap.containsKey("feedback")) {
                                response.setFeedback(responseMap.get("feedback").toString());
                            }
                            
                            response.setIsSuccess(true);
                            log.info("用户 {} 回答题目 {} 评分成功，得分: {}, 总分: {}", username, requestParam.getQuestionNumber(), score, totalScore);
                        } else {
                            response.setErrorMessage("AI返回的分数格式不正确");
                            log.warn("AI返回的分数超出范围: {}", score);
                        }
                    } else {
                        response.setErrorMessage("AI响应中未找到分数信息");
                        log.warn("AI响应格式不正确，未找到score字段: {}", contentStr);
                    }
                } else {
                    response.setErrorMessage("AI响应内容为空");
                    log.warn("提取的AI响应内容为空: {}", aiResponseStr);
                }
            } catch (Exception parseException) {
                response.setErrorMessage("解析AI响应失败");
                log.error("解析AI评分响应失败: {}", parseException.getMessage(), parseException);
            }
            
        } catch (Exception e) {
            response.setErrorMessage("评分过程中发生错误: " + e.getMessage());
            log.error("面试题回答评分失败，用户: {}, 题号: {}, 错误: {}", username, requestParam.getQuestionNumber(), e.getMessage(), e);
        }
        
        return response;
    }
    
    @Override
    public String evaluateDemeanor(String username, DemeanorEvaluationReqDTO reqDTO) {
        try {
            // 1. 上传图片到讯飞服务器获取URL
            String imageUrl = null;
            if (reqDTO.getUserPhoto() != null && !reqDTO.getUserPhoto().isEmpty()) {
                try {
                    // 获取智能体配置中的API密钥
                    Long agentId = reqDTO.getAgentId() != null ? reqDTO.getAgentId() : 1345345L;
                    AgentPropertiesDO agentProperties = agentPropertiesLoader.getAgentPropertiesMap().get(agentId);
                    if (agentProperties == null) {
                        throw new RuntimeException("智能体配置不存在");
                    }
                    
                    // 上传图片到讯飞服务器获取URL
                    imageUrl = xingChenAIClient.uploadFile(
                        reqDTO.getUserPhoto(), 
                        agentProperties.getApiKey(),
                        agentProperties.getApiSecret()
                    );
                    log.info("图片上传成功，URL: {}", imageUrl);
                } catch (Exception e) {
                    log.error("图片上传失败: {}", e.getMessage());
                    throw new ClientException("FILE_UPLOAD_FAILED", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
                }
            }
            
            if (imageUrl == null) {
                throw new ClientException("USER_PHOTO_NOT_FOUND", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
            }
            
            // 2. 构建神态评分的提示词
            String promptBuilder = "请对这张照片进行神态评分，分析用户的表情和神态，返回以下四个参数的评分（每个参数范围0-100）";
            
            // 3. 调用AI接口进行神态评分
            Long agentId = reqDTO.getAgentId() != null ? reqDTO.getAgentId() : 1345345L;
            AgentPropertiesDO agentProperties = agentPropertiesLoader.getAgentPropertiesMap().get(agentId);
            if (agentProperties == null) {
                throw new ClientException("AGENT_CONFIG_NOT_FOUND",AgentErrorCodeEnum.AGENT_SAVE_ERROR);
            }
            
            // 4. 使用现有的XingChenAIClient进行同步调用
             StringBuilder aiResponse = new StringBuilder();
             xingChenAIClient.chat(
                 promptBuilder,
                 reqDTO.getSessionId() != null ? reqDTO.getSessionId() : "demeanor_" + System.currentTimeMillis(),
                 "{}", // 空的历史记录
                 false, // 非流式
                 new OutputStream() {
                     @Override
                     public void write(int b) { /* 不需要实现 */ }
                     
                     @Override
                     public void write(byte[] b, int off, int len) throws IOException {
                         aiResponse.append(new String(b, off, len));
                     }
                     
                     @Override
                     public void flush() { /* 确保数据发送 */ }
                 },
                 data -> {},
                 agentProperties.getApiKey(),
                 agentProperties.getApiSecret(),
                 agentProperties.getApiFlowId(),
                 imageUrl // 传递图片URL
             );
            
            // 5. 解析AI响应获取评分数据
            String aiResponseStr = aiResponse.toString();
            log.info("AI神态评分原始响应: {}", aiResponseStr);
            
            try {
                JSONObject jsonObject = JSON.parseObject(aiResponseStr);
                Map<String, Object> responseMap = jsonObject.toJavaObject(Map.class);
                log.info("解析后的响应Map: {}", responseMap);
                
                if (responseMap != null) {
                    // 从choices数组中提取content内容
                    String contentStr = extractContentFromResponse(responseMap);
                    log.info("提取的content内容: {}", contentStr);
                    
                    if (contentStr != null) {
                        // 解析content中的JSON数据
                        JSONObject contentJson = JSON.parseObject(contentStr);
                        Map<String, Object> contentMap = contentJson.toJavaObject(Map.class);
                        log.info("解析后的content Map: {}", contentMap);
                        
                        // 解析四个评分参数
                        log.info("开始解析各个评分字段...");
                        Integer panicLevel = parseScoreFromResponse(contentMap, "panicLevel");
                        log.info("panicLevel解析结果: {}", panicLevel);
                        
                        Integer seriousnessLevel = parseScoreFromResponse(contentMap, "seriousnessLevel");
                        log.info("seriousnessLevel解析结果: {}", seriousnessLevel);
                        
                        Integer emoticonHandling = parseScoreFromResponse(contentMap, "emoticonHandling");
                        log.info("emoticonHandling解析结果: {}", emoticonHandling);
                        
                        Integer compositeScore = parseScoreFromResponse(contentMap, "compositeScore");
                        log.info("compositeScore解析结果: {}", compositeScore);
                    
                        // 验证评分范围
                        if (panicLevel != null && seriousnessLevel != null && 
                            emoticonHandling != null && compositeScore != null) {
                            
                            log.info("所有评分字段解析成功，开始缓存数据...");
                            
                            // 6. 缓存神态评分详细数据到Redis
                            interviewQuestionCacheService.cacheDemeanorScoreDetails(
                                username, panicLevel, seriousnessLevel, emoticonHandling, compositeScore
                            );
                            
                            // 7. 同时更新神态管理评分（用于综合雷达图）
                            interviewQuestionCacheService.cacheDemeanorScore(username, compositeScore);
                            
                            log.info("用户 {} 神态评分成功，慌乱度: {}, 严肃程度: {}, 表情处理: {}, 综合得分: {}", 
                                username, panicLevel, seriousnessLevel, emoticonHandling, compositeScore);
                            
                            return "神态评分完成";
                        } else {
                            log.error("评分字段验证失败 - panicLevel: {}, seriousnessLevel: {}, emoticonHandling: {}, compositeScore: {}", 
                                panicLevel, seriousnessLevel, emoticonHandling, compositeScore);
                            throw new ClientException("AI_RESPONSE_INVALID", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
                        }
                    } else {
                        log.error("无法提取content内容");
                        throw new ClientException("AI_RESPONSE_CONTENT_MISSING", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
                    }
                } else {
                    log.error("响应Map为空");
                    throw new ClientException("AI_RESPONSE_FORMAT_ERROR", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
                }
            } catch (Exception parseException) {
                log.error("解析AI神态评分响应失败，原始响应: {}, 错误: {}", aiResponseStr, parseException.getMessage(), parseException);
                throw new ClientException("AI_RESPONSE_PARSE_ERROR", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
            }
            
        } catch (Exception e) {
            log.error("神态评分失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            throw new ClientException("DEMEANOR_EVALUATION_FAILED", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }
    }
    
    /**
     * 从AI响应中提取content内容
     * @param responseMap AI响应的Map对象
     * @return content字符串
     */
    private String extractContentFromResponse(Map<String, Object> responseMap) {
        try {
            if (responseMap.containsKey("choices")) {
                Object choicesObj = responseMap.get("choices");
                if (choicesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
                    if (!choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        if (firstChoice.containsKey("delta")) {
                            Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");
                            if (delta.containsKey("content")) {
                                return delta.get("content").toString();
                            }
                        }
                        // 如果没有delta，尝试直接从message中获取
                        if (firstChoice.containsKey("message")) {
                            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                            if (message.containsKey("content")) {
                                return message.get("content").toString();
                            }
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("提取content内容失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从面试题抽取AI响应中提取content内容
     * @param aiResponse AI的原始响应
     * @return 提取出的content字符串
     */
    private String extractContentFromInterviewResponse(String aiResponse) {
        try {
            log.info("开始提取面试题AI响应content，原始响应: {}", aiResponse);
            
            // 尝试解析为JSON
            JSONObject jsonObject = JSON.parseObject(aiResponse);
            if (jsonObject == null) {
                log.warn("AI响应不是有效的JSON格式");
                return aiResponse; // 如果不是JSON，直接返回原始内容
            }
            
            // 检查是否有choices数组
            if (jsonObject.containsKey("choices")) {
                JSONArray choices = jsonObject.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice != null) {
                        // 检查delta.content
                        if (firstChoice.containsKey("delta")) {
                            JSONObject delta = firstChoice.getJSONObject("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String content = delta.getString("content");
                                log.info("从choices[0].delta.content提取到面试题内容: {}", content);
                                return content;
                            }
                        }
                        
                        // 检查message.content
                        if (firstChoice.containsKey("message")) {
                            JSONObject message = firstChoice.getJSONObject("message");
                            if (message != null && message.containsKey("content")) {
                                String content = message.getString("content");
                                log.info("从choices[0].message.content提取到面试题内容: {}", content);
                                return content;
                            }
                        }
                    }
                }
            }
            
            // 如果没有找到嵌套的content，检查顶层是否直接包含content
            if (jsonObject.containsKey("content")) {
                String content = jsonObject.getString("content");
                log.info("从顶层content提取到面试题内容: {}", content);
                return content;
            }
            
            log.warn("未找到content字段，返回原始响应");
            return aiResponse;
            
        } catch (Exception e) {
            log.error("提取面试题content时发生异常: {}", e.getMessage(), e);
            return aiResponse; // 异常情况下返回原始内容
        }
    }
    
    /**
     * 从AI响应中解析评分数据
     * @param responseMap AI响应的Map对象
     * @param scoreKey 评分字段名
     * @return 评分值（0-100）
     */
    private Integer parseScoreFromResponse(Map<String, Object> responseMap, String scoreKey) {
        log.info("开始解析字段: {}", scoreKey);
        
        if (!responseMap.containsKey(scoreKey)) {
            log.warn("响应中不包含字段: {}", scoreKey);
            return null;
        }
        
        Object scoreObj = responseMap.get(scoreKey);
        log.info("字段 {} 的原始值: {} (类型: {})", scoreKey, scoreObj, scoreObj != null ? scoreObj.getClass().getSimpleName() : "null");
        
        Integer score = null;
        
        if (scoreObj instanceof Number) {
            score = ((Number) scoreObj).intValue();
            log.info("从Number类型解析得到: {}", score);
        } else if (scoreObj instanceof String) {
            try {
                score = Integer.parseInt((String) scoreObj);
                log.info("从String类型解析得到: {}", score);
            } catch (NumberFormatException e) {
                log.warn("无法解析评分字符串: {} = {}, 错误: {}", scoreKey, scoreObj, e.getMessage());
                return null;
            }
        } else {
            log.warn("不支持的数据类型: {} = {} ({})", scoreKey, scoreObj, scoreObj != null ? scoreObj.getClass() : "null");
            return null;
        }
        
        // 对于超出范围的值，进行范围调整而不是直接返回null
        if (score != null) {
            if (score < 0) {
                log.warn("评分小于0，调整为0: {} = {}", scoreKey, score);
                score = 0;
            } else if (score > 100) {
                log.warn("评分大于100，调整为100: {} = {}", scoreKey, score);
                score = 100;
            }
            log.info("字段 {} 最终解析结果: {}", scoreKey, score);
            return score;
        } else {
            log.warn("字段 {} 解析结果为null", scoreKey);
            return null;
        }
    }
}





