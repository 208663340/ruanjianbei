package com.hewei.hzyjy.xunzhi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.dto.req.agent.AgentConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.agent.AgentSessionCreateReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.agent.InterviewQuestionReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.agent.InterviewAnswerReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.InterviewQuestionRespDTO;
import com.hewei.hzyjy.xunzhi.dto.req.user.UserMessageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentConversationRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.AgentSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.InterviewAnswerRespDTO;
import com.hewei.hzyjy.xunzhi.dto.req.agent.DemeanorEvaluationReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.agent.RadarChartDTO;

import com.hewei.hzyjy.xunzhi.service.AgentConversationService;
import com.hewei.hzyjy.xunzhi.service.AgentMessageService;
import com.hewei.hzyjy.xunzhi.service.InterviewQuestionCacheService;
import com.hewei.hzyjy.xunzhi.common.util.SaTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.util.List;
import java.util.Map;

/**
 *Agent文字聊天接口
 * @author nageoffer
 * @date 2023/9/27
 */
@RestController
@RequestMapping("/api/xunzhi/v1/agents")
@RequiredArgsConstructor
public class AgentController {
    
    private final SaTokenUtil saTokenUtil;
    private final AgentMessageService agentMessageService;
    private final AgentConversationService agentConversationService;
    private final InterviewQuestionCacheService interviewQuestionCacheService;


    /**
     * 创建Agent会话
     * @param requestParam 会话创建请求参数
     * @return 会话ID和标题
     */
    @PostMapping("/sessions")
    public Result<AgentSessionCreateRespDTO> createSession(@RequestBody AgentSessionCreateReqDTO requestParam) {
        AgentSessionCreateRespDTO result = agentConversationService.createConversationWithTitle(
                requestParam.getUserName(), 
                requestParam.getAgentId(), 
                requestParam.getFirstMessage()
        );
        
        return Results.success(result);
    }

    /**
     * Agent文字聊天SSE接口
     * @return SSE流
     */
    @PostMapping("/sessions/{sessionId}/chat")
    public SseEmitter chat(@PathVariable String sessionId, @RequestBody UserMessageReqDTO requestParam, HttpServletRequest request) {
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
        if (username != null) {
            requestParam.setUserName(username);
        }
        requestParam.setSessionId(sessionId);
        return agentMessageService.agentChatSse(requestParam);
    }


    /**
     * 分页查询用户会话列表
     */
    @GetMapping("/conversations")
    public Result<IPage<AgentConversationRespDTO>> pageConversations(
            AgentConversationPageReqDTO requestParam,
            HttpServletRequest request) {
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
        return Results.success(agentConversationService.pageConversations(username, requestParam));
    }

    /**
     * 查询会话历史消息
     */
    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<AgentMessageHistoryRespDTO>> getConversationHistory(@PathVariable String sessionId) {
        return Results.success(agentMessageService.getConversationHistory(sessionId));
    }

    /**
     * 分页查询历史消息
     */
    @GetMapping("/messages/history")
    public Result<IPage<AgentMessageHistoryRespDTO>> pageHistoryMessages(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
        return Results.success(agentMessageService.pageHistoryMessages(username, sessionId, current, size));
    }

    /**
     * 结束会话
     */
    @PutMapping("/conversations/{sessionId}/end")
    public Result<Void> endConversation(@PathVariable String sessionId) {
        agentConversationService.endConversation(sessionId);
        return Results.success();
    }

    /**
     * 面试题获取接口
     */
    @PostMapping("/sessions/{sessionId}/interview-questions")
    public Result<InterviewQuestionRespDTO> extractInterviewQuestions(
            @PathVariable String sessionId,
            @RequestParam("agentId") Long agentId,
            @RequestParam("resumePdf") MultipartFile resumePdf,
            HttpServletRequest request) {
        
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
        
        // 构建请求DTO
        InterviewQuestionReqDTO reqDTO = new InterviewQuestionReqDTO();
        reqDTO.setUserName(username);
        reqDTO.setAgentId(agentId);
        reqDTO.setSessionId(sessionId);
        reqDTO.setResumePdf(resumePdf);
        
        InterviewQuestionRespDTO result = agentMessageService.extractInterviewQuestions(reqDTO);
        return Results.success(result);
    }
    
    /**
     * 回答面试题接口
     */
    @PostMapping("/interview/answer")
    public Result<InterviewAnswerRespDTO> answerInterviewQuestion(
            @RequestParam("questionNumber") String questionNumber,
            @RequestParam(value = "answerContent", required = false) String answerContent,
            @RequestPart(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "agentId", required = false) Long agentId,
            HttpServletRequest request) {
        
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
        
        // 构建请求DTO
        InterviewAnswerReqDTO requestParam = new InterviewAnswerReqDTO();
        requestParam.setQuestionNumber(questionNumber);
        requestParam.setAnswerContent(answerContent);
        requestParam.setAudioFile(audioFile);
        requestParam.setSessionId(sessionId);
        requestParam.setAgentId(agentId);
        
        InterviewAnswerRespDTO result = agentMessageService.answerInterviewQuestion(username, requestParam);
         return Results.success(result);
     }
     
     /**
      * 获取用户面试题列表
      */
    @GetMapping("/interview/questions")
    public Result<Map<String, String>> getUserInterviewQuestions(
            @RequestParam(required = false) String sessionId,
            HttpServletRequest request) {
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
         
        Map<String, String> questions = interviewQuestionCacheService.getUserInterviewQuestions(username);
        
        // 如果缓存中没有数据且提供了sessionId，尝试从数据库加载
        if ((questions == null || questions.isEmpty()) && sessionId != null) {
            interviewQuestionCacheService.loadInterviewQuestionsFromDatabase(sessionId, username);
            questions = interviewQuestionCacheService.getUserInterviewQuestions(username);
        }
        
        return Results.success(questions);
    }
     
     /**
      * 获取用户当前总分
      */
     @GetMapping("/interview/score")
     public Result<Integer> getUserTotalScore(HttpServletRequest request) {
         // 从token中获取用户名
         String username = saTokenUtil.getUsernameFromRequest(request);
         
         Integer totalScore = interviewQuestionCacheService.getUserTotalScore(username);
         return Results.success(totalScore);
     }
     
     /**
      * 获取用户面试建议列表
      */
     @GetMapping("/interview/suggestions")
     public Result<Map<String, String>> getUserInterviewSuggestions(
             @RequestParam(required = false) String sessionId,
             HttpServletRequest request) {
         // 从token中获取用户名
         String username = saTokenUtil.getUsernameFromRequest(request);
         
         Map<String, String> suggestions = interviewQuestionCacheService.getUserInterviewSuggestions(username);
         
         // 如果缓存中没有数据且提供了sessionId，尝试从数据库加载
         if ((suggestions == null || suggestions.isEmpty()) && sessionId != null) {
             interviewQuestionCacheService.loadInterviewSuggestionsFromDatabase(sessionId, username);
             suggestions = interviewQuestionCacheService.getUserInterviewSuggestions(username);
         }
         
         return Results.success(suggestions);
     }

     /**
      * 获取用户简历评分
      */
     @GetMapping("/resume/score")
     public Result<Integer> getUserResumeScore(
             @RequestParam(required = false) String sessionId,
             HttpServletRequest request) {
         // 从token中获取用户名
         String username = saTokenUtil.getUsernameFromRequest(request);
         
         Integer resumeScore = interviewQuestionCacheService.getUserResumeScore(username);
         
         // 如果缓存中没有数据且提供了sessionId，尝试从数据库加载
         if (resumeScore == null && sessionId != null) {
             interviewQuestionCacheService.loadResumeScoreFromDatabase(sessionId, username);
             resumeScore = interviewQuestionCacheService.getUserResumeScore(username);
         }
         
         return Results.success(resumeScore);
     }
     
     /**
      * 获取雷达图数据
      */
     @GetMapping("/radar-chart")
     public Result<RadarChartDTO> getRadarChartData(HttpServletRequest request) {
         // 从token中获取用户名
         String username = saTokenUtil.getUsernameFromRequest(request);
         
         RadarChartDTO radarChart = interviewQuestionCacheService.getRadarChartData(username);
         return Results.success(radarChart);
     }
     
     /**
       * 神态评分接口
       */
     @PostMapping("/demeanor-evaluation")
     public Result<String> evaluateDemeanor(
             @RequestPart("userPhoto") MultipartFile userPhoto,
             @RequestParam("agentId") Long agentId,
             @RequestParam("sessionId") String sessionId,
             HttpServletRequest request) {
              // 从token中获取用户名
              String username = saTokenUtil.getUsernameFromRequest(request);
              
              // 构建请求DTO
              DemeanorEvaluationReqDTO reqDTO = new DemeanorEvaluationReqDTO();
              reqDTO.setUserName(username);
              reqDTO.setAgentId(agentId);
              reqDTO.setSessionId(sessionId);
              reqDTO.setUserPhoto(userPhoto);
              
              // 调用智能体进行神态评分
              String result = agentMessageService.evaluateDemeanor(username, reqDTO);
              return Results.success(result);
     }

}
