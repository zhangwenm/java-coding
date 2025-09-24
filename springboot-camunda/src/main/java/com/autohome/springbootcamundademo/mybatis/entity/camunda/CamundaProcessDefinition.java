package com.autohome.springbootcamundademo.mybatis.entity.camunda;

import lombok.Data;

@Data
public class CamundaProcessDefinition {
    private String id;
    private Integer version;
    private String name;
    private String bpmnXml;

    private String key;

}
