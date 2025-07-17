package com.hewei.hzyjy.xunzhi.dao.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName agent_properties
 */
@Data
@TableName("agent_properties")
public class AgentPropertiesDO {
    /**
     * ID
     */
    private Long id;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 鉴权密钥
     */
    private String apiSecret;

    /**
     * 鉴权key
     */
    private String apiKey;

    /**
     * 工作流id
     */
    private String apiFlowId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        AgentPropertiesDO other = (AgentPropertiesDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getAgentName() == null ? other.getAgentName() == null : this.getAgentName().equals(other.getAgentName()))
            && (this.getApiSecret() == null ? other.getApiSecret() == null : this.getApiSecret().equals(other.getApiSecret()))
            && (this.getApiKey() == null ? other.getApiKey() == null : this.getApiKey().equals(other.getApiKey()))
            && (this.getApiFlowId() == null ? other.getApiFlowId() == null : this.getApiFlowId().equals(other.getApiFlowId()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getDelFlag() == null ? other.getDelFlag() == null : this.getDelFlag().equals(other.getDelFlag()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAgentName() == null) ? 0 : getAgentName().hashCode());
        result = prime * result + ((getApiSecret() == null) ? 0 : getApiSecret().hashCode());
        result = prime * result + ((getApiKey() == null) ? 0 : getApiKey().hashCode());
        result = prime * result + ((getApiFlowId() == null) ? 0 : getApiFlowId().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getDelFlag() == null) ? 0 : getDelFlag().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", agentName=").append(agentName);
        sb.append(", apiSecret=").append(apiSecret);
        sb.append(", apiKey=").append(apiKey);
        sb.append(", apiFlowId=").append(apiFlowId);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", delFlag=").append(delFlag);
        sb.append("]");
        return sb.toString();
    }
}