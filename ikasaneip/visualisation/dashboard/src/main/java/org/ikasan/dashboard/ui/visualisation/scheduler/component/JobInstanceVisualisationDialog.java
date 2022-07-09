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
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JobInstanceVisualisationDialog extends AbstractCloseableResizableDialog {

    private Logger logger = LoggerFactory.getLogger(JobInstanceVisualisationDialog.class);

    private VerticalLayout layout;

    private boolean initialised = false;

    private ContextInstance rootContextInstance;
    private ContextInstance contextInstance;

    private String dynamicImagePath = ".";

    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobService schedulerJobService;
    private LogStreamingService logStreamingService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private JobInitiationService jobInitiationService;

    private ContextService contextService = new ContextService();

    private SchedulerInstanceVisualisation schedulerInstanceVisualisation;

    public JobInstanceVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                          SchedulerJobService schedulerJobService, LogStreamingService logStreamingService,
                                          SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService) {
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

    /**
     * @param contextInstance
     */
    public void createSchedulerVisualisation(ContextInstance rootContextInstance, ContextInstance contextInstance) throws IOException {
        this.rootContextInstance = rootContextInstance;
        this.contextInstance = contextInstance;
        this.initialised = false;

        initParentNavigation();

        this.schedulerInstanceVisualisation = new SchedulerInstanceVisualisation(this.dynamicImagePath, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService,
            this.schedulerJobInstanceService, this.jobInitiationService);

        this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.rootContextInstance, this.contextInstance, this);

        this.layout.add(this.schedulerInstanceVisualisation);
    }

    private void initParentNavigation() throws IOException{
        if(!initialised && this.contextInstance != null) {
            ContextInstance parentContextInstance = contextService.getParent(this.rootContextInstance, this.contextInstance);

            if(parentContextInstance != null) {
                Button gotoParentButton = new Button("Go to Parent - " + parentContextInstance.getName(), VaadinIcon.ARROW_UP.create());
                gotoParentButton.setIconAfterText(true);
                gotoParentButton.addClickListener(buttonClickEvent -> {
                    if (this.contextInstance != null && this.rootContextInstance != null) {
                        this.contextInstance = contextService.getParent(this.rootContextInstance, this.contextInstance);

                        if (this.contextInstance != null) {
                            try {
                                this.close();
                                ContextInstanceVisualisationDialog contextInstanceVisualisationDialog
                                    = new ContextInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService, this.configurationRestService
                                    , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService
                                    , this.schedulerJobInstanceService, this.jobInitiationService);

                                contextInstanceVisualisationDialog.createSchedulerVisualisation(this.rootContextInstance, this.contextInstance);
                                contextInstanceVisualisationDialog.open();

                                this.close();
                            }
                            catch (IOException e) {
                                // todo notification message
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
