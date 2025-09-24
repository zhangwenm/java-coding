package com.autohome.springbootcamundademo.controller;

import com.alibaba.fastjson2.JSON;
import com.autohome.springbootcamundademo.mybatis.entity.camunda.CamundaProcessDefinition;
import com.autohome.springbootcamundademo.service.ProcessService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.event.EventType;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
public class CamundaTestController {
    private static Logger logger = LoggerFactory.getLogger(CamundaTestController.class);
    @Autowired
    private ProcessService processService;

    @GetMapping("/process-definitions")
    public ResponseEntity<Object> getProcessDefinitions() {
        List<CamundaProcessDefinition> processDefinitions = processService.getDefineList();
        return ResponseEntity.ok(processDefinitions);
    }
    @GetMapping("/camunda/startProcess")
    public ResponseEntity<String> startProcess(String processDefinationKey){
        processService.startProcess(processDefinationKey);

        return ResponseEntity.ok("process started");
    }

    @GetMapping("/camunda/startProcessByProcessKeyAndVersion")
    public ResponseEntity<String> startProcessByProcessKeyAndVersion(String processDefinationKey,int version){
        processService.startProcessInstanceBySpecificVersion(processDefinationKey,version);

        return ResponseEntity.ok("process started");
    }



    @GetMapping("/camunda/testAssignee")
    public ResponseEntity<String> testAssignee(String processInstanceId,String userId){
        ProcessInstance processInstance = processService.getProcessInstanceByProcessInstanceId(processInstanceId);
        logger.info("processInstance: " + processInstance.toString());
        if (processInstance == null) {
            return ResponseEntity.ok("processInstance not found");
        }
        if (processInstance.isEnded()) {
            return ResponseEntity.ok("processInstance is ended");
        }
        if (processInstance.isSuspended()) {
            return ResponseEntity.ok("processInstance is suspended");
        }
        List<HistoricTaskInstance> historicTaskInstances = processService.listHistoryTasks(processInstanceId);
        for (HistoricTaskInstance historyTask : historicTaskInstances) {
            HistoricTaskInstanceEntity historicTaskInstanceEntity = (HistoricTaskInstanceEntity) historyTask;
            Date endTime = historicTaskInstanceEntity.getEndTime();
            logger.info("history task name : {} ,desc : {}, ended at {}",historicTaskInstanceEntity.getName(),historicTaskInstanceEntity.getDescription(),endTime);
        }

        List<Task> taskList = processService.listCurrentTasks(processInstanceId);
        for (Task task : taskList) {
            TaskEntity taskEntity = (TaskEntity) task;
            processService.setAssignee(taskEntity,"demo");
//            processService.completeTask(taskEntity,"准入测试通过");
        }
        return ResponseEntity.ok("操作成功");
    }

    @GetMapping("/camunda/testGetExtensionProperties")
    public ResponseEntity<String> testGetExtensionProperties(String processInstanceId){
        ProcessInstance processInstance = processService.getProcessInstanceByProcessInstanceId(processInstanceId);
        logger.info("processInstance: " + processInstance.toString());
        if (processInstance == null) {
            return ResponseEntity.ok("processInstance not found");
        }
        if (processInstance.isEnded()) {
            return ResponseEntity.ok("processInstance is ended");
        }
        if (processInstance.isSuspended()) {
            return ResponseEntity.ok("processInstance is suspended");
        }
        return ResponseEntity.ok("操作成功");
    }

    @GetMapping("/camunda/testGetSiblingTask")
    public ResponseEntity<String> testGetSiblingTask(String processInstanceId){
        ProcessInstance processInstance = processService.getProcessInstanceByProcessInstanceId(processInstanceId);
        logger.info("processInstance: " + processInstance.toString());
        if (processInstance == null) {
            return ResponseEntity.ok("processInstance not found");
        }
        if (processInstance.isEnded()) {
            return ResponseEntity.ok("processInstance is ended");
        }
        if (processInstance.isSuspended()) {
            return ResponseEntity.ok("processInstance is suspended");
        }
        List<HistoricTaskInstance> historicTaskInstances = processService.listHistoryTasks(processInstanceId);
        for (HistoricTaskInstance historyTask : historicTaskInstances) {
            HistoricTaskInstanceEntity historicTaskInstanceEntity = (HistoricTaskInstanceEntity) historyTask;
            Date endTime = historicTaskInstanceEntity.getEndTime();
            logger.info("history task name : {} ,desc : {}, ended at {}",historicTaskInstanceEntity.getName(),historicTaskInstanceEntity.getDescription(),endTime);
        }

        List<Task> taskList = processService.listCurrentTasks(processInstanceId);

        return ResponseEntity.ok("操作成功");
    }
    @GetMapping("/camunda/testComplete")
    public ResponseEntity<String> testComplete(String processInstanceId){
        ProcessInstance processInstance = processService.getProcessInstanceByProcessInstanceId(processInstanceId);
        logger.info("processInstance: " + processInstance.toString());
        if (processInstance == null) {
            return ResponseEntity.ok("processInstance not found");
        }
        if (processInstance.isEnded()) {
            return ResponseEntity.ok("processInstance is ended");
        }
        if (processInstance.isSuspended()) {
            return ResponseEntity.ok("processInstance is suspended");
        }
        List<HistoricTaskInstance> historicTaskInstances = processService.listHistoryTasks(processInstanceId);
        for (HistoricTaskInstance historyTask : historicTaskInstances) {
            HistoricTaskInstanceEntity historicTaskInstanceEntity = (HistoricTaskInstanceEntity) historyTask;
            Date endTime = historicTaskInstanceEntity.getEndTime();
            logger.info("history task name : {} ,desc : {}, ended at {}",historicTaskInstanceEntity.getName(),historicTaskInstanceEntity.getDescription(),endTime);
        }

        List<Task> taskList = processService.listCurrentTasks(processInstanceId);
        for (Task task : taskList) {
            TaskEntity taskEntity = (TaskEntity) task;
            taskEntity.setAssignee("demo");
            processService.completeTask(taskEntity);
        }
        return ResponseEntity.ok("操作成功");
    }
    @GetMapping("/camunda/testCompleteWithExpression")
    public ResponseEntity<String> testCompleteWithExpression(String processInstanceId){
        ProcessInstance processInstance = processService.getProcessInstanceByProcessInstanceId(processInstanceId);
        if (processInstance == null) {
            return ResponseEntity.ok("processInstance not found");
        }
        if (processInstance.isEnded()) {
            return ResponseEntity.ok("processInstance is ended");
        }
        if (processInstance.isSuspended()) {
            return ResponseEntity.ok("processInstance is suspended");
        }
        List<HistoricTaskInstance> historicTaskInstances = processService.listHistoryTasks(processInstanceId);
        for (HistoricTaskInstance historyTask : historicTaskInstances) {
            HistoricTaskInstanceEntity historicTaskInstanceEntity = (HistoricTaskInstanceEntity) historyTask;
            Date endTime = historicTaskInstanceEntity.getEndTime();
            logger.info("history task name : {} ,desc : {}, ended at {}",historicTaskInstanceEntity.getName(),historicTaskInstanceEntity.getDescription(),endTime);
        }

        List<Task> taskList = processService.listCurrentTasks(processInstanceId);
        for (Task task : taskList) {
            TaskEntity taskEntity = (TaskEntity) task;
            processService.completeTaskWithSignal(taskEntity);
        }
        return ResponseEntity.ok("操作成功");
    }

    @GetMapping("/camunda/testRejectToParallel")
    public ResponseEntity<String> testRejectToParallel(String processInstanceId){
        ProcessInstance processInstance = processService.getProcessInstanceByProcessInstanceId(processInstanceId);
        logger.info("processInstance: " + processInstance.toString());
        if (processInstance == null) {
            return ResponseEntity.ok("processInstance not found");
        }
        if (processInstance.isEnded()) {
            return ResponseEntity.ok("processInstance is ended");
        }
        if (processInstance.isSuspended()) {
            return ResponseEntity.ok("processInstance is suspended");
        }
        List<HistoricTaskInstance> historicTaskInstances = processService.listHistoryTasks(processInstanceId);
        for (HistoricTaskInstance historyTask : historicTaskInstances) {
            HistoricTaskInstanceEntity historicTaskInstanceEntity = (HistoricTaskInstanceEntity) historyTask;
            Date endTime = historicTaskInstanceEntity.getEndTime();
            logger.info("history task name : {} ,desc : {}, ended at {}",historicTaskInstanceEntity.getName(),historicTaskInstanceEntity.getDescription(),endTime);
        }

        List<Task> taskList = processService.listCurrentTasks(processInstanceId);
        for (Task task : taskList) {
            logger.info("current task name : {} ,desc : {}, ended at {}",task.getName(),task.getDescription(),task.getCreateTime());

        }
        return ResponseEntity.ok("操作成功");
    }

    @GetMapping("/camunda/testAddEventListener")
    public ResponseEntity<String> testAddEventListener(String processDefineId, String node){
        return ResponseEntity.ok("操作成功");

    }
}
