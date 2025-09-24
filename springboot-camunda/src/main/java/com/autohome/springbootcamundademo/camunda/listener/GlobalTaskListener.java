package com.autohome.springbootcamundademo.camunda.listener;

import com.alibaba.fastjson2.JSON;
import org.camunda.bpm.engine.ProcessEngineServices;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceModificationBuilder;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class GlobalTaskListener implements TaskListener, ExecutionListener {
    private static Logger logger = LoggerFactory.getLogger(GlobalTaskListener.class);
    @Override
    public void notify(DelegateTask delegateTask) {

        logger.info("delegateTask------------------> " + delegateTask.getName()+"----------->"+delegateTask.getEventName()+"----->"+
                delegateTask.getBpmnModelElementInstance().getElementType().getTypeName()+"--->"+JSON.toJSONString(((TaskEntity)delegateTask).getPersistentState()));
        String nodeName = delegateTask.getTaskDefinitionKey();
        String variableName = nodeName + "Flag";
        if (delegateTask.getEventName().equals(EVENTNAME_COMPLETE)) {
            Boolean nodePassFlag = (Boolean) delegateTask.getVariable(variableName);

            if (nodePassFlag == null){
                return;
            }
            if (nodePassFlag) {
                logger.info("nodePassFlag is true");
            } else {
                String rejectVariable = "rejectNode";
                String rejectNode = (String) delegateTask.getVariable(rejectVariable);
                if (rejectNode != null){
                    logger.info("nodePassFlag is not null");

                    ProcessInstanceModificationBuilder modificationBuilder = rejectParallelNodeToSpecifyNode(delegateTask,rejectNode);
                    // 完成修改构建，同时开启目标活动
                    modificationBuilder.execute();
                    //删除当前任务对应的variable
                    delegateTask.removeVariable(variableName);
                    delegateTask.removeVariable(rejectVariable);
                }


            }
        }


    }

    /**
     * @param repositoryService
     * @param nodeId
     * @param processDefinitionId
     * @return
     */
    public List<String> getFirstTaskIdsOfParallelGatewayOrNodeItself(RepositoryService repositoryService, String nodeId, String processDefinitionId) {
        // 加载BPMN模型
        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);
        FlowNode node = bpmnModelInstance.getModelElementById(nodeId);
        if (node == null) {
            throw new RuntimeException("未找到节点ID为" + nodeId + "的节点");
        }

        // 判断节点是否为并行网关或连接到并行网关
        ParallelGateway parallelGateway = null;
        if (node instanceof ParallelGateway) {
            parallelGateway = (ParallelGateway) node;
        } else if (!node.getIncoming().isEmpty()) {
            // 获取入线（Incoming Flows）
            for (SequenceFlow incomingFlow : node.getIncoming()) {
                FlowNode sourceNode = incomingFlow.getSource();
                if (sourceNode instanceof ParallelGateway) {
                    parallelGateway = (ParallelGateway) sourceNode;
                    break;
                }else{
                    //一直向前查询直到返回一个并行网关
                    while (!(sourceNode instanceof ParallelGateway) && !sourceNode.getIncoming().isEmpty()) {
                        sourceNode = sourceNode.getIncoming().iterator().next().getSource();
                    }
                    if (sourceNode instanceof ParallelGateway) {
                        parallelGateway = (ParallelGateway) sourceNode;
                        break;
                    }
                }
            }
        }

        // 节点是并行网关或连接到并行网关
        if (parallelGateway != null) {
            return getParallelGatewayFirstTaskIds(parallelGateway,nodeId);
        } else {
            // 不是并行网关，返回节点本身ID
            return Collections.singletonList(nodeId);
        }
    }

    private List<String> getParallelGatewayFirstTaskIds(ParallelGateway parallelGateway,String nodeId) {
        // 获取并行网关的所有出口序列流
        Collection<SequenceFlow> outgoingFlows = parallelGateway.getOutgoing();

        List<String> nodeIds = new ArrayList<>();

        // 判断提供的节点ID是否在并行网关的子节点列表中

        for (SequenceFlow flow : outgoingFlows) {
            // 如果子节点列表包含提供的节点ID，则取提供的节点ID
            // 反之取第一个节点的ID
            FlowNode targetNode = flow.getTarget();
            while (!(targetNode instanceof UserTask) && !targetNode.getOutgoing().isEmpty()){
                targetNode = targetNode.getOutgoing().iterator().next().getTarget();
            }
            if (targetNode.getId().equals(nodeId)) {
                // 如果是并行网关的直接子节点
                nodeIds.add(nodeId);
            }else{
                // 如果是并行网关的间接子节点
                List<UserTask> list = targetNode.getSucceedingNodes().filterByType(UserTask.class).list();
                //获取当前节点的兄弟节点列表并判断是否包含当前节点
                if (!list.isEmpty()){
                    List<String> siblingNodeIds = list.stream().map(UserTask::getId).collect(Collectors.toList());
                    if (siblingNodeIds.contains(nodeId)){
                        nodeIds.add(nodeId);
                    }else {
                        nodeIds.add(siblingNodeIds.get(0));
                    }
                }else{
                    nodeIds.add(targetNode.getId());
                }
            }
        }

        return nodeIds;

    }

    private ProcessInstanceModificationBuilder rejectParallelNodeToSpecifyNode(DelegateTask delegateTask,String targetNodeId) {
        ProcessEngineServices processEngineServices = delegateTask.getProcessEngineServices();
        RuntimeService runtimeService = processEngineServices.getRuntimeService();
        String processInstanceId = delegateTask.getProcessInstanceId();

        // 创建修改构建器，开始准备修改命令
        ProcessInstanceModificationBuilder modificationBuilder = runtimeService
                .createProcessInstanceModification(processInstanceId);
        ActivityInstance[] childActivityInstances = runtimeService.getActivityInstance(processInstanceId)
                .getChildActivityInstances();
        if (childActivityInstances != null) {
            for (ActivityInstance childActivityInstance : childActivityInstances) {
                // 取消所有当前的子执行实例，除了触发退回操作的那个
                modificationBuilder.cancelAllForActivity(childActivityInstance.getActivityId());
            }
        }
        RepositoryService repositoryService = processEngineServices.getRepositoryService();
        List<String> relationNodeIds = getFirstTaskIdsOfParallelGatewayOrNodeItself(repositoryService, targetNodeId, delegateTask.getProcessDefinitionId());
        relationNodeIds.forEach(modificationBuilder::startBeforeActivity);
//        modificationBuilder.startBeforeActivity(targetNodeId);
        return modificationBuilder;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        logger.info("execution------------------> " + execution.getCurrentActivityName()+"----------->"+execution.getEventName()+"----->"+
                execution.getBpmnModelElementInstance().getElementType().getTypeName()+"---->"+ JSON.toJSONString(((ExecutionEntity)execution).getPersistentState()));

    }
}
