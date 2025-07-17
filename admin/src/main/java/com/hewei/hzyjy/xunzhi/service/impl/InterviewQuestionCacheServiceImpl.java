package com.hewei.hzyjy.xunzhi.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.dto.req.agent.DemeanorScoreDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.RadarChartDTO;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.hewei.hzyjy.xunzhi.dao.entity.InterviewQuestion;
import com.hewei.hzyjy.xunzhi.service.InterviewQuestionCacheService;
import com.hewei.hzyjy.xunzhi.service.InterviewQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 面试题缓存服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionCacheServiceImpl implements InterviewQuestionCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final InterviewQuestionService interviewQuestionService;
    
    /**
     * 面试题缓存前缀
     */
    private static final String INTERVIEW_QUESTIONS_KEY = "interview:questions:";
    
    /**
     * 面试建议缓存前缀
     */
    private static final String INTERVIEW_SUGGESTIONS_KEY = "interview:suggestions:";
    
    /**
     * 简历评分缓存前缀
     */
    private static final String RESUME_SCORE_KEY = "interview:resume_score:";
    
    /**
     * 神态管理评分缓存前缀
     */
    private static final String DEMEANOR_SCORE_KEY = "interview:demeanor_score:";
    
    /**
     * 用户分数缓存前缀
     */
    private static final String USER_SCORE_KEY = "interview:score:";
    
    /**
     * 缓存过期时间（小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 24;
    
    @Override
    public void cacheInterviewQuestions(String username, List<String> questions) {
        try {
            String cacheKey = INTERVIEW_QUESTIONS_KEY + username;
            
            // 清除旧的缓存
            stringRedisTemplate.delete(cacheKey);
            
            // 存储新的面试题，使用题号作为field，题目作为value
            Map<String, String> questionMap = new HashMap<>();
            for (int i = 0; i < questions.size(); i++) {
                String questionNumber = String.valueOf(i + 1);
                questionMap.put(questionNumber, questions.get(i));
            }
            
            if (!questionMap.isEmpty()) {
                stringRedisTemplate.opsForHash().putAll(cacheKey, questionMap);
                // 设置过期时间
                stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            log.info("成功缓存用户 {} 的 {} 道面试题", username, questions.size());
        } catch (Exception e) {
            log.error("缓存面试题失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    @Override
    public void cacheInterviewSuggestions(String username, List<String> suggestions) {
        try {
            String cacheKey = INTERVIEW_SUGGESTIONS_KEY + username;
            
            // 清除旧的缓存
            stringRedisTemplate.delete(cacheKey);
            
            // 存储新的面试建议，使用建议编号作为field，建议内容作为value
            Map<String, String> suggestionMap = new HashMap<>();
            for (int i = 0; i < suggestions.size(); i++) {
                String suggestionNumber = String.valueOf(i + 1);
                suggestionMap.put(suggestionNumber, suggestions.get(i));
            }
            
            if (!suggestionMap.isEmpty()) {
                stringRedisTemplate.opsForHash().putAll(cacheKey, suggestionMap);
                // 设置过期时间
                stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            log.info("成功缓存用户 {} 的 {} 条面试建议", username, suggestions.size());
        } catch (Exception e) {
            log.error("缓存面试建议失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    @Override
    public void cacheResumeScore(String username, Integer resumeScore) {
        try {
            String cacheKey = RESUME_SCORE_KEY + username;
            // 清除旧的缓存
            stringRedisTemplate.delete(cacheKey);

            stringRedisTemplate.opsForValue().set(cacheKey, resumeScore.toString());
            // 设置过期时间
            stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("成功缓存用户 {} 的简历评分: {}", username, resumeScore);
        } catch (Exception e) {
            log.error("缓存简历评分失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    @Override
    public void cacheDemeanorScore(String username, Integer demeanorScore) {
        try {
            String cacheKey = DEMEANOR_SCORE_KEY + username;
            stringRedisTemplate.opsForValue().set(cacheKey, demeanorScore.toString());
            // 设置过期时间
            stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("成功缓存用户 {} 的神态管理评分: {}", username, demeanorScore);
        } catch (Exception e) {
            log.error("缓存神态管理评分失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, String> getUserInterviewQuestions(String username) {
        try {
            String cacheKey = INTERVIEW_QUESTIONS_KEY + username;
            Map<Object, Object> rawMap = stringRedisTemplate.opsForHash().entries(cacheKey);
            
            // 使用LinkedHashMap保持插入顺序，并按题号排序
            Map<String, String> questionMap = new LinkedHashMap<>();
            
            // 将题号转换为整数进行排序
            rawMap.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    try {
                        // 提取题号进行数字排序
                        String key1 = entry1.getKey().toString();
                        String key2 = entry2.getKey().toString();
                        
                        // 如果题号是纯数字，按数字排序
                        if (key1.matches("\\d+") && key2.matches("\\d+")) {
                            return Integer.compare(Integer.parseInt(key1), Integer.parseInt(key2));
                        }
                        // 否则按字符串排序
                        return key1.compareTo(key2);
                    } catch (NumberFormatException e) {
                        // 如果转换失败，按字符串排序
                        return entry1.getKey().toString().compareTo(entry2.getKey().toString());
                    }
                })
                .forEach(entry -> {
                    questionMap.put(entry.getKey().toString(), entry.getValue().toString());
                });
            
            log.info("获取用户 {} 的面试题成功，共 {} 道题，已按题号排序", username, questionMap.size());
            return questionMap;
        } catch (Exception e) {
            log.error("获取用户面试题失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    @Override
    public Integer getUserResumeScore(String username) {
        try {
            String cacheKey = RESUME_SCORE_KEY + username;
            String scoreStr = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(scoreStr)) {
                return Integer.parseInt(scoreStr);
            }
            return null;
        } catch (Exception e) {
            log.error("获取用户简历评分失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public Integer getUserDemeanorScore(String username) {
        try {
            String cacheKey = DEMEANOR_SCORE_KEY + username;
            String scoreStr = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(scoreStr)) {
                return Integer.parseInt(scoreStr);
            }
            return null;
        } catch (Exception e) {
            log.error("获取用户神态管理评分失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public Map<String, String> getUserInterviewSuggestions(String username) {
        try {
            String cacheKey = INTERVIEW_SUGGESTIONS_KEY + username;
            Map<Object, Object> rawMap = stringRedisTemplate.opsForHash().entries(cacheKey);
            
            // 使用LinkedHashMap保持插入顺序，并按建议编号排序
            Map<String, String> suggestionMap = new LinkedHashMap<>();
            
            // 将建议编号转换为整数进行排序
            rawMap.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    try {
                        // 提取建议编号进行数字排序
                        String key1 = entry1.getKey().toString();
                        String key2 = entry2.getKey().toString();
                        
                        // 如果编号是纯数字，按数字排序
                        if (key1.matches("\\d+") && key2.matches("\\d+")) {
                            return Integer.compare(Integer.parseInt(key1), Integer.parseInt(key2));
                        }
                        // 否则按字符串排序
                        return key1.compareTo(key2);
                    } catch (NumberFormatException e) {
                        // 如果转换失败，按字符串排序
                        return entry1.getKey().toString().compareTo(entry2.getKey().toString());
                    }
                })
                .forEach(entry -> {
                    suggestionMap.put(entry.getKey().toString(), entry.getValue().toString());
                });
            
            log.info("获取用户 {} 的面试建议成功，共 {} 条建议，已按编号排序", username, suggestionMap.size());
            return suggestionMap;
        } catch (Exception e) {
            log.error("获取用户面试建议失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    @Override
    public String getQuestionByNumber(String username, String questionNumber) {
        try {
            String cacheKey = INTERVIEW_QUESTIONS_KEY + username;
            Object question = stringRedisTemplate.opsForHash().get(cacheKey, questionNumber);
            return question != null ? question.toString() : null;
        } catch (Exception e) {
            log.error("获取题目失败，用户: {}, 题号: {}, 错误: {}", username, questionNumber, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public void clearUserQuestions(String username) {
        try {
            String cacheKey = INTERVIEW_QUESTIONS_KEY + username;
            stringRedisTemplate.delete(cacheKey);
            log.info("清除用户 {} 的面试题缓存", username);
        } catch (Exception e) {
            log.error("清除面试题缓存失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    @Override
    public void clearUserSuggestions(String username) {
        try {
            String cacheKey = INTERVIEW_SUGGESTIONS_KEY + username;
            stringRedisTemplate.delete(cacheKey);
            log.info("清除用户 {} 的面试建议缓存", username);
        } catch (Exception e) {
            log.error("清除面试建议缓存失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    /**
     * 从数据库加载面试题到缓存
     * 优先使用JSON格式数据，如果不存在则使用List格式数据
     */
    public void loadInterviewQuestionsFromDatabase(String sessionId, String username) {
        try {
            InterviewQuestion question = interviewQuestionService.getBySessionId(sessionId);
            if (question == null) {
                log.warn("未找到会话 {} 的面试题数据", sessionId);
                return;
            }
            
            // 优先使用JSON格式数据
            if (StrUtil.isNotBlank(question.getQuestionsJson())) {
                try {
                    Map<String, String> questionsMap = JSON.parseObject(
                        question.getQuestionsJson(), 
                        new TypeReference<LinkedHashMap<String, String>>() {}
                    );
                    
                    String cacheKey = INTERVIEW_QUESTIONS_KEY + username;
                    stringRedisTemplate.delete(cacheKey);
                    
                    if (!questionsMap.isEmpty()) {
                        stringRedisTemplate.opsForHash().putAll(cacheKey, questionsMap);
                        stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                    }
                    
                    log.info("从数据库JSON格式加载面试题到缓存成功，用户: {}, 题目数量: {}", username, questionsMap.size());
                    return;
                } catch (Exception e) {
                    log.warn("解析面试题JSON数据失败，尝试使用List格式数据，错误: {}", e.getMessage());
                }
            }
            
            // 如果JSON格式数据不存在或解析失败，使用List格式数据
            if (question.getQuestions() != null && !question.getQuestions().isEmpty()) {
                cacheInterviewQuestions(username, question.getQuestions());
                log.info("从数据库List格式加载面试题到缓存成功，用户: {}, 题目数量: {}", username, question.getQuestions().size());
            }
            
        } catch (Exception e) {
            log.error("从数据库加载面试题到缓存失败，会话ID: {}, 用户: {}, 错误: {}", sessionId, username, e.getMessage(), e);
        }
    }
    
    /**
     * 从数据库加载面试建议到缓存
     * 优先使用JSON格式数据，如果不存在则使用List格式数据
     */
    public void loadInterviewSuggestionsFromDatabase(String sessionId, String username) {
        try {
            InterviewQuestion question = interviewQuestionService.getBySessionId(sessionId);
            if (question == null) {
                log.warn("未找到会话 {} 的面试建议数据", sessionId);
                return;
            }
            
            // 优先使用JSON格式数据
            if (StrUtil.isNotBlank(question.getSuggestionsJson())) {
                try {
                    Map<String, String> suggestionsMap = JSON.parseObject(
                        question.getSuggestionsJson(), 
                        new TypeReference<LinkedHashMap<String, String>>() {}
                    );
                    
                    String cacheKey = INTERVIEW_SUGGESTIONS_KEY + username;
                    stringRedisTemplate.delete(cacheKey);
                    
                    if (!suggestionsMap.isEmpty()) {
                        stringRedisTemplate.opsForHash().putAll(cacheKey, suggestionsMap);
                        stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                    }
                    
                    log.info("从数据库JSON格式加载面试建议到缓存成功，用户: {}, 建议数量: {}", username, suggestionsMap.size());
                    return;
                } catch (Exception e) {
                    log.warn("解析面试建议JSON数据失败，尝试使用List格式数据，错误: {}", e.getMessage());
                }
            }
            
            // 如果JSON格式数据不存在或解析失败，使用List格式数据
            if (question.getSuggestions() != null && !question.getSuggestions().isEmpty()) {
                cacheInterviewSuggestions(username, question.getSuggestions());
                log.info("从数据库List格式加载面试建议到缓存成功，用户: {}, 建议数量: {}", username, question.getSuggestions().size());
            }
            
        } catch (Exception e) {
            log.error("从数据库加载面试建议到缓存失败，会话ID: {}, 用户: {}, 错误: {}", sessionId, username, e.getMessage(), e);
        }
    }
    
    /**
     * 从数据库加载简历评分到缓存
     */
    public void loadResumeScoreFromDatabase(String sessionId, String username) {
        try {
            InterviewQuestion question = interviewQuestionService.getBySessionId(sessionId);
            if (question == null || question.getResumeScore() == null) {
                log.warn("未找到会话 {} 的简历评分数据", sessionId);
                return;
            }
            
            cacheResumeScore(username, question.getResumeScore());
            log.info("从数据库加载简历评分到缓存成功，用户: {}, 评分: {}", username, question.getResumeScore());
            
        } catch (Exception e) {
            log.error("从数据库加载简历评分到缓存失败，会话ID: {}, 用户: {}, 错误: {}", sessionId, username, e.getMessage(), e);
        }
    }
    
    @Override
    public Integer getUserTotalScore(String username) {
        try {
            String scoreKey = USER_SCORE_KEY + username;
            String score = stringRedisTemplate.opsForValue().get(scoreKey);
            return score != null ? Integer.valueOf(score) : 0;
        } catch (Exception e) {
            log.error("获取用户总分失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public Integer addUserScore(String username, Integer score) {
        try {
            String scoreKey = USER_SCORE_KEY + username;
            Long newScore = stringRedisTemplate.opsForValue().increment(scoreKey, score);
            // 设置过期时间
            stringRedisTemplate.expire(scoreKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.info("用户 {} 本次得分: {}, 累计总分: {}", username, score, newScore);
            return newScore.intValue();
        } catch (Exception e) {
            log.error("累加用户分数失败，用户: {}, 分数: {}, 错误: {}", username, score, e.getMessage(), e);
            return getUserTotalScore(username);
        }
    }
    
    @Override
    public void resetUserScore(String username) {
        try {
            String scoreKey = USER_SCORE_KEY + username;
            stringRedisTemplate.delete(scoreKey);
            log.info("重置用户 {} 的分数", username);
        } catch (Exception e) {
            log.error("重置用户分数失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    @Override
    public RadarChartDTO getRadarChartData(String username) {
        try {
            // 从缓存获取三个评分
            Integer resumeScore = getUserResumeScore(username);
            Integer totalScore = getUserTotalScore(username);
            // 获取神态管理评分（使用神态评分的综合得分）
            String compositeKey = "demeanor:composite:" + username;
            String compositeScoreStr = stringRedisTemplate.opsForValue().get(compositeKey);
            Integer demeanorScore = StrUtil.isNotBlank(compositeScoreStr) ? Integer.parseInt(compositeScoreStr) : null;
            
            RadarChartDTO radarChart = new RadarChartDTO();
            
            // 直接使用0-100范围的整数
            Integer resumeNormalized = resumeScore != null ? resumeScore : 0;
            Integer interviewNormalized = totalScore != null ? Math.min(totalScore, 100) : 0;
            // 神态评分从十分制转换为百分制（乘以10）
            Integer demeanorNormalized = demeanorScore != null ? Math.min(demeanorScore * 10, 100) : 0;
            
            radarChart.setResumeScore(resumeNormalized);
            radarChart.setInterviewPerformance(interviewNormalized);
            radarChart.setDemeanorEvaluation(demeanorNormalized);
            
            // 生成专业技能评分：基于简历评分上下浮动10分以内的随机数
            Integer professionalSkills = resumeNormalized;
            if (resumeScore != null && resumeScore > 0) {
                // 生成-10到10之间的随机数
                int randomOffset = (int)((Math.random() - 0.5) * 20); // -10 到 10
                professionalSkills = Math.max(0, Math.min(100, resumeNormalized + randomOffset));
            }
            radarChart.setProfessionalSkills(professionalSkills);
            
            // 按权重计算用户潜力指数
            // resume: 0.25, interview: 0.4, demeanor: 0.15, professional: 0.2
            Integer potentialIndex = (int)(resumeNormalized * 0.25 + interviewNormalized * 0.4 + 
                                 demeanorNormalized * 0.15 + professionalSkills * 0.2);
            radarChart.setPotentialIndex(potentialIndex);
            
            log.info("获取用户 {} 雷达图数据成功", username);
            return radarChart;
        } catch (Exception e) {
            log.error("获取用户雷达图数据失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            // 返回默认值
            RadarChartDTO defaultChart = new RadarChartDTO();
            defaultChart.setResumeScore(0);
            defaultChart.setInterviewPerformance(0);
            defaultChart.setDemeanorEvaluation(0);
            defaultChart.setProfessionalSkills(0);
            defaultChart.setPotentialIndex(0);
            return defaultChart;
        }
    }
    
    @Override
    public void cacheDemeanorScoreDetails(String username, Integer panicLevel, Integer seriousnessLevel, 
                                          Integer emoticonHandling, Integer compositeScore) {
        try {
            String panicKey = "demeanor:panic:" + username;
            String seriousnessKey = "demeanor:seriousness:" + username;
            String emoticonKey = "demeanor:emoticon:" + username;
            String compositeKey = "demeanor:composite:" + username;
            
            stringRedisTemplate.opsForValue().set(panicKey, panicLevel.toString());
            stringRedisTemplate.opsForValue().set(seriousnessKey, seriousnessLevel.toString());
            stringRedisTemplate.opsForValue().set(emoticonKey, emoticonHandling.toString());
            stringRedisTemplate.opsForValue().set(compositeKey, compositeScore.toString());
            
            // 设置过期时间
            stringRedisTemplate.expire(panicKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.expire(seriousnessKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.expire(emoticonKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.expire(compositeKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.info("成功缓存用户 {} 的神态评分详细数据", username);
        } catch (Exception e) {
            log.error("缓存神态评分详细数据失败，用户: {}, 错误: {}", username, e.getMessage(), e);
        }
    }
    
    @Override
    public DemeanorScoreDTO getUserDemeanorScoreDetails(String username) {
        try {
            String panicKey = "demeanor:panic:" + username;
            String seriousnessKey = "demeanor:seriousness:" + username;
            String emoticonKey = "demeanor:emoticon:" + username;
            String compositeKey = "demeanor:composite:" + username;
            
            String panicStr = stringRedisTemplate.opsForValue().get(panicKey);
            String seriousnessStr = stringRedisTemplate.opsForValue().get(seriousnessKey);
            String emoticonStr = stringRedisTemplate.opsForValue().get(emoticonKey);
            String compositeStr = stringRedisTemplate.opsForValue().get(compositeKey);
            
            DemeanorScoreDTO demeanorScoreDTO = new DemeanorScoreDTO();
            
            // 神态评分从十分制转换为百分制（乘以10）
            demeanorScoreDTO.setPanicLevel(StrUtil.isNotBlank(panicStr) ? 
                Math.min(Integer.parseInt(panicStr) * 10, 100) : 0);
            demeanorScoreDTO.setSeriousnessLevel(StrUtil.isNotBlank(seriousnessStr) ? 
                Math.min(Integer.parseInt(seriousnessStr) * 10, 100) : 0);
            demeanorScoreDTO.setEmoticonHandling(StrUtil.isNotBlank(emoticonStr) ? 
                Math.min(Integer.parseInt(emoticonStr) * 10, 100) : 0);
            demeanorScoreDTO.setCompositeScore(StrUtil.isNotBlank(compositeStr) ? 
                Math.min(Integer.parseInt(compositeStr) * 10, 100) : 0);
            
            log.info("获取用户 {} 神态评分详细数据成功", username);
            return demeanorScoreDTO;
        } catch (Exception e) {
            log.error("获取用户神态评分详细数据失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            // 返回默认值
            DemeanorScoreDTO defaultScore = new DemeanorScoreDTO();
            defaultScore.setPanicLevel(0);
            defaultScore.setSeriousnessLevel(0);
            defaultScore.setEmoticonHandling(0);
            defaultScore.setCompositeScore(0);
            return defaultScore;
        }
    }
}