package org.ikasan.scheduler.integration;

import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.spec.dashboard.DashboardRestService;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

//@Component
public class StartupApplicationListener implements
    ApplicationListener<ContextRefreshedEvent> {

    private Logger logger = LoggerFactory.getLogger(StartupApplicationListener.class);

    private DashboardRestService moduleMetadataDashboardRestService;
    private DashboardRestService configurationMetadataDashboardRestService;
    private Module<Flow> inboundFlowModule;

    public StartupApplicationListener(DashboardRestService moduleMetadataDashboardRestService, DashboardRestService configurationMetadataDashboardRestService,
                                      Module<Flow> inboundFlowModule) {
        this.moduleMetadataDashboardRestService = moduleMetadataDashboardRestService;
        this.configurationMetadataDashboardRestService = configurationMetadataDashboardRestService;
        this.inboundFlowModule = inboundFlowModule;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.moduleMetadataDashboardRestService.publish(this.inboundFlowModule);
        this.configurationMetadataDashboardRestService.publish(this.inboundFlowModule);
    }
}