package com.hewei.hzyjy.xunzhi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiSessionCreateReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiConversationRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.service.AiConversationService;
import com.hewei.hzyjy.xunzhi.common.util.SaTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;





/**
 * AI会话控制器
 * @author nageoffer
 */
@RestController
@RequestMapping("/api/xunzhi/v1/ai/conversations")
@RequiredArgsConstructor
public class AiConversationController {
    
    private final AiConversationService aiConversationService;
    private final SaTokenUtil saTokenUtil;
    
    /**
     * 创建AI会话
     */
    @PostMapping
    public Result<AiSessionCreateRespDTO> createConversation(@RequestBody AiSessionCreateReqDTO requestParam, HttpServletRequest request) {
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
        AiSessionCreateRespDTO result = aiConversationService.createConversationWithTitle(
                username, 
                requestParam.getAiId(), 
                requestParam.getFirstMessage()
        );
        return Results.success(result);
    }
    
    /**
     * 分页查询会话列表
     */
    @GetMapping
    public Result<IPage<AiConversationRespDTO>> pageConversations(
            AiConversationPageReqDTO requestParam,
            HttpServletRequest request) {
        // 从token中获取用户名
        String username = saTokenUtil.getUsernameFromRequest(request);
        IPage<AiConversationRespDTO> result = aiConversationService.pageConversations(username, requestParam);
        return Results.success(result);
    }
    
    /**
     * 更新会话信息
     */
    @PutMapping("/{sessionId}")
    public Result<Void> updateConversation(@PathVariable String sessionId, 
                                          @RequestParam(required = false) Integer messageCount,
                                          @RequestParam(required = false) String title) {
        aiConversationService.updateConversation(sessionId, messageCount, title);
        return Results.success();
    }
    
    /**
     * 结束会话
     */
    @PutMapping("/{sessionId}/end")
    public Result<Void> endConversation(@PathVariable String sessionId) {
        aiConversationService.endConversation(sessionId);
        return Results.success();
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteConversation(@PathVariable String sessionId) {
        aiConversationService.deleteConversation(sessionId);
        return Results.success();
    }
    
    /**
     * 根据会话ID获取会话信息
     */
    @GetMapping("/{sessionId}")
    public Result<AiConversationRespDTO> getConversationById(@PathVariable String sessionId) {
        AiConversationRespDTO result = aiConversationService.getConversationBySessionId(sessionId);
        return Results.success(result);
    }
}