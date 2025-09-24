package com.autohome.springbootcamundademo.mybatis.entity.ci;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessWebHookEntity {
    private Integer id;
    private String processKey;
    private String subProcessKey;
    private String activityname;
    private String hookType;
    private String url;
    private String groupId;
    private String description;
    private String createUser;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    private String serviceLineId;
    private Integer isDel;
}
