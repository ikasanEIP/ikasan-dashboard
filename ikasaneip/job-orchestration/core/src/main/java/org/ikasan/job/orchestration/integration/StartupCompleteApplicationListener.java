package org.ikasan.job.orchestration.integration;

import com.arjuna.ats.arjuna.coordinator.TxControl;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.jta.JtaTransactionManager;

public class StartupCompleteApplicationListener implements ApplicationListener<ApplicationStartedEvent> {

    private Logger logger = LoggerFactory.getLogger(StartupCompleteApplicationListener.class);
    private Module<Flow> inboundFlowModule;

    private JtaTransactionManager transactionManager;

    public StartupCompleteApplicationListener(JtaTransactionManager transactionManager, Module<Flow> inboundFlowModule) {
        this.inboundFlowModule = inboundFlowModule;
        this.transactionManager = transactionManager;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        TxControl.setXANodeName("bigQueue");

        Flow inboundFlow = inboundFlowModule.getFlow("Scheduled Process Event Inbound Flow");
        inboundFlow.start();
    }
}