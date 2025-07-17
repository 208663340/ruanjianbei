package com.hewei.hzyjy.xunzhi.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.dao.entity.InterviewQuestion;
import com.hewei.hzyjy.xunzhi.dao.repository.InterviewQuestionRepository;
import com.hewei.hzyjy.xunzhi.dto.req.agent.InterviewQuestionReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.InterviewQuestionRespDTO;
import com.hewei.hzyjy.xunzhi.service.InterviewQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 面试题服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private final InterviewQuestionRepository interviewQuestionRepository;

    @Override
    public InterviewQuestion saveInterviewQuestion(InterviewQuestion interviewQuestion) {
        if (interviewQuestion.getCreateTime() == null) {
            interviewQuestion.setCreateTime(new Date());
        }
        interviewQuestion.setUpdateTime(new Date());
        if (interviewQuestion.getDelFlag() == null) {
            interviewQuestion.setDelFlag(0);
        }
        return interviewQuestionRepository.save(interviewQuestion);
    }

    @Override
    public InterviewQuestion getBySessionId(String sessionId) {
        Optional<InterviewQuestion> optional = interviewQuestionRepository
                .findBySessionIdAndDelFlag(sessionId, 0);
        return optional.orElse(null);
    }

    @Override
    public List<InterviewQuestion> getByUserName(String userName) {
        return interviewQuestionRepository
                .findByUserNameAndDelFlagOrderByCreateTimeDesc(userName, 0);
    }

    @Override
    public IPage<InterviewQuestionRespDTO> pageUserInterviewQuestions(String userName, Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        org.springframework.data.domain.Page<InterviewQuestion> questionPage = 
                interviewQuestionRepository.findByUserNameAndDelFlagOrderByCreateTimeDesc(userName, 0, pageable);
        
        // 转换为MyBatis-Plus的IPage格式
        Page<InterviewQuestionRespDTO> resultPage = new Page<>(current, size);
        resultPage.setTotal(questionPage.getTotalElements());
        resultPage.setRecords(
                questionPage.getContent().stream()
                        .map(this::convertToRespDTO)
                        .collect(Collectors.toList())
        );
        
        return resultPage;
    }

    @Override
    public IPage<InterviewQuestionRespDTO> pageAllInterviewQuestions(Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        org.springframework.data.domain.Page<InterviewQuestion> questionPage = 
                interviewQuestionRepository.findByDelFlagOrderByCreateTimeDesc(0, pageable);
        
        // 转换为MyBatis-Plus的IPage格式
        Page<InterviewQuestionRespDTO> resultPage = new Page<>(current, size);
        resultPage.setTotal(questionPage.getTotalElements());
        resultPage.setRecords(
                questionPage.getContent().stream()
                        .map(this::convertToRespDTO)
                        .collect(Collectors.toList())
        );
        
        return resultPage;
    }

    @Override
    public List<InterviewQuestion> getByInterviewType(String interviewType) {
        return interviewQuestionRepository
                .findByInterviewTypeAndDelFlagOrderByCreateTimeDesc(interviewType, 0);
    }

    @Override
    public boolean deleteInterviewQuestion(String id) {
        try {
            Optional<InterviewQuestion> optional = interviewQuestionRepository.findById(id);
            if (optional.isPresent()) {
                InterviewQuestion question = optional.get();
                question.setDelFlag(1);
                question.setUpdateTime(new Date());
                interviewQuestionRepository.save(question);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("删除面试题失败，ID: {}, 错误: {}", id, e.getMessage());
            return false;
        }
    }

    @Override
    public Integer countByUserName(String userName) {
        return interviewQuestionRepository.countByUserNameAndDelFlag(userName, 0);
    }

    @Override
    public InterviewQuestion createFromAIResponse(InterviewQuestionReqDTO reqDTO, String aiResponseData, 
                                                  Integer responseTime, Integer tokenCount) {
        try {
            // 解析AI响应数据
            JSONObject responseJson = JSON.parseObject(aiResponseData);
            
            InterviewQuestion question = new InterviewQuestion();
            question.setSessionId(reqDTO.getSessionId());
            question.setUserName(reqDTO.getUserName());
            question.setAgentId(reqDTO.getAgentId());
            question.setResumeFileUrl(reqDTO.getResumeFileUrl());
            question.setResponseTime(responseTime);
            question.setTokenCount(tokenCount);
            question.setRawResponseData(aiResponseData);
            
            // 解析面试题列表
            if (responseJson.containsKey("questions")) {
                List<String> questions = responseJson.getJSONArray("questions")
                        .toJavaList(String.class);
                question.setQuestions(questions);
                
                // 转换为JSON格式存储（按题号组织）
                Map<String, String> questionsMap = new LinkedHashMap<>();
                for (int i = 0; i < questions.size(); i++) {
                    questionsMap.put(String.valueOf(i + 1), questions.get(i));
                }
                question.setQuestionsJson(JSON.toJSONString(questionsMap));
                log.info("面试题JSON格式存储完成，会话ID: {}, 题目数量: {}", reqDTO.getSessionId(), questions.size());
            }
            
            // 解析建议列表
            if (responseJson.containsKey("sugest") || responseJson.containsKey("suggestions")) {
                String suggestKey = responseJson.containsKey("sugest") ? "sugest" : "suggestions";
                List<String> suggestions = responseJson.getJSONArray(suggestKey)
                        .toJavaList(String.class);
                question.setSuggestions(suggestions);
                
                // 转换为JSON格式存储（按编号组织）
                Map<String, String> suggestionsMap = new LinkedHashMap<>();
                for (int i = 0; i < suggestions.size(); i++) {
                    suggestionsMap.put(String.valueOf(i + 1), suggestions.get(i));
                }
                question.setSuggestionsJson(JSON.toJSONString(suggestionsMap));
                log.info("面试建议JSON格式存储完成，会话ID: {}, 建议数量: {}", reqDTO.getSessionId(), suggestions.size());
            }
            
            // 解析简历评分
            if (responseJson.containsKey("resumeScore")) {
                try {
                    Integer resumeScore = responseJson.getInteger("resumeScore");
                    question.setResumeScore(resumeScore);
                    log.info("简历评分解析完成，会话ID: {}, 评分: {}", reqDTO.getSessionId(), resumeScore);
                } catch (Exception e) {
                    log.warn("简历评分解析失败，会话ID: {}, 错误: {}", reqDTO.getSessionId(), e.getMessage());
                }
            }
            
            // 解析面试类型
            if (responseJson.containsKey("type")) {
                question.setInterviewType(responseJson.getString("type"));
            }
            
            return saveInterviewQuestion(question);
            
        } catch (Exception e) {
            log.error("创建面试题记录失败，会话ID: {}, 错误: {}", reqDTO.getSessionId(), e.getMessage());
            
            // 创建错误记录
            InterviewQuestion errorQuestion = new InterviewQuestion();
            errorQuestion.setSessionId(reqDTO.getSessionId());
            errorQuestion.setUserName(reqDTO.getUserName());
            errorQuestion.setAgentId(reqDTO.getAgentId());
            errorQuestion.setResumeFileUrl(reqDTO.getResumeFileUrl());
            errorQuestion.setResponseTime(responseTime);
            errorQuestion.setTokenCount(tokenCount);
            errorQuestion.setRawResponseData(aiResponseData);
            errorQuestion.setErrorMessage(e.getMessage());
            
            return saveInterviewQuestion(errorQuestion);
        }
    }
    
    /**
     * 转换实体为响应DTO
     */
    private InterviewQuestionRespDTO convertToRespDTO(InterviewQuestion question) {
        InterviewQuestionRespDTO respDTO = new InterviewQuestionRespDTO();
        BeanUtils.copyProperties(question, respDTO);
        
        // 设置成功状态
        if (StrUtil.isBlank(question.getErrorMessage())) {
            respDTO.setIsSuccess(1);
        } else {
            respDTO.setIsSuccess(0);
        }
        
        return respDTO;
    }
}