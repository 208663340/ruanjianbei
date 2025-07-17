package com.hewei.hzyjy.xunzhi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiPropertiesCreateReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiPropertiesPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiPropertiesUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiPropertiesRespDTO;
import com.hewei.hzyjy.xunzhi.service.AiPropertiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI配置控制器
 * @author nageoffer
 */
@RestController
@RequestMapping("/api/xunzhi/v1/ai-properties")
@RequiredArgsConstructor
public class AiPropertiesController {
    
    private final AiPropertiesService aiPropertiesService;
    
    /**
     * 创建AI配置
     */
    @PostMapping
    public Result<Void> createAiProperties(@RequestBody AiPropertiesCreateReqDTO requestParam) {
        aiPropertiesService.createAiProperties(requestParam);
        return Results.success();
    }
    
    /**
     * 更新AI配置
     */
    @PutMapping
    public Result<Void> updateAiProperties(@RequestBody AiPropertiesUpdateReqDTO requestParam) {
        aiPropertiesService.updateAiProperties(requestParam);
        return Results.success();
    }
    
    /**
     * 删除AI配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAiProperties(@PathVariable Long id) {
        aiPropertiesService.deleteAiProperties(id);
        return Results.success();
    }
    
    /**
     * 根据ID查询AI配置
     */
    @GetMapping("/{id}")
    public Result<AiPropertiesRespDTO> getAiPropertiesById(@PathVariable Long id) {
        AiPropertiesRespDTO result = aiPropertiesService.getAiPropertiesById(id);
        return Results.success(result);
    }
    
    /**
     * 分页查询AI配置
     */
    @GetMapping
    public Result<IPage<AiPropertiesRespDTO>> pageAiProperties(AiPropertiesPageReqDTO requestParam) {
        IPage<AiPropertiesRespDTO> result = aiPropertiesService.pageAiProperties(requestParam);
        return Results.success(result);
    }
    
    /**
     * 查询所有启用的AI配置
     */
    @GetMapping("/enabled")
    public Result<List<AiPropertiesRespDTO>> getAllEnabledAiProperties() {
        List<AiPropertiesRespDTO> result = aiPropertiesService.getAllEnabledAiProperties();
        return Results.success(result);
    }
    
    /**
     * 启用/禁用AI配置
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleAiPropertiesStatus(@PathVariable Long id, @RequestParam Integer isEnabled) {
        aiPropertiesService.toggleAiPropertiesStatus(id, isEnabled);
        return Results.success();
    }
}