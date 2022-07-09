package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ContextTemplateVisualisationDialog extends AbstractCloseableResizableDialog {

    private Logger logger = LoggerFactory.getLogger(ContextTemplateVisualisationDialog.class);
    private VerticalLayout layout;

    private boolean initialised = false;

    private ContextTemplate rootContextTemplate;
    private ContextTemplate contextTemplate;
    private LogStreamingService logStreamingService;

    private String dynamicImagePath = ".";

    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobService schedulerJobService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private JobInitiationService jobInitiationService;

    private ContextService contextService = new ContextService();

    private SchedulerVisualisation schedulerVisualisation;

    public ContextTemplateVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                              LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                              JobInitiationService jobInitiationService) {
        this.setHeight("90%");
        this.setWidth("90%");

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("agent cannot be null!");
        }

        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if(this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }

        this.configurationRestService = configurationRestService;
        if(this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }

        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }

        this.metaDataRestService = metaDataRestService;
        if(this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }

        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }

        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.logStreamingService = logStreamingService;
        if(this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }

        this.jobInitiationService = jobInitiationService;
        if(this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
    }

    public void createSchedulerVisualisation(ContextTemplate rootContextTemplate, ContextTemplate contextTemplate) throws IOException {
        this.rootContextTemplate = rootContextTemplate;
        this.contextTemplate = contextTemplate;
        this.initialised = false;

        this.schedulerVisualisation = new SchedulerVisualisation(this.dynamicImagePath, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService,
            this.schedulerJobInstanceService, this.jobInitiationService);

        schedulerVisualisation.createSchedulerVisualisation(rootContextTemplate, contextTemplate, this);

        initParentNavigation();

        this.layout.add(schedulerVisualisation);
    }

    private void initParentNavigation() {
        if(!initialised && this.contextTemplate != null) {

            ContextTemplate parentContextTemplate = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

            if(parentContextTemplate != null) {
                Button gotoParentButton = new Button("Go to Parent - " + parentContextTemplate.getName(), VaadinIcon.ARROW_UP.create());
                gotoParentButton.setIconAfterText(true);
                gotoParentButton.addClickListener(buttonClickEvent -> {
                    if (this.contextTemplate != null && this.rootContextTemplate != null) {
                        this.contextTemplate = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

                        if (this.contextTemplate != null) {
                            try {
                                this.close();
                                ContextTemplateVisualisationDialog contextTemplateVisualisationDialog
                                    = new ContextTemplateVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService, this.configurationRestService
                                    , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService
                                    , this.schedulerJobInstanceService, this.jobInitiationService);

                                contextTemplateVisualisationDialog.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate);
                                contextTemplateVisualisationDialog.open();

                                this.close();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                layout.add(gotoParentButton);
                layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, gotoParentButton);
            }

            this.initialised = true;
        }
    }
}
