package com.autohome.springbootcamundademo.camunda;

import org.apache.catalina.User;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class ProcessDefinitionService {

    @Autowired
    private RepositoryService repositoryService;

    public void addListenersToUserTasks(String processDefinitionKey, String startListenerClass, String endListenerClass) {
        // 获取最新版本的流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .latestVersion()
                .singleResult();

        // 获取流程定义的BPMN模型
        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinition.getId());

        // 获取所有用户任务并为其添加监听器
        Collection<UserTask> userTasks = bpmnModelInstance.getModelElementsByType(UserTask.class);
        for (UserTask userTask : userTasks) {
            // 添加开始监听器
            userTask.builder().camundaExecutionListenerClass("start", startListenerClass);
            // 添加结束监听器
            userTask.builder().camundaExecutionListenerClass("end", endListenerClass);
        }

        // 将修改后的BPMN模型更新到流程定义中
        repositoryService.createDeployment()
                .addModelInstance(processDefinition.getKey() + ".bpmn", bpmnModelInstance)
                .deploy();
    }
}
