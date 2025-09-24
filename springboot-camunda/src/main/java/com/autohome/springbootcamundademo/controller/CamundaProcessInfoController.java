package com.autohome.springbootcamundademo.controller;

import com.autohome.springbootcamundademo.mybatis.entity.ci.CamundaProcessInfo;
import com.autohome.springbootcamundademo.mybatis.mapper.ci.CamundaProcessInfoMapper;
import com.autohome.springbootcamundademo.util.Result;
import com.autohome.springbootcamundademo.util.ResultGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CamundaProcessInfoController {
    private static final Logger logger = LoggerFactory.getLogger(CamundaProcessInfoController.class);
    @Autowired
    private CamundaProcessInfoMapper camundaProcessInfoMapper;

    @GetMapping("/camundaProcessInfo/list")
    public Result<List<CamundaProcessInfo>> getAllCamundaProcessInfo(CamundaProcessInfo camundaProcessInfo) {
     try {
         List<CamundaProcessInfo> allCamundaProcessInfo = camundaProcessInfoMapper.getCamundaProcessInfo(camundaProcessInfo);
         return ResultGenerator.genSuccessResult(allCamundaProcessInfo);
     }catch (Exception e) {
         logger.error("Error occurred while fetching all camunda process info", e);
         return ResultGenerator.genFailResult("Error occurred while fetching all camunda process info");
     }

    }

    @PostMapping("/camundaProcessInfo/add")
    public Result<String> add(@RequestBody CamundaProcessInfo camundaProcessInfo) {
        try {
            camundaProcessInfoMapper.save(camundaProcessInfo);
            return ResultGenerator.genSuccessResult("保存成功");
        }catch (Exception e) {
            logger.error("Error occurred while fetching all camunda process info", e);
            return ResultGenerator.genFailResult("Error occurred while fetching all camunda process info");
        }

    }

    @PostMapping("/camundaProcessInfo/update")
    public Result<String> update(@RequestBody CamundaProcessInfo camundaProcessInfo) {
        try {
            camundaProcessInfoMapper.update(camundaProcessInfo);
            return ResultGenerator.genSuccessResult("更新成功");
        }catch (Exception e) {
            logger.error("Error occurred while fetching all camunda process info", e);
            return ResultGenerator.genFailResult("Error occurred while fetching all camunda process info");
        }

    }
    @PostMapping("/camundaProcessInfo/getCamundaInfoById")
    public Result<CamundaProcessInfo> getCamundaInfoById(@RequestBody CamundaProcessInfo camundaProcessInfo){
        try {
            if (camundaProcessInfo == null) {
                return ResultGenerator.genFailResult("请求对象不能为空");
            }
            if (camundaProcessInfo.getId() == null) {
                return ResultGenerator.genFailResult("id不能为空");
            }
            CamundaProcessInfo result = camundaProcessInfoMapper.getCamundaInfoById(camundaProcessInfo.getId());
            return ResultGenerator.genSuccessResult(result);
        }catch (Exception e){
            logger.error("Error occurred while fetching all camunda process info", e);
            return ResultGenerator.genFailResult("Error occurred while fetching all camunda process info");
        }
    }

}
