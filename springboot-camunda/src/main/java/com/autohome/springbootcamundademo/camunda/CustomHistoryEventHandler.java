package com.autohome.springbootcamundademo.camunda;

import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.CompositeDbHistoryEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CustomHistoryEventHandler extends CompositeDbHistoryEventHandler {
    private static Logger logger = LoggerFactory.getLogger(CustomHistoryEventHandler.class);
    @Override
    public void handleEvent(HistoryEvent historyEvent) {
        logger.info("custom history event handler, event: " + historyEvent.getEventType() + ", processInstanceId: " + historyEvent.getProcessInstanceId());
        super.handleEvent(historyEvent);
    }

    @Override
    public void handleEvents(List<HistoryEvent> list) {
        super.handleEvents(list);
    }
}
