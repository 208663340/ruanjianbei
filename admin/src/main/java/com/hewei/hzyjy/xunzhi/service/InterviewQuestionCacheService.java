package com.hewei.hzyjy.xunzhi.service;

import com.hewei.hzyjy.xunzhi.dto.req.agent.DemeanorScoreDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.RadarChartDTO;

import java.util.List;
import java.util.Map;

/**
 * 面试题缓存服务接口
 */
public interface InterviewQuestionCacheService {
    
    /**
     * 缓存面试题到Redis
     * @param username 用户名
     * @param questions 面试题列表
     */
    void cacheInterviewQuestions(String username, List<String> questions);
    
    /**
     * 缓存面试建议到Redis
     * @param username 用户名
     * @param suggestions 建议列表
     */
    void cacheInterviewSuggestions(String username, List<String> suggestions);
    
    /**
     * 缓存简历评分到Redis
     * @param username 用户名
     * @param resumeScore 简历评分
     */
    void cacheResumeScore(String username, Integer resumeScore);
    
    /**
     * 缓存神态管理评分到Redis
     * @param username 用户名
     * @param demeanorScore 神态管理评分
     */
    void cacheDemeanorScore(String username, Integer demeanorScore);
    
    /**
     * 获取用户的面试题
     * @param username 用户名
     * @return 题号和题目的映射
     */
    Map<String, String> getUserInterviewQuestions(String username);
    
    /**
     * 获取用户的面试建议
     * @param username 用户名
     * @return 建议编号和建议内容的映射
     */
    Map<String, String> getUserInterviewSuggestions(String username);
    
    /**
     * 获取用户的简历评分
     * @param username 用户名
     * @return 简历评分
     */
    Integer getUserResumeScore(String username);
    
    /**
     * 获取用户的神态管理评分
     * @param username 用户名
     * @return 神态管理评分
     */
    Integer getUserDemeanorScore(String username);
    
    /**
     * 根据题号获取题目
     * @param username 用户名
     * @param questionNumber 题号
     * @return 题目内容
     */
    String getQuestionByNumber(String username, String questionNumber);
    
    /**
     * 清除用户的面试题缓存
     * @param username 用户名
     */
    void clearUserQuestions(String username);
    
    /**
     * 清除用户的面试建议缓存
     * @param username 用户名
     */
    void clearUserSuggestions(String username);
    
    /**
     * 获取用户当前总分
     * @param username 用户名
     * @return 总分
     */
    Integer getUserTotalScore(String username);
    
    /**
     * 累加用户分数
     * @param username 用户名
     * @param score 本次得分
     * @return 累加后的总分
     */
    Integer addUserScore(String username, Integer score);
    
    /**
     * 重置用户分数
     * @param username 用户名
     */
    void resetUserScore(String username);
    
    /**
     * 获取雷达图数据
     * @param username 用户名
     * @return 雷达图数据
     */
    RadarChartDTO getRadarChartData(String username);
    
    /**
     * 缓存神态评分详细数据到Redis
     * @param username 用户名
     * @param panicLevel 慌乱度
     * @param seriousnessLevel 严肃程度
     * @param emoticonHandling 表情处理
     * @param compositeScore 综合得分
     */
    void cacheDemeanorScoreDetails(String username, Integer panicLevel, Integer seriousnessLevel, 
                                   Integer emoticonHandling, Integer compositeScore);
    
    /**
     * 获取用户神态评分详细数据
     * @param username 用户名
     * @return 神态评分详细数据
     */
    DemeanorScoreDTO getUserDemeanorScoreDetails(String username);
    
    /**
     * 从数据库加载面试题到缓存
     * 优先使用JSON格式数据，如果不存在则使用List格式数据
     * @param sessionId 会话ID
     * @param username 用户名
     */
    void loadInterviewQuestionsFromDatabase(String sessionId, String username);
    
    /**
     * 从数据库加载面试建议到缓存
     * 优先使用JSON格式数据，如果不存在则使用List格式数据
     * @param sessionId 会话ID
     * @param username 用户名
     */
    void loadInterviewSuggestionsFromDatabase(String sessionId, String username);
    
    /**
     * 从数据库加载简历评分到缓存
     * @param sessionId 会话ID
     * @param username 用户名
     */
    void loadResumeScoreFromDatabase(String sessionId, String username);
}