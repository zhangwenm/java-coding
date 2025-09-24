package com.autohome.springbootcamundademo.service;

import com.autohome.springbootcamundademo.mybatis.entity.camunda.CamundaProcessDefinition;
import jakarta.annotation.PostConstruct;
import org.camunda.bpm.application.PostDeploy;
import org.camunda.bpm.application.ProcessApplicationInfo;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProcessService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessService.class);


    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ProcessEngine processEngine;

    private static ConcurrentHashMap<String,Collection<CamundaProperty>> CAMUNDA_PROCESSDEFINITION_VERSION_NODE_PROPERTIES = new ConcurrentHashMap<>();

    public void startProcessInstanceBySpecificVersion(String processDefinitionKey, int version) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .processDefinitionVersion(version)
                .singleResult();

        if (processDefinition != null) {
            String processDefinitionId = processDefinition.getId();
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId);
            logger.info("startProcess, processDefinitionKey: " + processDefinitionKey + ", processInstanceId: " + processInstance.getProcessInstanceId());
            // 更多的业务逻辑...
        } else {
            // 处理错误情况，例如抛出异常或返回错误信息
            throw new RuntimeException("No process definition found for key='" + processDefinitionKey +
                    "' and version='" + version + "'");
        }
    }

    public void startProcess(String processDefinitionKey) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);


        logger.info("startProcess, processDefinitionKey: " + processDefinitionKey + ", processInstanceId: " + processInstance.getProcessInstanceId());
    }
    public ProcessInstance getProcessInstanceByProcessInstanceId(String processInstanceId) {
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).list();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public List<HistoricTaskInstance> listHistoryTasks(String processInstanceId) {
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).finished().list();
        return list;
    }


    public List<Task> listCurrentTasks(String processInstanceId) {
        List<Task> list = taskService.createTaskQuery().processInstanceId(processInstanceId).active().list();
        return list;
    }


    public void completeTask(TaskEntity taskEntity) {
        //taskService.claim(taskEntity.getId(), "demo");
        String name = taskEntity.getName();
        if ("测试准入".equals(name)){
            taskService.complete(taskEntity.getId(),Collections.singletonMap("accessTest",true));
        }else if ("功能测试".equals(name)){
            taskService.complete(taskEntity.getId(),Collections.singletonMap("functionTest",true));
        }else{
            taskService.complete(taskEntity.getId());
        }

    }

    public void setAssignee(TaskEntity taskEntity, String userId) {
        taskService.setAssignee(taskEntity.getId(), userId);

    }

    public void completeTaskWithSignal(TaskEntity taskEntity) {
        // 完成任务并选择要跳转的Sequence Flow
        taskService.complete(taskEntity.getId(), Collections.singletonMap("task1Approve",true));
    }

    public List<CamundaProcessDefinition> getDefineList() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        List<CamundaProcessDefinition> customCamundaProcessDefinitions = new ArrayList<>();
        for (ProcessDefinition processDefinition : processDefinitions) {
            CamundaProcessDefinition definition = new CamundaProcessDefinition();
            definition.setId(processDefinition.getId());
            int version = processDefinition.getVersion();
            definition.setVersion(version);
            String key = processDefinition.getKey();
            definition.setKey(key);

            InputStream inputStream = repositoryService.getProcessModel(processDefinition.getId());
            String bpmnXml = convertStreamToString(inputStream);
            definition.setBpmnXml(bpmnXml);
            customCamundaProcessDefinitions.add(definition);
        }
        return customCamundaProcessDefinitions;
    }

    private String convertStreamToString(InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public List<Task> getSiblingTasks(TaskEntity taskEntity, String nodeName) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(taskEntity.getProcessInstanceId()).list();
        List<Task> siblingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.getName().equals(nodeName)) {
                siblingTasks.add(task);
            }
        }
        return siblingTasks;
    }




}
