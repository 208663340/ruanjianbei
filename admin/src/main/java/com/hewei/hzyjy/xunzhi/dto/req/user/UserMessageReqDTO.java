package com.hewei.hzyjy.xunzhi.dto.req.user;

import lombok.Data;

@Data
public class UserMessageReqDTO {

    /**
     * 用户名
     */
    private String userName;

    /**
     * AgentID
     */
    private Long agentId;

    /**
     * 用户输入信息
     */
    private String inputMessage;

    /**
     * 消息序号
     */
    private int messageSeq;

    /**
     * 会话ID（
     */
    private String sessionId;
}
