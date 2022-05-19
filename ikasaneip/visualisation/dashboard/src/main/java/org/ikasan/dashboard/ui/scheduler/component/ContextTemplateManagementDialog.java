package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;

public class ContextTemplateManagementDialog extends AbstractCloseableResizableDialog {
    private ContextTemplateManagementWidget contextTemplateManagementWidget;
    private ScheduledContextService scheduledContextService;

    public ContextTemplateManagementDialog(ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService, ContextTemplate contextTemplate) {
        this.scheduledContextService = scheduledContextService;
        this.contextTemplateManagementWidget = new ContextTemplateManagementWidget(scheduledContextService, scheduledContextInstanceService, dynamicImagePath
            , moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate);

        this.setHeight("95vh");
        this.setWidth("90vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.contextTemplateManagementWidget);
        layout.getStyle().set("padding-bottom", "20px");

        super.title.setText(String.format(getTranslation("label.context-template-management", UI.getCurrent().getLocale())));

        super.content.add(layout);
    }
}
