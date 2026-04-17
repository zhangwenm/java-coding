package com.geekzhang.mybatisplus.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 机器人在线状态查询请求
 */
@Data
public class RobotDtoQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<String> productIdList;
}