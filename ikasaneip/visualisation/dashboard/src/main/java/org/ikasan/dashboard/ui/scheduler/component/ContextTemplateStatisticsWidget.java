package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;

public class ContextTemplateStatisticsWidget extends Div {

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    /**
     * Constructor
     */
    public ContextTemplateStatisticsWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService, ContextTemplate contextTemplate) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");


        div.add(new H1("Under Construction"));

        this.add(div);
        this.setSizeFull();
    }

}
