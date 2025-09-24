package com.autohome.springbootcamundademo.mybatis.entity.ci;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class CamundaProcessInfo {
    private Integer id;
    private String processType;
    private String processKey;
    private String processDefinitionXml;
    private Integer processDefinitionVersion;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private String createdStime;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private String modifiedStime;
    private Integer isDel;
}
