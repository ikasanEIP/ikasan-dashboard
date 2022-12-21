package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerStatusDiv;
import org.ikasan.dashboard.ui.scheduler.listener.ContextOpenedListener;
import org.ikasan.dashboard.ui.scheduler.listener.ContextSelectedListener;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.designer.CanvasInitialisedListener;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class SplitContextInstanceVisualisation extends Div implements ContextOpenedListener, ContextSelectedListener, CanvasInitialisedListener {
    Logger logger = LoggerFactory.getLogger(SplitContextInstanceVisualisation.class);
    private Registration contextInstanceStateChangeRegistration;
    private Registration schedulerJobInstanceStateChangeRegistration;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    protected SchedulerInstanceVisualisation schedulerInstanceVisualisation;
    protected JobSchedulerInstanceVisualisation jobVisualisation;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ModuleMetaDataService moduleMetaDataService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private LogStreamingService logStreamingService;
    private SplitLayout visualisationSplitLayout;
    private ContextInstance contextInstance;
    private ContextInstance childContextInstance;
    private SchedulerStatusDiv childJobPlansStatusDiv;
    private Label childJobPlanName;
    private IkasanAuthentication authentication;

    private boolean initialised = false;

    /**
     * Constructor
     *
     * @param scheduledContextInstanceService
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param logStreamingService
     * @param contextInstance
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param contextProfileService
     * @param jobUtilsService
     * @param scheduledContextService
     */
    public SplitContextInstanceVisualisation(ScheduledContextInstanceService scheduledContextInstanceService, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, LogStreamingService logStreamingService, ContextInstance contextInstance, SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                 ContextProfileService contextProfileService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if (this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if (this.contextInstance == null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if (this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.configurationRestService = configurationRestService;
        if (this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if (this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.moduleControlRestService = moduleControlRestService;
        if (this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if (this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }
        this.metaDataRestService = metaDataRestService;
        if (this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if (this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if (this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if (this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Initial the visualisation associated with the widget.
     */
    public void initialiseVisualisation() {
        if(!initialised) {
            this.setSizeFull();

            if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext();
            }

            this.schedulerInstanceVisualisation = new ContextSchedulerInstanceVisualisation("", this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService
                , this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.contextProfileService);
            this.schedulerInstanceVisualisation.addContextOpenListener(this);
            this.schedulerInstanceVisualisation.setWidthFull();
            this.schedulerInstanceVisualisation.setHeight("100%");

            try {
                ContextProfileSearchFilter searchFilter = new SolrContextProfileSearchFilterImpl();
                searchFilter.setContextName(this.contextInstance.getName());
                searchFilter.setOwner(ContextProfileRecord.SYSTEM_OWNER);

//                SearchResults<ContextProfileRecord> results = this.contextProfileService.findByFilter(searchFilter, -1, -1, null, null);
//
//                if (results.getResultList().size() > 0 && results.getResultList().get(0).getContextProfile().getDefaultContext() != null
//                    && !results.getResultList().get(0).getContextProfile().getDefaultContext().isEmpty()) {
//                    ContextInstance childContextInstance = ContextHelper.getChildContextInstance(results.getResultList()
//                        .get(0).getContextProfile().getDefaultContext(), this.contextInstance);
//
//                    this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance, childContextInstance, null);
//                } else {
//                    this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance, this.contextInstance, null);
//                }

                this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance, this.contextInstance, null);

                this.schedulerInstanceVisualisation.addCanvasInitialisedListener(this);

                this.visualisationSplitLayout = new SplitLayout();
                this.visualisationSplitLayout.setHeight("100%");
                this.visualisationSplitLayout.setWidthFull();
                this.visualisationSplitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
                this.visualisationSplitLayout.getElement().getStyle().set("margin-bottom", "5px");

                this.jobVisualisation = new JobSchedulerInstanceVisualisation("", moduleMetaDataService, scheduledProcessManagementService,
                    configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, logStreamingService
                    , this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService);
                this.jobVisualisation.addContextOpenListener(this);
                this.jobVisualisation.addContextSelectedListener(this);
                this.jobVisualisation.setWidthFull();
                this.jobVisualisation.createSchedulerVisualisation(this.contextInstance, this.contextInstance, null);
                this.jobVisualisation.setHeight("100%");
                this.jobVisualisation.setVisible(false);

                Button upButton = new Button();
                Button downButton = new Button();
                Button middleButton = new Button();

                upButton.getElement().appendChild(VaadinIcon.ARROW_UP.create().getElement());
                upButton.addClickListener(event -> {
                    visualisationSplitLayout.setSplitterPosition(0);
                    this.jobVisualisation.setVisible(true);
                });

                middleButton.getElement().appendChild(VaadinIcon.LINE_H.create().getElement());
                middleButton.addClickListener(event -> {
                    visualisationSplitLayout.setSplitterPosition(50);
                    this.jobVisualisation.setVisible(true);
                });

                downButton.getElement().appendChild(VaadinIcon.ARROW_DOWN.create().getElement());
                downButton.addClickListener(event -> {
                    visualisationSplitLayout.setSplitterPosition(97);
                    this.jobVisualisation.setVisible(false);
                });

                HorizontalLayout splitLayoutManagerButtonLayout = new HorizontalLayout();
                splitLayoutManagerButtonLayout.getStyle().set("position", "absolute");
                splitLayoutManagerButtonLayout.getStyle().set("right", "10px");

                childJobPlanName = new Label();
                childJobPlanName.getElement().getStyle().set("margin-top", "10px");
                childJobPlanName.setVisible(false);

                HorizontalLayout labelLayout = new HorizontalLayout();
                labelLayout.getStyle().set("position", "absolute");
                labelLayout.getStyle().set("left", "80px");
                labelLayout.add(childJobPlanName);
                labelLayout.setWidthFull();

                this.childJobPlansStatusDiv = new SchedulerStatusDiv();
                this.childJobPlansStatusDiv.setHeight("20px");
                this.childJobPlansStatusDiv.setWidth("800px");
                this.childJobPlansStatusDiv.getElement().getStyle().set("font-size", "12pt");
                childJobPlansStatusDiv.getStyle().set("position", "absolute");
                childJobPlansStatusDiv.getStyle().set("left", "50%");
                childJobPlansStatusDiv.getStyle().set("margin-left", "-500px");

                labelLayout.add(this.childJobPlansStatusDiv);

                HorizontalLayout wrapper = new HorizontalLayout();
                wrapper.add(labelLayout, splitLayoutManagerButtonLayout);

                splitLayoutManagerButtonLayout.add(upButton, middleButton, downButton);
                VerticalLayout jobVisLayout = new VerticalLayout();
                jobVisLayout.setSpacing(false);
                jobVisLayout.setMargin(false);
                jobVisLayout.setPadding(false);
                jobVisLayout.add(wrapper, jobVisualisation);

                this.visualisationSplitLayout.setSplitterPosition(95);
                this.visualisationSplitLayout.addToPrimary(this.schedulerInstanceVisualisation);
                this.visualisationSplitLayout.addToSecondary(jobVisLayout);

                this.visualisationSplitLayout.addSplitterDragendListener(event -> {
                    jobVisualisation.setVisible(true);
                });

                this.add(this.visualisationSplitLayout);

                this.setVisible(false);
            } catch (IOException e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("notification.error-opening-visualisation"
                    , UI.getCurrent().getLocale()));
            }
        }
        initialised = true;
    }

    @Override
    public void contextOpened(Context context) {
        try {
            this.childContextInstance = (ContextInstance) context;
            this.jobVisualisation.createSchedulerVisualisation(this.contextInstance
                , this.childContextInstance, null);
            this.jobVisualisation.setWidthFull();
            this.jobVisualisation.setVisible(true);
            this.visualisationSplitLayout.setSplitterPosition(50);
            this.childJobPlanName.setVisible(true);
            this.childJobPlanName.setText(context.getName());

            this.childJobPlansStatusDiv.setStatus(((ContextInstance) context).getStatus());
        } catch (IOException e) {
            logger.error(String.format("An error has occurred opening context [%s]", context), e);
            NotificationHelper.showErrorNotification(getTranslation("error.could-not-open-job-plan"
                , UI.getCurrent().getLocale()));
        }
    }

    @Override
    public void contextSelected(String contextName) {
        this.schedulerInstanceVisualisation.addBoundaryToItem(contextName, true);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if (contextInstanceStateChangeEvent.getContextInstance() != null) {
                if(ui.isAttached()) {
                    ui.access(() -> {
                        if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                            this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext();
                        }

                        if (this.childContextInstance != null && this.childContextInstance.getId().equals(contextInstanceStateChangeEvent.getContextInstance().getId())) {
                            this.childJobPlansStatusDiv.setStatus(contextInstanceStateChangeEvent.getNewStatus());
                        }
                    });
                }
            }
        });

        schedulerJobInstanceStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if(ui.isAttached()) {
                ui.access(() -> {
                    if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                        this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext();
                    }
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

        if(this.schedulerJobInstanceStateChangeRegistration != null) {
            this.schedulerJobInstanceStateChangeRegistration.remove();
            this.schedulerJobInstanceStateChangeRegistration = null;
        }
    }

    @Override
    public void canvasInitialised() {
        if(this.schedulerInstanceVisualisation != null && this.childContextInstance != null) {
            this.schedulerInstanceVisualisation.addBoundaryToItem(this.childContextInstance.getName(), true);
        }
    }
}
