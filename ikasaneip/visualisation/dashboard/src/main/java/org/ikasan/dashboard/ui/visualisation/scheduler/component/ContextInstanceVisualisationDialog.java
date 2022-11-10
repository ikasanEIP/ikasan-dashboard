package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerStatusDiv;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ContextInstanceVisualisationDialog extends AbstractCloseableResizableDialog {

    private Logger logger = LoggerFactory.getLogger(ContextInstanceVisualisationDialog.class);

    private Registration contextInstanceStateChangeRegistration;

    private VerticalLayout layout;

    private boolean initialised = false;

    private ContextInstance rootContextInstance;
    private ContextInstance contextInstance;
    private LogStreamingService logStreamingService;

    private String dynamicImagePath = ".";

    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private JobInitiationService jobInitiationService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;

    private SchedulerInstanceVisualisation schedulerInstanceVisualisation;

    private ContextService contextService = new ContextService();

    private SchedulerStatusDiv statusDiv;

    public ContextInstanceVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                              LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                              JobInitiationService jobInitiationService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService) {
        this.setHeight("98vh");
        this.setWidth("98vw");

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

        this.jobUtilsService = jobUtilsService;
        if(this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.layout = new VerticalLayout();
        this.layout.getStyle().set("padding-top", "0px");

        this.layout.setSizeFull();
        super.content.add(this.layout);
    }

    /**
     * @param contextInstance
     */
    public void createSchedulerVisualisation(ContextInstance rootContextInstance, ContextInstance contextInstance) throws IOException {
        this.rootContextInstance = rootContextInstance;
        this.contextInstance = contextInstance;

        this.initialised = false;

        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.setStatus(this.contextInstance.getStatus());

        this.layout.add(this.statusDiv);

        this.initParentNavigation();

        this.schedulerInstanceVisualisation =  new ContextSchedulerInstanceVisualisation(this.dynamicImagePath, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
            this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService);
        this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.rootContextInstance, this.contextInstance, this);

        this.layout.add(schedulerInstanceVisualisation);
    }

    private void initParentNavigation() {
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
                                    , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService
                                    , this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService);

                                contextInstanceVisualisationDialog.createSchedulerVisualisation(this.rootContextInstance, this.contextInstance);
                                contextInstanceVisualisationDialog.open();

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

            super.title.setText(this.contextInstance.getName());
            this.initialised = true;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if (contextInstanceStateChangeEvent.getContextInstance() != null &&
                contextInstanceStateChangeEvent.getContextInstance().getName().equals(this.contextInstance.getName())) {
                ui.access(() -> {
                    this.statusDiv.setStatus(contextInstanceStateChangeEvent.getNewStatus());
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.contextInstanceStateChangeRegistration != null) {
            this.contextInstanceStateChangeRegistration.remove();
            this.contextInstanceStateChangeRegistration = null;
        }
    }
}
