package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;

public class SchedulerJobSelectDialog extends AbstractCloseableResizableDialog {
    private SchedulerJobSelectGridWidget contextTemplateManagementWidget;

    public SchedulerJobSelectDialog(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                    ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                    MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                    LogStreamingService logStreamingService, ContextTemplate contextTemplate, JobInitiationService jobInitiationService,
                                    JobProvisionService jobProvisionService) {
        this.contextTemplateManagementWidget = new SchedulerJobSelectGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, contextTemplate,
            jobInitiationService, jobProvisionService, this);

        this.setHeight("90vh");
        this.setWidth("90vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.contextTemplateManagementWidget);
        layout.getStyle().set("padding-bottom", "20px");

        super.content.add(layout);
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.contextTemplateManagementWidget.addSchedulerJobSelectedListener(listener);
    }

    public void removeSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.contextTemplateManagementWidget.removeSchedulerJobSelectedListener(listener);
    }
}
