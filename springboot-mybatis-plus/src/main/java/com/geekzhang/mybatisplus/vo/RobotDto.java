package com.geekzhang.mybatisplus.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 机器人在线状态信息
 */
@Data
public class RobotDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String productId;
    private String placeId;
    private String type;
    private String subType;
    private Long ts;
    private Integer floor;
    private Float power;
    private Boolean charging;
    private Boolean estop;
    private Boolean idle;
    
    /**
     * 判断是否在线（5分钟内有心跳）
     */
    public Boolean getOnline() {
        return ts != null && System.currentTimeMillis() - ts < 5 * 60 * 1000;
    }
}