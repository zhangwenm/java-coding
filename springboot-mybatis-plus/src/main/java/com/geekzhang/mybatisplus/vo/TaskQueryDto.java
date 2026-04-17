package com.geekzhang.mybatisplus.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 任务查询请求
 */
@Data
public class TaskQueryDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> productIds;
    private String robotType;
    private Integer start;
    private Integer count;
    private String startTime;
    private String endTime;
    private String taskType;
}
