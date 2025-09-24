package com.autohome.springbootcamundademo.camunda.listener;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class StartEndListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if ("start".equals(execution.getEventName())) {
            // 开始事件逻辑
            System.out.println("流程实例开始：" + execution.getProcessInstanceId());
        } else if ("end".equals(execution.getEventName())) {
            // 结束事件逻辑
            System.out.println("流程实例结束：" + execution.getProcessInstanceId());
        }
    }
}
