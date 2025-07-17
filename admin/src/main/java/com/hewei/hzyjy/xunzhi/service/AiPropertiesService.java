package com.hewei.hzyjy.xunzhi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hewei.hzyjy.xunzhi.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiPropertiesCreateReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiPropertiesPageReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.ai.AiPropertiesUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.ai.AiPropertiesRespDTO;

import java.util.List;

/**
 * AI配置Service接口
 * @author nageoffer
 */
public interface AiPropertiesService extends IService<AiPropertiesDO> {
    
    /**
     * 创建AI配置
     */
    void createAiProperties(AiPropertiesCreateReqDTO requestParam);
    
    /**
     * 更新AI配置
     */
    void updateAiProperties(AiPropertiesUpdateReqDTO requestParam);
    
    /**
     * 删除AI配置
     */
    void deleteAiProperties(Long id);
    
    /**
     * 根据ID查询AI配置
     */
    AiPropertiesRespDTO getAiPropertiesById(Long id);
    
    /**
     * 分页查询AI配置列表
     */
    IPage<AiPropertiesRespDTO> pageAiProperties(AiPropertiesPageReqDTO requestParam);
    
    /**
     * 查询所有启用的AI配置
     */
    List<AiPropertiesRespDTO> listEnabledAiProperties();
    
    /**
     * 启用/禁用AI配置
     */
    void toggleAiPropertiesStatus(Long id, Integer isEnabled);

    List<AiPropertiesRespDTO> getAllEnabledAiProperties();

}