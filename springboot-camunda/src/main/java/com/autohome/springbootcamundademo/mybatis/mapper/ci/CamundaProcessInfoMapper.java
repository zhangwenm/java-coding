package com.autohome.springbootcamundademo.mybatis.mapper.ci;

import com.autohome.springbootcamundademo.mybatis.entity.ci.CamundaProcessInfo;

import java.util.List;

public interface CamundaProcessInfoMapper {
    List<CamundaProcessInfo> getAllCamundaProcessInfo();

    void save(CamundaProcessInfo camundaProcessInfo);

    void update(CamundaProcessInfo camundaProcessInfo);

    CamundaProcessInfo getCamundaInfoById(Integer id);

    List<CamundaProcessInfo> getCamundaProcessInfo(CamundaProcessInfo camundaProcessInfo);
}
