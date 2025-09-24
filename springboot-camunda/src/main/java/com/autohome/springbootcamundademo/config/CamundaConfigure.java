package com.autohome.springbootcamundademo.config;

import com.autohome.springbootcamundademo.camunda.CustomHistoryEventHandler;
import com.autohome.springbootcamundademo.camunda.listener.GlobalBpmnParseListener;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CamundaConfigure {

    @Bean
    public HistoryEventHandler customHistoryEventHandler() {
        return new CustomHistoryEventHandler();
    }

    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(@Qualifier("camundaBpmnDataSource") DataSource dataSource,@Qualifier("camundaBpmTransactionManager") PlatformTransactionManager transactionManager) {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setHistoryLevel(HistoryLevel.HISTORY_LEVEL_FULL);
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        // 其他配置...
        BpmnParseListener globalListener = new GlobalBpmnParseListener();
        List<BpmnParseListener> customPostBPMNParseListeners = configuration.getCustomPreBPMNParseListeners();
        if (customPostBPMNParseListeners == null) {
            customPostBPMNParseListeners = new ArrayList<>();
        }
        customPostBPMNParseListeners.add(globalListener);
        configuration.setCustomPreBPMNParseListeners(customPostBPMNParseListeners);
        return configuration;
    }
}
