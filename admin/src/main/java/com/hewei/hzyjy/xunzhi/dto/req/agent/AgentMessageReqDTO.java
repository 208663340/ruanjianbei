package com.hewei.hzyjy.xunzhi.dto.req.agent;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName agent_message_0
 */
@Data
@TableName("agent_message")
public class AgentMessageReqDTO {
    /**
     * ID
     */
    private Long id;

    /**
     * 智能体id
     */
    private Long agentId;

    /**
     * 智能体消息
     */
    private String chatMessage;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 是否完成对话
     */
    private Integer isSuccess;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;

}