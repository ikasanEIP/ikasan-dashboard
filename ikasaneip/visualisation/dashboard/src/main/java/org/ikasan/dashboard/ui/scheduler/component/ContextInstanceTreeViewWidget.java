package org.ikasan.dashboard.ui.scheduler.component;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.componentfactory.explorer.ExplorerTreeGrid;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.apache.commons.lang3.time.StopWatch;
import org.ikasan.dashboard.ui.administration.component.SystemEventDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobInstanceSplitVisualisationDialog;
import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.AggregateContextInstanceStatus;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.orchestration.service.context.local.LocalEventServiceImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.LocalEventService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.systemevent.model.SolrSystemEventSearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cronutils.model.CronType.QUARTZ;
import static org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl.SCHEDULED_CONTEXT_INSTANCE;

public class ContextInstanceTreeViewWidget extends AbstractGridSchedulerJobInstanceActionWidget
    implements ContextInstanceStateChangeEventBroadcastListener, SchedulerJobStateChangeEventBroadcastListener,
    ContextInstanceSavedEventBroadcastListener {
    private static final String PRECEDING_ITEM_COMPONENT = "PRECEDING_ITEM_COMPONENT";
    private static final String SKIP_ICON = "SKIP_ICON";
    private static final String ENABLE_ICON = "ENABLE_ICON";
    private static final String HOLD_ICON = "HOLD_ICON";
    private static final String RELEASE_ICON = "RELEASE_ICON";
    private static final String SUBMIT_ICON = "SUBMIT_ICON";
    private static final String LOG_ICON = "LOG_ICON";
    private static final String ERROR_LOG_ICON = "ERROR_LOG_ICON";
    private static final String LOG_FILE_HISTORY = "LOG_FILE_HISTORY";
    private static final String EVENT_ICON = "EVENT_ICON";
    private static final String EXECUTION_ENVIRONMENT_ICON = "EXECUTION_ENVIRONMENT_ICON";
    private static final String RESET_JOB_ICON = "RESET_JOB_ICON";
    private static final String ACKNOWLEDGE_ERROR_JOB_ICON = "ACKNOWLEDGE_ERROR_JOB_ICON";
    private static final String SUBMIT_DOWNSTREAM_JOBS_ICON = "SUBMIT_DOWNSTREAM_JOBS_ICON";
    private Logger logger = LoggerFactory.getLogger(ContextInstanceTreeViewWidget.class);
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private LogStreamingService logStreamingService;
    private JobInitiationService jobInitiationService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private SystemEventSearchService systemEventSearchService;
    private ContextInstance contextInstance;
    private Map<ComponentKey, Image> jobImageMap;
    private Map<ComponentKey, Map<String, Icon>> schedulerJobIconMap;
    private Map<ComponentKey, SchedulerStatusDiv> statusDivMap;
    private Map<ComponentKey, Icon> errorAcknowledgedMap;
    private Map<ComponentKey, SchedulerStatusIconDiv> statusIconDivMap;
    private Map<ComponentKey, InstanceStatus> instanceStatusMap;
    private Map<ComponentKey, SchedulerStatusFreeTextDiv> schedulerStatusFreeTextDivMap;
    private Map<ComponentKey, Div> startTimes;
    private Map<ComponentKey, Div> endTimes;
    private Map<ComponentKey, Button> manuallySubmittedBy;
    private TreeGrid<Object> grid;
    private CronParser parser;
    private CronDescriptor descriptor;
    private ArrayList<Object> expandedNodes;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextProfileService contextProfileService;
    private GlobalEventService globalEventService;
    private LocalEventService localEventService;
    private UI ui;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    private VaadinSession vaadinSession;

    /**
     * Constructor
     *
     * @param contextInstance
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param logStreamingService
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param jobUtilsService
     * @param scheduledContextService
     */
    public ContextInstanceTreeViewWidget(ContextInstance contextInstance, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                         ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                         SystemEventLogger systemEventLogger, LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                         JobInitiationService jobInitiationService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                         ScheduledContextInstanceService scheduledContextInstanceService, ContextProfileService contextProfileService, GlobalEventService globalEventService, SystemEventSearchService systemEventSearchService,
                                         double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        super(moduleMetaDataService, systemEventLogger, logStreamingService, contextInstance,
             schedulerJobInstanceService);
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
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.globalEventService = globalEventService;
        if (this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.systemEventSearchService = systemEventSearchService;
        if (this.systemEventSearchService == null) {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }

        this.localEventService = new LocalEventServiceImpl();

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;

        this.jobImageMap = new ConcurrentHashMap<>();
        this.schedulerJobIconMap = new ConcurrentHashMap<>();
        this.statusDivMap = new ConcurrentHashMap<>();
        this.errorAcknowledgedMap = new ConcurrentHashMap<>();
        this.statusIconDivMap = new ConcurrentHashMap<>();
        this.instanceStatusMap = new ConcurrentHashMap<>();
        this.schedulerStatusFreeTextDivMap = new ConcurrentHashMap<>();
        this.startTimes = new ConcurrentHashMap<>();
        this.endTimes = new ConcurrentHashMap<>();
        this.manuallySubmittedBy = new ConcurrentHashMap<>();
        this.expandedNodes = new ArrayList<>();

        parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
        descriptor = CronDescriptor.instance(Locale.UK);

        setSizeFull();
        this.buildGrid();

        Button collapse = new Button(getTranslation("button.collapse", UI.getCurrent().getLocale()), VaadinIcon.COMPRESS.create());
        collapse.setIconAfterText(true);
        collapse.getElement().getStyle().set("margin-left", "auto");
        collapse.addClickListener(event -> {
            this.grid.collapseRecursively(this.expandedNodes, 1);
        });

        Button refreshTreeButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshTreeButton.setIconAfterText(true);
        refreshTreeButton.addClickListener(event -> {
            if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext();
            }
            this.grid.getDataProvider().refreshAll();
        });


        Div div = new Div();
        div.setSizeFull();

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(collapse, refreshTreeButton);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, collapse);

        div.add(layout);
        div.add(this.grid);

        super.add(div);
        super.setSizeFull();

        this.vaadinSession = VaadinSession.getCurrent();
    }

    /**
     * Helper method to build the underlying grid.
     */
    private void buildGrid() {
        grid = new ExplorerTreeGrid<>();

        Map<String, InternalEventDrivenJob> internalEventDrivenJobInstanceMap
            = this.getCommandExecutionJobsForContextInstance(contextInstance.getId());

        Map<String, SchedulerJob> schedulerJobMap
            = this.getAllSchedulerJobInstancesForContextInstance(contextInstance.getId());

        Map<String, QuartzScheduleDrivenJobInstance> quartzSchedulerJobMap
            = this.getQuartzSchedulerJobInstancesForContextInstance(contextInstance.getId());

        ContextHelper.enrichJobs(this.contextInstance, schedulerJobMap);

        grid.addComponentHierarchyColumn(value -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            if(value instanceof ContextInstance) {

                SchedulerStatusFreeTextDiv schedulerStatusFreeTextDiv
                    = new SchedulerStatusFreeTextDiv();
                schedulerStatusFreeTextDiv.setWidthFull();
                schedulerStatusFreeTextDiv.getElement().getStyle().set("font-size", "10pt");
                schedulerStatusFreeTextDiv.getElement().getStyle().set("margin-top", "1px");
                schedulerStatusFreeTextDiv.getElement().getStyle().set("padding-left", "50px");
                schedulerStatusFreeTextDiv.getElement().getStyle().set("margin-bottom", "1px");
                schedulerStatusFreeTextDiv.getElement().getStyle().set("padding-right", "50px");

                ComponentKey componentKey = new ComponentKey(contextInstance.getName()
                    , ((ContextInstance)value).getId(), ((ContextInstance)value).getName());
                if(ContextMachineCache.instance().containsInstanceIdentifier(contextInstance.getId())) {
                    value = ContextHelper.getChildContextInstance(((ContextInstance) value).getName(),
                        ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext());
                }
                if(this.instanceStatusMap.containsKey(componentKey)) {
                    if(!this.instanceStatusMap.get(componentKey).equals(((ContextInstance) value).getStatus())) {
                        this.instanceStatusMap.put(componentKey, ((ContextInstance) value).getStatus());
                    }
                    schedulerStatusFreeTextDiv.setStatus(this.instanceStatusMap.get(componentKey)
                        , ((ContextInstance) value).getName());
                }
                else {
                    schedulerStatusFreeTextDiv.setStatus(((ContextInstance) value).getStatus()
                        , ((ContextInstance) value).getName());
                }
                horizontalLayout.add(schedulerStatusFreeTextDiv);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, schedulerStatusFreeTextDiv);
                horizontalLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

                this.schedulerStatusFreeTextDivMap.put(componentKey, schedulerStatusFreeTextDiv);
            }
            else if(value instanceof SchedulerJobInstance) {
                SchedulerJobInstance schedulerJobInstance = (SchedulerJobInstance)value;
                SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId()
                    , ((SchedulerJobInstance)value).getJobName(), ((SchedulerJobInstance)value).getChildContextName());

                if(schedulerJobInstanceRecord != null) {
                    schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
                }
                else {
                    logger.info("Could not load job instance record for job[{}], context[{}], child context[{}]"
                        , schedulerJobInstance.getJobName(), schedulerJobInstance.getContextName(), schedulerJobInstance.getChildContextName());
                }

                boolean isTargetResidingContextOnly =
                internalEventDrivenJobInstanceMap.containsKey(schedulerJobInstance.getIdentifier()+"-"+schedulerJobInstance.getChildContextName()) &&
                    internalEventDrivenJobInstanceMap.get(schedulerJobInstance.getIdentifier()+"-"+schedulerJobInstance.getChildContextName()).isTargetResidingContextOnly();

                if (!isTargetResidingContextOnly && ContextHelper.determineIfJobsTransitionFromOtherContexts(contextInstance, schedulerJobInstance.getJobName()
                    , schedulerJobInstance.getChildContextName(), schedulerJobMap).size() > 0) {
                    horizontalLayout.add(VaadinIcon.ARROW_RIGHT.create());
                }

                Image image = new Image(this.getJobImage(schedulerJobInstance), "");
                horizontalLayout.add(image);
                image.setHeight("30px");
                this.setImageBackgroundColour(image, schedulerJobInstance);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, image);

                this.jobImageMap.put(new ComponentKey(schedulerJobInstance instanceof GlobalEventJobInstance ? JobConstants.GLOBAL_EVENT : this.contextInstance.getName()
                    , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName()), image);


                Label jobNameLabel =  new Label(((SchedulerJobInstance)value).getJobName());
                if(this.contextInstance.isUseDisplayName() && ((SchedulerJobInstance)value).getDisplayName() != null && !((SchedulerJobInstance)value).getDisplayName().isEmpty()) {
                    Label jobDisplayNameLabel =  new Label(((SchedulerJobInstance)value).getDisplayName());
                    horizontalLayout.add(jobDisplayNameLabel);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, jobNameLabel);
                }
                else {
                    horizontalLayout.add(jobNameLabel);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, jobNameLabel);
                }

                if(internalEventDrivenJobInstanceMap.containsKey(schedulerJobInstance.getIdentifier()+"-"+schedulerJobInstance.getChildContextName()) &&
                    internalEventDrivenJobInstanceMap.get(schedulerJobInstance.getIdentifier()+"-"+schedulerJobInstance.getChildContextName()).isJobRepeatable()) {
                    Image repeatable = new Image("frontend/images/repeating.png", "");
                    horizontalLayout.add(repeatable);
                    repeatable.setHeight("20px");
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, repeatable);
                }

            }
            else if(value instanceof PrecedingItem) {
                SchedulerJobInstance schedulerJobInstance = ((PrecedingItem)value).schedulerJobInstance;
                SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId()
                    , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                if(schedulerJobInstanceRecord != null) {
                    schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
                }

                Icon arrow = VaadinIcon.ARROW_RIGHT.create();
                Image image = new Image(this.getJobImage(schedulerJobInstance), "");
                horizontalLayout.add(arrow, image);
                image.setHeight("30px");
                this.setImageBackgroundColour(image, schedulerJobInstance);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, image);

                this.jobImageMap.put(new ComponentKey(PRECEDING_ITEM_COMPONENT+this.contextInstance.getName()
                    , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName()), image);

                Label jobNameLabel =  new Label(schedulerJobInstance.getJobName());
                if(this.contextInstance.isUseDisplayName() && schedulerJobInstance.getDisplayName() != null && !schedulerJobInstance.getDisplayName().isEmpty()) {
                    Label jobDisplayNameLabel =  new Label((schedulerJobInstance.getDisplayName()
                        + " - " + schedulerJobInstance.getJobName()));
                    horizontalLayout.add(jobDisplayNameLabel);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, jobNameLabel);
                }
                else {
                    horizontalLayout.add(jobNameLabel);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, jobNameLabel);
                }
            }

            return horizontalLayout;
        })
            .setFlexGrow(7)
            .setHeader(getTranslation("table-header.context-job-name", UI.getCurrent().getLocale()))
            .setKey("name")
            .setResizable(true);

        grid.addComponentColumn(value -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidth("300px");
            if (value instanceof ContextInstance) {
                this.getContextInstanceActionComponents((ContextInstance) value, horizontalLayout);
            }
            else if(value instanceof SchedulerJobInstance || value instanceof PrecedingItem) {

                SchedulerJobInstance schedulerJobInstance;

                if(value instanceof SchedulerJobInstance) {
                    schedulerJobInstance = (SchedulerJobInstance)value;
                }
                else {
                    schedulerJobInstance = ((PrecedingItem) value).schedulerJobInstance;
                }

                SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId()
                    , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                if(schedulerJobInstanceRecord != null) {
                    schedulerJobInstanceRecord.setStatus(schedulerJobInstance.getStatus().name());
                    schedulerJobInstanceRecord.getSchedulerJobInstance().setStatus(schedulerJobInstance.getStatus());

                    ComponentKey key;
                    ComponentKey precedingJobKey;

                    if(value instanceof SchedulerJobInstance && ((SchedulerJobInstance)value).getAgentName().equals(JobConstants.GLOBAL_EVENT)) {
                        key = new ComponentKey(JobConstants.GLOBAL_EVENT, schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName()
                            , schedulerJobInstanceRecord.getJobName());

                        precedingJobKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+JobConstants.GLOBAL_EVENT
                            ,schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());
                    }
                    else {
                        key = new ComponentKey(this.contextInstance.getName(),
                            schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());

                        precedingJobKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+this.contextInstance.getName(),
                            schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());
                    }

                    logger.debug(String.format("refreshing icons JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName()
                        , schedulerJobInstanceRecord.getSchedulerJobInstance().getContextName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(),
                        schedulerJobInstanceRecord.getStatus()));

                    if(value instanceof PrecedingItem) {
                        this.getJobInstanceActionComponents(precedingJobKey, schedulerJobInstanceRecord, horizontalLayout);
                        this.setIconVisibility(schedulerJobInstanceRecord, precedingJobKey);
                    }
                    else {
                        this.getJobInstanceActionComponents(key, schedulerJobInstanceRecord, horizontalLayout);
                        this.setIconVisibility(schedulerJobInstanceRecord, key);
                    }
                }

                if(value instanceof PrecedingItem) {
                    horizontalLayout.add(this.createContextVisualisationIcon
                        (ContextHelper.getChildContextInstance(schedulerJobInstance.getChildContextName(), this.contextInstance)));
                }
            }

            return horizontalLayout;
        })
        .setResizable(true)
        .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
        .setFlexGrow(3);

        grid.addComponentColumn(value -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                if(value instanceof SchedulerJobInstance || value instanceof PrecedingItem) {
                    SchedulerJobInstance schedulerJobInstance;

                    if(value instanceof SchedulerJobInstance) {
                        schedulerJobInstance =(SchedulerJobInstance) value;
                    }
                    else  {
                        schedulerJobInstance = ((PrecedingItem) value).schedulerJobInstance;
                    }

                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService
                        .findByContextIdJobNameChildContextName(this.contextInstance.getId()
                        , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                    if(schedulerJobInstanceRecord != null) {
                        if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance &&
                            !(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance    )) {
                            Cron quartzCron = parser.parse(((QuartzScheduleDrivenJobInstance) schedulerJobInstanceRecord
                                .getSchedulerJobInstance()).getCronExpression());
                            Div label = new Div();
                            label.getElement().getStyle().set("word-wrap", "normal");
                            label.getElement().getStyle().set("white-space", "normal");

                            label.setText(descriptor.describe(quartzCron));
                            horizontalLayout.add(label);
                        }
                    }
                }

                return horizontalLayout;
            })
            .setResizable(true)
            .setHeader(getTranslation("table-header.scheduled-time", UI.getCurrent().getLocale()))
            .setFlexGrow(1);
        grid.addComponentColumn(value -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                if(value instanceof SchedulerJobInstance || value instanceof PrecedingItem) {
                    SchedulerJobInstance schedulerJobInstance;

                    if(value instanceof SchedulerJobInstance) {
                        schedulerJobInstance =(SchedulerJobInstance) value;
                    }
                    else  {
                        schedulerJobInstance = ((PrecedingItem) value).schedulerJobInstance;
                    }

                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService
                        .findByContextIdJobNameChildContextName(this.contextInstance.getId()
                            , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                    if(schedulerJobInstanceRecord != null) {
                        if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance &&
                            !(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance)) {
                            Label label;

                            if (((QuartzScheduleDrivenJobInstance) schedulerJobInstanceRecord
                                .getSchedulerJobInstance()).getTimeZone() != null) {
                                label = new Label(((QuartzScheduleDrivenJobInstance) schedulerJobInstanceRecord
                                    .getSchedulerJobInstance()).getTimeZone());
                            } else {
                                label = new Label(TimeZone.getDefault().getID());
                            }

                            horizontalLayout.add(label);
                        }
                    }
                }

                return horizontalLayout;
            })
            .setResizable(true)
            .setHeader(getTranslation("table-header.timezone", UI.getCurrent().getLocale()))
            .setFlexGrow(1);
        grid.addComponentColumn(value -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                if(value instanceof SchedulerJobInstance || value instanceof PrecedingItem) {
                    SchedulerJobInstance schedulerJobInstance;

                    if(value instanceof SchedulerJobInstance) {
                        schedulerJobInstance =(SchedulerJobInstance) value;
                    }
                    else  {
                        schedulerJobInstance = ((PrecedingItem) value).schedulerJobInstance;
                    }

                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService
                        .findByContextIdJobNameChildContextName(this.contextInstance.getId()
                            , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                    if(schedulerJobInstanceRecord != null) {
                        Div label = new Div();
                        label.getElement().getStyle().set("word-wrap", "normal");
                        label.getElement().getStyle().set("white-space", "normal");

                        ComponentKey key;

                        if (value instanceof SchedulerJobInstance) {
                            key = new ComponentKey(this.contextInstance.getName()
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());
                        } else {
                            key = new ComponentKey(PRECEDING_ITEM_COMPONENT + this.contextInstance.getName()
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());

                        }

                        this.startTimes.put(key, label);

                        horizontalLayout.add(label);

                        if (schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null
                            && schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime() > 0) {

                            label.setText(DateFormatter.instance().getFormattedDate(schedulerJobInstanceRecord
                                .getSchedulerJobInstance().getScheduledProcessEvent().getFireTime()));
                        }
                    }
                }

                return horizontalLayout;
            })
            .setResizable(true)
            .setHeader(getTranslation("table-header.fire-time", UI.getCurrent().getLocale()))
            .setFlexGrow(1);
        grid.addComponentColumn(value -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                if(value instanceof  ContextInstance) {
                    ContextInstance instance;
                    if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                        instance = ContextHelper.getChildContextInstance(((ContextInstance) value).getName()
                            , ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext());
                    }
                    else {
                        instance = (ContextInstance) value;
                    }

                    AggregateContextInstanceStatus aggregateContextInstanceStatus
                        = ContextHelper.getAggregateContextInstanceStatus(instance, internalEventDrivenJobInstanceMap
                            , quartzSchedulerJobMap, this.contextInstance);

                    SchedulerStatusIconDiv disabledStatusDiv = new SchedulerStatusIconDiv();
                    disabledStatusDiv.getElement().getStyle().set("font-size", "8pt");
                    disabledStatusDiv.getElement().getStyle().set("margin-top", "1px");
                    disabledStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
                    disabledStatusDiv.setStatus(InstanceStatus.DISABLED);
                    disabledStatusDiv.setVisible(false);

                    horizontalLayout.add(disabledStatusDiv);

                    ComponentKey componentKey = new ComponentKey(this.contextInstance.getName()
                        , ((ContextInstance) value).getName(), InstanceStatus.DISABLED.name());
                    this.statusIconDivMap.put(componentKey, disabledStatusDiv);

                    SchedulerStatusIconDiv skippedStatusDiv = new SchedulerStatusIconDiv();
                    skippedStatusDiv.getElement().getStyle().set("font-size", "8pt");
                    skippedStatusDiv.getElement().getStyle().set("margin-top", "1px");
                    skippedStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
                    skippedStatusDiv.setStatus(InstanceStatus.SKIPPED);
                    skippedStatusDiv.setVisible(false);

                    horizontalLayout.add(skippedStatusDiv);

                    componentKey = new ComponentKey(this.contextInstance.getName()
                        , ((ContextInstance) value).getName(), InstanceStatus.SKIPPED.name());
                    this.statusIconDivMap.put(componentKey, skippedStatusDiv);

                    SchedulerStatusIconDiv onHoldStatusDiv = new SchedulerStatusIconDiv();
                    onHoldStatusDiv.getElement().getStyle().set("font-size", "8pt");
                    onHoldStatusDiv.getElement().getStyle().set("margin-top", "1px");
                    onHoldStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
                    onHoldStatusDiv.setStatus(InstanceStatus.ON_HOLD);
                    onHoldStatusDiv.setVisible(false);

                    componentKey = new ComponentKey(this.contextInstance.getName()
                        , ((ContextInstance) value).getName(), InstanceStatus.ON_HOLD.name());
                    this.statusIconDivMap.put(componentKey, onHoldStatusDiv);

                    horizontalLayout.add(onHoldStatusDiv);

                    if(aggregateContextInstanceStatus.isDisabledJobs()) {
                        disabledStatusDiv.setVisible(true);
                    }

                    if(aggregateContextInstanceStatus.isSkippedJobs()) {
                        skippedStatusDiv.setVisible(true);
                    }

                    if(aggregateContextInstanceStatus.isHeldJobs()) {
                        onHoldStatusDiv.setVisible(true);
                    }
                }
                else if(value instanceof SchedulerJobInstance) {
                    SchedulerJobInstance schedulerJobInstance = (SchedulerJobInstance)value;
                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService
                        .findByContextIdJobNameChildContextName(this.contextInstance.getId()
                            , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                    if(schedulerJobInstanceRecord != null) {
                        schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
                    }

                    SchedulerStatusDiv statusDiv = new SchedulerStatusDiv();
                    statusDiv.setWidthFull();
                    statusDiv.getElement().getStyle().set("font-size", "10pt");
                    statusDiv.getElement().getStyle().set("margin-top", "1px");
                    statusDiv.getElement().getStyle().set("margin-bottom", "1px");

                    horizontalLayout.add(statusDiv);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, statusDiv);

                    if(this.contextInstance.isQuartzScheduleDrivenJobsDisabledForContext() &&
                        schedulerJobInstanceRecord != null &&
                        schedulerJobInstanceRecord.getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                        statusDiv.setStatus(InstanceStatus.DISABLED);
                    }
                    else {
                        if(schedulerJobInstance instanceof InternalEventDrivenJobInstance &&
                            ((InternalEventDrivenJobInstance) schedulerJobInstance).isKilled()) {
                            statusDiv.setStatus(InstanceStatus.KILLED);
                        }
                        else {
                            statusDiv.setStatus(schedulerJobInstance.getStatus().name()
                                , schedulerJobInstance.isErrorAcknowledged());
                        }
                    }

                    ComponentKey componentKey = new ComponentKey(schedulerJobInstance instanceof GlobalEventJob ? JobConstants.GLOBAL_EVENT :  schedulerJobInstance.getContextName()
                        , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName());

                    Icon acknowledgedIcon = VaadinIcon.THUMBS_UP.create();
                    acknowledgedIcon.getStyle().set("cursor", "pointer");
                    acknowledgedIcon.getElement().setAttribute("title", getTranslation("tooltip.view-acknowledgement-details"));
                    acknowledgedIcon.setId(Integer.toString(componentKey.hashCode()));
                    if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged() != null &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged()) {
                        acknowledgedIcon.setVisible(true);
                    }
                    else {
                        acknowledgedIcon.setVisible(false);
                    }

                    acknowledgedIcon.addClickListener(event -> {
                        SchedulerJobInstanceRecord dbRecord = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
                        ErrorAcknowledgedPositionedDialog errorAcknowledgedPositionedDialog = new ErrorAcknowledgedPositionedDialog(dbRecord,
                            this.contextInstance, this.scheduledContextInstanceService, this.moduleMetaDataService, this.logStreamingService,
                            this.schedulerJobInstanceService);
                        PositionedDialog.Position position = new PositionedDialog.Position(event.getScreenY(), event.getScreenX());
                        errorAcknowledgedPositionedDialog.setPosition(position);
                        errorAcknowledgedPositionedDialog.open();
                    });

                    Button systemEventButton = new Button();
                    systemEventButton.getElement().appendChild(VaadinIcon.ELLIPSIS_DOTS_V.create().getElement());
                    systemEventButton.getElement().setAttribute("title", getTranslation("tab-label.system-events", UI.getCurrent().getLocale()));
                    systemEventButton.addClickListener(event -> {
                        JobSystemEventHistoryDialog systemEventHistoryDialog = new JobSystemEventHistoryDialog(this.contextInstance,
                                (SchedulerJobInstance) value, this.systemEventSearchService);
                        systemEventHistoryDialog.open();
                    });

                    horizontalLayout.add(acknowledgedIcon, systemEventButton);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, acknowledgedIcon, systemEventButton);

                    this.statusDivMap.put(componentKey, statusDiv);
                    this.errorAcknowledgedMap.put(componentKey, acknowledgedIcon);
                }
                else if(value instanceof PrecedingItem) {
                    SchedulerJobInstance schedulerJobInstance = ((PrecedingItem) value).schedulerJobInstance;
                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService
                        .findByContextIdJobNameChildContextName(this.contextInstance.getId()
                            , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                    if(schedulerJobInstanceRecord != null) {
                        schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
                    }

                    SchedulerStatusDiv statusDiv = new SchedulerStatusDiv();
                    statusDiv.setWidthFull();
                    statusDiv.getElement().getStyle().set("font-size", "10pt");
                    statusDiv.getElement().getStyle().set("margin-top", "1px");
                    statusDiv.getElement().getStyle().set("margin-bottom", "1px");

                    horizontalLayout.add(statusDiv);
                    horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, statusDiv);

                    if(this.contextInstance.isQuartzScheduleDrivenJobsDisabledForContext() &&
                        schedulerJobInstanceRecord.getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                        statusDiv.setStatus(InstanceStatus.DISABLED);
                    }
                    else {
                        if(schedulerJobInstance instanceof InternalEventDrivenJobInstance &&
                            ((InternalEventDrivenJobInstance) schedulerJobInstance).isKilled()) {
                            statusDiv.setStatus(InstanceStatus.KILLED);
                        }
                        else {
                            statusDiv.setStatus(schedulerJobInstance.getStatus());
                        }
                    }

                    ComponentKey componentKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+(schedulerJobInstance instanceof GlobalEventJob ? JobConstants.GLOBAL_EVENT :  schedulerJobInstance.getContextName())
                        , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName());
                    this.statusDivMap.put(componentKey, statusDiv);
                }

                return horizontalLayout;
            })
            .setResizable(true)
            .setHeader(getTranslation("table-header.status", UI.getCurrent().getLocale()))
            .setFlexGrow(1);
        grid.addComponentColumn(value -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                if(value instanceof SchedulerJobInstance || value instanceof PrecedingItem) {
                    SchedulerJobInstance schedulerJobInstance;

                    if(value instanceof SchedulerJobInstance) {
                        schedulerJobInstance =(SchedulerJobInstance) value;
                    }
                    else  {
                        schedulerJobInstance = ((PrecedingItem) value).schedulerJobInstance;
                    }

                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService
                        .findByContextIdJobNameChildContextName(this.contextInstance.getId()
                            , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                    if(schedulerJobInstanceRecord != null) {
                        Div label = new Div();
                        label.getElement().getStyle().set("word-wrap", "normal");
                        label.getElement().getStyle().set("white-space", "normal");

                        ComponentKey key;

                        if (value instanceof SchedulerJobInstance) {
                            key = new ComponentKey(this.contextInstance.getName()
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());
                        } else {
                            key = new ComponentKey(PRECEDING_ITEM_COMPONENT + this.contextInstance.getName()
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());

                        }

                        this.endTimes.put(key, label);

                        horizontalLayout.add(label);

                        if (schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null
                            && schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime() > 0) {
                            label.setText(DateFormatter.instance().getFormattedDate(schedulerJobInstanceRecord
                                .getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime()));
                        }
                    }
                }

                return horizontalLayout;
            })
            .setResizable(true)
            .setHeader(getTranslation("table-header.completion-time", UI.getCurrent().getLocale()))
            .setFlexGrow(1);

        grid.addComponentColumn(value -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                if(value instanceof SchedulerJobInstance || value instanceof PrecedingItem) {
                    SchedulerJobInstance schedulerJobInstance;

                    if(value instanceof SchedulerJobInstance) {
                        schedulerJobInstance =(SchedulerJobInstance) value;
                    }
                    else  {
                        schedulerJobInstance = ((PrecedingItem) value).schedulerJobInstance;
                    }

                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService
                        .findByContextIdJobNameChildContextName(this.contextInstance.getId()
                            , schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());

                    if(schedulerJobInstanceRecord != null) {
                        Button manuallySubmittedBy = new Button();
                        ComponentKey key;

                        if (value instanceof SchedulerJobInstance) {
                            key = new ComponentKey(this.contextInstance.getName()
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());
                        } else {
                            key = new ComponentKey(PRECEDING_ITEM_COMPONENT + this.contextInstance.getName()
                                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());

                        }

                        this.manuallySubmittedBy.put(key, manuallySubmittedBy);

                        if (schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
                            manuallySubmittedBy.setText(schedulerJobInstanceRecord.getManuallySubmittedBy());
                            horizontalLayout.add(manuallySubmittedBy);
                            if(schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
                                manuallySubmittedBy.addClickListener(event -> {
                                    SolrSystemEventSearchFilter searchFilter = new SolrSystemEventSearchFilter();
                                    searchFilter.setSubject(SystemEventConstants.SCHEDULED_JOB_SUBMITTED);
                                    searchFilter.setActor(schedulerJobInstanceRecord.getManuallySubmittedBy());
                                    searchFilter.setSearchTerm(schedulerJobInstanceRecord.getJobName());
                                    searchFilter.setAction(schedulerJobInstanceRecord.getContextInstanceId());
                                    SearchResults<SystemEvent> searchResults = this.systemEventSearchService
                                        .findByFilter(searchFilter, -1, -1, null, null);

                                    if(searchResults.getTotalNumberOfResults() > 0) {
                                        SystemEventDialog systemEventDialog = new SystemEventDialog(DateFormatter.instance());
                                        systemEventDialog.populate(searchResults.getResultList().get(0));
                                    }
                                });
                            }
                        }
                    }
                }

                return horizontalLayout;
            })
            .setResizable(true)
            .setHeader(getTranslation("table-header.manually-submitted-by", UI.getCurrent().getLocale()))
            .setFlexGrow(1);

        HierarchicalConfigurableFilterDataProvider dataProvider = this.createTreeGridDataProvider();

        this.addGridFiltering(dataProvider);

        grid.setDataProvider(dataProvider);

        grid.setSizeFull();
        grid.expandRecursively(Collections.singleton(contextInstance), contextInstance.getTreeViewExpandLevel() - 1);

        grid.addExpandListener(e -> this.expandedNodes.addAll(e.getItems()));
        grid.addCollapseListener(e -> this.expandedNodes.removeAll(e.getItems()));

        grid.setClassNameGenerator(item -> {
            if(item instanceof PrecedingItem) {
                return "precedingItem";
            }
            return null;
        });
    }


    /**
     * Sets the background color of an Image based on the status of the SchedulerJobInstance.
     * If the schedulerJobInstance is an instance of InternalEventDrivenJobInstance and is killed,
     * the background color is set to SCHEDULER_ERROR.
     * If the status of the schedulerJobInstance is COMPLETE, the background color is set to SCHEDULER_COMPLETE.
     * If the status of the schedulerJobInstance is RUNNING or SKIPPED_RUNNING, the background color is set to SCHEDULER_RUNNING.
     * If the status of the schedulerJobInstance is WAITING, the background color is set to SCHEDULER_WAITING.
     * If the status of the schedulerJobInstance is ERROR, and the error is acknowledged, the background color is set to SCHEDULER_ERROR_ACKNOWLEDGED,
     * otherwise, the background color is set to SCHEDULER_ERROR.
     * If the status of the schedulerJobInstance is LOCK_QUEUED, the background color is set to SCHEDULER_LOCK_QUEUED.
     * If the status of the schedulerJobInstance is SKIPPED or SKIPPED_COMPLETE, the background color is set to SCHEDULER_SKIPPED.
     * If the status of the schedulerJobInstance is ON_HOLD, the background color is set to SCHEDULER_ON_HOLD.
     * Finally, the image is marked as dirty to update the UI.
     *
     * @param image                the Image object whose background color is to be set
     * @param schedulerJobInstance the SchedulerJobInstance whose status is used to determine the background color
     */
    private void setImageBackgroundColour(Image image, SchedulerJobInstance schedulerJobInstance) {
        image.getElement().getStyle().remove("background-color");
        if(schedulerJobInstance instanceof InternalEventDrivenJobInstance && ((InternalEventDrivenJobInstance) schedulerJobInstance).isKilled()) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
        }
        else if(schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
        }
        else if(schedulerJobInstance.getStatus().equals(InstanceStatus.RUNNING) || schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED_RUNNING)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RUNNING);
        }
        else if(schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_WAITING);
        }
        else if(schedulerJobInstance.getStatus().equals(InstanceStatus.ERROR)) {
            if(schedulerJobInstance.isErrorAcknowledged() != null && schedulerJobInstance.isErrorAcknowledged()) {
                image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR_ACKNOWLEDGED);
            }
            else {
                image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
            }
        }
        else if(schedulerJobInstance.getStatus().equals(InstanceStatus.LOCK_QUEUED)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
        }
        else if(schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED) || schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
        }
        else if(schedulerJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
        }
        image.getElement().getNode().markAsDirty();
    }

    /**
     * Helper method to get the functional context visualisation icon.
     *
     * @param contextInstance the context instance that will be opened when the icon is clicked.
     *
     * @return the initialised functional icon.
     */
    private Icon createContextVisualisationIcon(ContextInstance contextInstance) {
        Icon visualisation = IconDecorator.decorate(new Icon(VaadinIcon.SITEMAP), getTranslation("tooltip.open-visualisation"
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        visualisation.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            JobInstanceSplitVisualisationDialog jobInstanceSplitVisualisationDialog = new JobInstanceSplitVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
                this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService,
                this.contextProfileService, this.globalEventService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance,
                this.contextVisualisationNodeDistance);

            if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                this.contextInstance = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId()).getContext();
                ContextHelper.enrichJobs(this.contextInstance);
            }

            ContextInstance childContext = ContextHelper.getChildContextInstance(contextInstance.getName(), this.contextInstance);

            try {
                jobInstanceSplitVisualisationDialog.createSchedulerVisualisation(this.contextInstance, childContext);
                jobInstanceSplitVisualisationDialog.open();
            }
            catch (IOException e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.cannot-open-visualisation", UI.getCurrent().getLocale()));
            }
        });

        return visualisation;
    }

    /**
     * Get a reference to the image path associated with a job type.
     *
     * @param schedulerJob the job to get the image path for.
     *
     * @return the image path.
     */
    protected String getJobImage(SchedulerJob schedulerJob) {
        String image = "frontend/images/command_black.png";

        if(schedulerJob instanceof FileEventDrivenJob || schedulerJob instanceof FileEventDrivenJobInstance) {
            image = "frontend/images/file_black.png";
        }
        else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
            image = "frontend/images/time_black.png";
        }
        else if(schedulerJob instanceof GlobalEventJob || schedulerJob instanceof GlobalEventJobInstance) {
            image = "frontend/images/global-job.png";
        }
        else if(schedulerJob instanceof LocalEventJob || schedulerJob instanceof LocalEventJobInstance) {
            image = "frontend/images/local-event-job.png";
        }

        return image;
    }

    /**
     * Create the action components for a context instance.
     *
     * @param contextInstance
     * @param horizontalLayout
     */
    protected void getContextInstanceActionComponents(ContextInstance contextInstance, HorizontalLayout horizontalLayout) {
        if(contextInstance.getScheduledJobs() != null
            && !contextInstance.getScheduledJobs().isEmpty()){
            Icon visualisationIcon = this.createContextVisualisationIcon(contextInstance);
            horizontalLayout.add(visualisationIcon);

            ComponentSecurityVisibility.applySecurity(this.authentication, visualisationIcon, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);
        }
        Icon hold = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-all-nested-jobs", UI.getCurrent().getLocale()
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        hold.addClickListener(event -> {
            if(!this.canPerformAction(true)) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.hold-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.hold-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.open(getTranslation("progress-dialog.hold-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                    getTranslation("progress-dialog.hold-all-jobs-jobs-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextInstanceTreeViewWidget"));
                executor.execute(() -> {
                    boolean error = false;
                    try {
                        if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                            List<SchedulerJobInstanceRecord> updatedJobs = schedulerJobInstanceService.holdJobsWithinContext(ContextMachineCache
                                .instance().getByContextInstanceId(this.contextInstance.getId()).getContext(), contextInstance.getName());

                            if (updatedJobs.size() > 0) {
                                updatedJobs.forEach(schedulerJobInstanceRecord -> {
                                    SchedulerJobInstance schedulerJobInstance = ContextHelper.getSchedulerJobInstance(schedulerJobInstanceRecord.getJobName(),
                                        schedulerJobInstanceRecord.getChildContextName(), ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext());
                                    if(schedulerJobInstance != null) {
                                        schedulerJobInstance.setStatus(InstanceStatus.ON_HOLD);
                                        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
                                            = new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstanceRecord.getSchedulerJobInstance(),
                                            this.contextInstance, InstanceStatus.WAITING, InstanceStatus.ON_HOLD);
                                        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
                                    }
                                    else {
                                        logger.info("Could not update job [{}] to ON_HOLD for context instance name[{}], context instance id[{}], child context[{}]",
                                            schedulerJobInstanceRecord.getJobName(), this.contextInstance.getName(), this.contextInstance.getId()
                                            , schedulerJobInstanceRecord.getChildContextName());
                                    }
                                });
                                ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                                contextMachine.saveContext();
                                ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
                                this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_HOLDING_ALL_JOBS, String.format("Job Plan Name[%s], Child Job Plan Name[%s], Job Plan Identifier[%s]"
                                    ,this.contextInstance.getName() , contextInstance.getName(), this.contextInstance.getId()), this.authentication.getName());
                            }
                        }
                    } catch (Exception e) {
                        logger.error(String.format("An error has occurred holding all jobs for job plan[%s] with instance id[%s]!", contextInstance.getName(), contextInstance.getId()), e);
                        error = true;
                    } finally {
                        boolean finalError = error;
                        current.access(() -> {
                            dialog.close();
                            if (finalError) {
                                NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-hold-error"
                                    , UI.getCurrent().getLocale()));
                            } else {
                                NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-successfully-held"
                                    , UI.getCurrent().getLocale()));
                            }
                        });
                    }
                });
            });
        });

        horizontalLayout.add(hold);

        ComponentSecurityVisibility.applySecurity(this.authentication, hold, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        Icon release = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-all-nested-jobs", UI.getCurrent().getLocale()
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        release.addClickListener(event -> {
            if(!this.canPerformAction(true)) {
                return;
            }
            List<SchedulerJobInstanceRecord> jobsToReleaseWithinContext = schedulerJobInstanceService.getJobsToReleaseWithinContext(ContextMachineCache
                .instance().getByContextInstanceId(this.contextInstance.getId()).getContext(), contextInstance.getName());

            if(jobsToReleaseWithinContext.size() > 0) {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.release-jobs-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(String.format(getTranslation("confirm-dialog.release-jobs-body", UI.getCurrent().getLocale())
                    , jobsToReleaseWithinContext.size()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                    dialog.open(getTranslation("progress-dialog.release-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                        getTranslation("progress-dialog.release-all-jobs-jobs-body", UI.getCurrent().getLocale()));

                    final UI current = UI.getCurrent();
                    Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextInstanceTreeViewWidget"));
                    executor.execute(() -> {
                        boolean error = false;
                        try {
                            if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                                this.systemEventLogger.logEvent(SystemEventConstants.CHILD_CONTEXT_INSTANCE_RELEASING_ALL_JOBS_START, String.format("Job Plan Name[%s], Child Job Plan Name[%s], Job Plan Identifier[%s]"
                                    , this.contextInstance.getName(), contextInstance.getName(), contextInstance.getId()), this.authentication.getName());
                                if (jobsToReleaseWithinContext.size() > 0) {
                                    for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : jobsToReleaseWithinContext) {
                                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                                        contextMachine.releaseJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                                            schedulerJobInstanceRecord.getChildContextName());
                                    }
                                }
                                this.systemEventLogger.logEvent(SystemEventConstants.CHILD_CONTEXT_INSTANCE_RELEASING_ALL_JOBS_END, String.format("Job Plan Name[%s], Child Job Plan Name[%s], Job Plan Identifier[%s]"
                                    , this.contextInstance.getName(), contextInstance.getName(), this.contextInstance.getId()), this.authentication.getName());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            error = true;
                        } finally {
                            boolean finalError = error;
                            current.access(() -> {
                                dialog.close();

                                if (finalError) {
                                    NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-released-error"
                                        , UI.getCurrent().getLocale()));
                                } else {
                                    NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-successfully-released"
                                        , UI.getCurrent().getLocale()));
                                }
                            });
                        }
                    });
                });
            }
            else {
                NotificationHelper.showUserNotification(getTranslation("notification.no-scheduler-jobs-to-release"));
            }
        });

        ComponentSecurityVisibility.applySecurity(this.authentication, release, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        horizontalLayout.add(release);

        Icon skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-all-nested-jobs", UI.getCurrent().getLocale()
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        skip.addClickListener(event -> {
            if(!this.canPerformAction(true)) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.skip-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.skip-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.open(getTranslation("progress-dialog.skip-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                    getTranslation("progress-dialog.skip-all-jobs-jobs-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextInstanceTreeViewWidget"));
                executor.execute(() -> {
                    boolean error = false;
                    try {
                        ContextMachine contextMachine = ContextMachineCache
                            .instance().getByContextInstanceId(this.contextInstance.getId());

                        contextMachine.skipJobs(contextInstance.getName(), true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = true;
                    } finally {
                        boolean finalError = error;
                        current.access(() -> {
                            dialog.close();
                            if (finalError) {
                                NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-skip-error"
                                    , UI.getCurrent().getLocale()));
                            } else {
                                this.systemEventLogger.logEvent(SystemEventConstants.All_SCHEDULED_JOBS_SKIPPED_FOR_JOB_PLAN,
                                    String.format("Skipping all jobs for job plan. Job Plan Name[%s], Job Plan Instance Id[%s], Child Context[%s]"
                                        , this.contextInstance.getName(), this.contextInstance.getId(), contextInstance.getName())
                                    , this.authentication.getName());
                                NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-successfully-skipped"
                                    , UI.getCurrent().getLocale()));
                            }
                        });
                    }
                });
            });
        });

        ComponentSecurityVisibility.applySecurity(this.authentication, skip, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        horizontalLayout.add(skip);

        Icon enable = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.enable-all-nested-jobs", UI.getCurrent().getLocale()
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        enable.addClickListener(event -> {
            if(!this.canPerformAction(true)) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.enable-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.enable-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.open(getTranslation("progress-dialog.enable-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                    getTranslation("progress-dialog.enable-all-jobs-jobs-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextInstanceTreeViewWidget"));
                executor.execute(() -> {
                    boolean error = false;
                    try {
                        ContextMachine contextMachine = ContextMachineCache
                            .instance().getByContextInstanceId(this.contextInstance.getId());

                        contextMachine.skipJobs(contextInstance.getName(), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = true;
                    } finally {
                        boolean finalError = error;
                        current.access(() -> {
                            dialog.close();
                            if (finalError) {
                                NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-enable-error"
                                    , UI.getCurrent().getLocale()));
                            } else {
                                this.systemEventLogger.logEvent(SystemEventConstants.All_SCHEDULED_JOBS_ENABLED_FOR_JOB_PLAN,
                                    String.format("Enabling all skipped jobs for job plan. Job Plan Name[%s], Job Plan Instance Id[%s], Child Context[%s]"
                                        , this.contextInstance.getName(), this.contextInstance.getId(), contextInstance.getName())
                                    , this.authentication.getName());
                                NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-successfully-enabled"
                                    , UI.getCurrent().getLocale()));
                            }
                        });
                    }
                });
            });
        });

        ComponentSecurityVisibility.applySecurity(this.authentication, enable, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        horizontalLayout.add(enable);
    }

    /**
     * Method to initialise and create all action icons for a given job record. All icons are added to the provided layout.
     *
     * @param key
     * @param schedulerJobInstanceRecord
     * @param layout
     */
    protected void getJobInstanceActionComponents(ComponentKey key, SchedulerJobInstanceRecord schedulerJobInstanceRecord, HorizontalLayout layout) {
        if(!this.schedulerJobIconMap.containsKey(key)) {
            this.schedulerJobIconMap.put(key, new HashMap<>());
        }

        Map<String, Icon> iconMap = this.schedulerJobIconMap.get(key);

        Icon modal = IconDecorator.decorate(new Icon(VaadinIcon.MODAL), getTranslation("tooltip.open-visualisation"
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        modal.addClickListener(event -> {
            SchedulerJobInstanceRecord refreshedRecord = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
            if(refreshedRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                FileEventJobInstanceDialog fileEventJobInstanceDialog
                    = new FileEventJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName()),
                    this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService, this.contextInstance);
                fileEventJobInstanceDialog.setJob(refreshedRecord);
                fileEventJobInstanceDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance) {
                QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobInstanceDialog
                    = new QuartzDrivenScheduledJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName()),
                    this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService, this.contextInstance);
                quartzDrivenScheduledJobInstanceDialog.setJob(refreshedRecord);
                quartzDrivenScheduledJobInstanceDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                InternalEventDrivenJobInstanceDialog internalEventDrivenJobDialog
                    = new InternalEventDrivenJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName())
                    , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService
                    , this.systemEventLogger, this.schedulerJobInstanceService, this.contextInstance, this.jobInitiationService, this.moduleMetaDataService
                    , this.logStreamingService, this.jobUtilsService, this.scheduledContextInstanceService);
                internalEventDrivenJobDialog.setJob(refreshedRecord);
                internalEventDrivenJobDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof GlobalEventJobInstance) {
                GlobalEventJobInstanceDialog globalEventJobInstanceDialog = new GlobalEventJobInstanceDialog(systemEventLogger, schedulerJobInstanceService, this.globalEventService
                    , this.contextInstance);
                globalEventJobInstanceDialog.setJob(refreshedRecord);

                globalEventJobInstanceDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof LocalEventJobInstance) {
                LocalEventJobInstanceDialog localEventJobInstanceDialog = new LocalEventJobInstanceDialog(systemEventLogger, schedulerJobInstanceService
                    , this.contextInstance);
                localEventJobInstanceDialog.setJob(refreshedRecord);

                localEventJobInstanceDialog.open();
            }
        });

        layout.add(modal);

        Icon skip;
        if(!iconMap.containsKey(SKIP_ICON)) {
            skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");

            skip.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(true)) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> this.skipJob(schedulerJobInstanceRecord));
            });

            iconMap.put(SKIP_ICON, skip);
        }
        else {
            skip = iconMap.get(SKIP_ICON);
        }

        layout.add(skip);

        Icon enable;
        if(!iconMap.containsKey(ENABLE_ICON)) {
            enable = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.enable-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            enable.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE));
            enable.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(true)) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.enableJob(schedulerJobInstanceRecord));
            });

            iconMap.put(ENABLE_ICON, enable);
        }
        else {
            enable = iconMap.get(ENABLE_ICON);
        }

        layout.add(enable);

        Icon hold;
        if(!iconMap.containsKey(HOLD_ICON)) {
            hold = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            hold.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE));
            hold.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(true)) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.holdJob(schedulerJobInstanceRecord));
            });

            iconMap.put(HOLD_ICON, hold);
        }
        else {
            hold = iconMap.get(HOLD_ICON);
        }

        layout.add(hold);

        Icon release;
        if(!iconMap.containsKey(RELEASE_ICON)) {
            release = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            release.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE));
            release.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(true)) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.releaseJob(schedulerJobInstanceRecord));
            });

            iconMap.put(RELEASE_ICON, release);
        }
        else {
            release = iconMap.get(RELEASE_ICON);
        }

        layout.add(release);

        Icon submit;

        if(!iconMap.containsKey(SUBMIT_ICON)) {
            submit = IconDecorator.decorate(new Icon(VaadinIcon.PAPERPLANE), getTranslation("tooltip.submit-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            submit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(false)) {
                    return;
                }
                if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    InternalEventDrivenJobSubmissionDialog internalEventDrivenJobSubmissionDialog = new InternalEventDrivenJobSubmissionDialog(this.systemEventLogger,
                        this.moduleMetaDataService, this.contextInstance, this.jobInitiationService, schedulerJobInstanceRecord, this.schedulerJobInstanceService);

                    internalEventDrivenJobSubmissionDialog.open();
                } else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-file-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-file-job", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                            schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            this.jobInitiationService.raiseFileEventSchedulerJob(
                                agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getSchedulerJobInstance(), schedulerJobInstanceRecord.getContextInstanceId());

                            logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                    , this.contextInstance.getId()), this.authentication.getName());

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                }
                else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-quartz-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-quartz-job", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                            this.jobInitiationService.raiseQuartzSchedulerJob(agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getSchedulerJobInstance(), schedulerJobInstanceRecord.getContextInstanceId());

                            logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                    , this.contextInstance.getId()), this.authentication.getName());

                            schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                }
                else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof GlobalEventJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-global-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-global-job", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            GlobalEventJobInstance globalEventJobInstance = (GlobalEventJobInstance)schedulerJobInstanceRecord
                                .getSchedulerJobInstance();

                            this.globalEventService.raiseGlobalEventJob(globalEventJobInstance,
                                this.contextInstance.getId(), SecurityContextHolder.getContext().getAuthentication().getName());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                    , this.contextInstance.getId()), this.authentication.getName());

                            globalEventJobInstance.setStatus(InstanceStatus.COMPLETE);
                            schedulerJobInstanceRecord.setSchedulerJobInstance(globalEventJobInstance);
                            schedulerJobInstanceRecord.setStatus(InstanceStatus.COMPLETE.toString());
                            schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                }
                else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof LocalEventJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-local-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-local-job", UI.getCurrent().getLocale()));
                    confirmDialog.setConfirmText(getTranslation("button.ok"));
                    confirmDialog.setCancelText(getTranslation("button.cancel"));
                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            LocalEventJobInstance localEventJobInstance = (LocalEventJobInstance)schedulerJobInstanceRecord
                                .getSchedulerJobInstance();

                            localEventService.raiseLocalEventJob(localEventJobInstance,
                                this.contextInstance.getId(), SecurityContextHolder.getContext().getAuthentication().getName());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                                , this.authentication.getName());

                            localEventJobInstance.setStatus(InstanceStatus.COMPLETE);
                            schedulerJobInstanceRecord.setSchedulerJobInstance(localEventJobInstance);
                            schedulerJobInstanceRecord.setStatus(InstanceStatus.COMPLETE.toString());
                            schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                }
            });
            iconMap.put(SUBMIT_ICON, submit);
        }
        else {
            submit = iconMap.get(SUBMIT_ICON);
        }

        layout.add(submit);

        Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale())
            , "14pt", "rgba(0, 0, 0, 1.0)");
        StreamResource streamResource = new StreamResource(schedulerJobInstanceRecord.getJobName()+".json"
            , () -> {
            try {
                return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedulerJobInstanceRecord.getSchedulerJobInstance()));
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.downloading-job", UI.getCurrent().getLocale()));
                return null;
            }
        });

        ComponentSecurityVisibility.applySecurity(export, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
        exportWrapper.wrapComponent(export);
        layout.add(exportWrapper);

        Icon logFile;

        if(!iconMap.containsKey(LOG_ICON)) {
            logFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_PROCESS), getTranslation("tooltip.view-log-file"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            logFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                SchedulerJobInstanceRecord refreshedRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(schedulerJobInstanceRecord.getContextInstanceId(),
                    schedulerJobInstanceRecord.getJobName(), schedulerJobInstanceRecord.getChildContextName());
                this.streamLog(refreshedRecord, false);
            });

            iconMap.put(LOG_ICON, logFile);
        }
        else {
            logFile = iconMap.get(LOG_ICON);
        }

        layout.add(logFile);

        Icon errorLogFile;

        if(!iconMap.containsKey(ERROR_LOG_ICON)) {
            errorLogFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_REMOVE), getTranslation("tooltip.view-error-log-file"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            errorLogFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                SchedulerJobInstanceRecord refreshedRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(schedulerJobInstanceRecord.getContextInstanceId(),
                    schedulerJobInstanceRecord.getJobName(), schedulerJobInstanceRecord.getChildContextName());
                this.streamLog(refreshedRecord, true);
            });

            iconMap.put(ERROR_LOG_ICON, errorLogFile);
        }
        else {
            errorLogFile = iconMap.get(ERROR_LOG_ICON);
        }

        layout.add(errorLogFile);

        Icon logFileHistory;

        if(!iconMap.containsKey(LOG_FILE_HISTORY)) {
            logFileHistory = IconDecorator.decorate(new Icon(VaadinIcon.CLIPBOARD_HEART), getTranslation("tooltip.view-job-execution-history"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            logFileHistory.addClickListener(event -> {
                LogFileHistoryDialog logFileHistoryDialog = new LogFileHistoryDialog(scheduledContextInstanceService
                    , this.contextInstance, schedulerJobInstanceRecord.getSchedulerJobInstance()
                    , this.moduleMetaDataService, this.logStreamingService);
                logFileHistoryDialog.open();
            });

            iconMap.put(LOG_FILE_HISTORY, logFileHistory);
        }
        else {
            logFileHistory = iconMap.get(LOG_FILE_HISTORY);
        }

        layout.add(logFileHistory);

        Icon event;

        if(!iconMap.containsKey(EVENT_ICON)) {
            event = IconDecorator.decorate(new Icon(VaadinIcon.CALENDAR_CLOCK), getTranslation("tooltip.view-scheduled-process-event"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            event.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                SchedulerJobInstanceRecord updated = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());

                JsonViewerDialog dialog = new JsonViewerDialog(updated.getSchedulerJobInstance().getScheduledProcessEvent()
                    , getTranslation("header.catalyst-scheduled-process-event", UI.getCurrent().getLocale()));
                dialog.open();
            });

            iconMap.put(EVENT_ICON, event);
        }
        else {
            event = iconMap.get(EVENT_ICON);
        }

        layout.add(event);

        Icon executionDetails;

        if(!iconMap.containsKey(EXECUTION_ENVIRONMENT_ICON)) {
            executionDetails = IconDecorator.decorate(new Icon(VaadinIcon.COG), getTranslation("tooltip.view-process-execution-details"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            executionDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                SchedulerJobInstanceRecord updated = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
                TextViewerDialog dialog = new TextViewerDialog(updated.getSchedulerJobInstance().getScheduledProcessEvent().getExecutionDetails()
                    , getTranslation("header.process-execution-details", UI.getCurrent().getLocale()));
                dialog.open();
            });

            iconMap.put(EXECUTION_ENVIRONMENT_ICON, event);
        }
        else {
            executionDetails = iconMap.get(EXECUTION_ENVIRONMENT_ICON);
        }

        layout.add(executionDetails);

        Icon reset;

        if(!iconMap.containsKey(RESET_JOB_ICON)) {
            reset = IconDecorator.decorate(new Icon(VaadinIcon.ARROW_BACKWARD), getTranslation("tooltip.reset-job"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            reset.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(false)) {
                    return;
                }
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.reset-job-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.reset-job-body", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    super.resetJob(schedulerJobInstanceRecord.getSchedulerJobInstance());
                });
            });

            iconMap.put(RESET_JOB_ICON, reset);
        }
        else {
            reset = iconMap.get(RESET_JOB_ICON);
        }

        layout.add(reset);

        Icon acknowledgeError;

        if(!iconMap.containsKey(ACKNOWLEDGE_ERROR_JOB_ICON)) {
            acknowledgeError = IconDecorator.decorate(new Icon(VaadinIcon.THUMBS_UP), getTranslation("tooltip.acknowledge-error"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            acknowledgeError.getStyle().set("cursor", "pointer");
            acknowledgeError.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(false)) {
                    return;
                }

                AcknowledgeErrorDialog errorDialog = new AcknowledgeErrorDialog(this.contextInstance, this.schedulerJobInstanceService,
                    schedulerJobInstanceRecord, this.systemEventLogger);
                errorDialog.open();
            });

            iconMap.put(ACKNOWLEDGE_ERROR_JOB_ICON, acknowledgeError);
        }
        else {
            acknowledgeError = iconMap.get(ACKNOWLEDGE_ERROR_JOB_ICON);
        }

        layout.add(acknowledgeError);

        Icon submitDownstreamJobs;

        if(!iconMap.containsKey(SUBMIT_DOWNSTREAM_JOBS_ICON)) {
            submitDownstreamJobs = IconDecorator.decorate(new Icon(VaadinIcon.FAST_FORWARD), getTranslation("button.initiate-downstream-jobs"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            submitDownstreamJobs.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(!this.canPerformAction(false)) {
                    return;
                }
                submitDownstreamJobs((InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance());
            });

            iconMap.put(SUBMIT_DOWNSTREAM_JOBS_ICON, submitDownstreamJobs);
        }
        else {
            submitDownstreamJobs = iconMap.get(SUBMIT_DOWNSTREAM_JOBS_ICON);
        }

        layout.add(submitDownstreamJobs);
    }

    /**
     * Helper method to confirm that actions can be performed on a job plan
     * @return
     */
    private boolean canPerformAction(boolean isAllowedWhenPrepared) {
        if(!ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
            this.contextInstance = this.scheduledContextInstanceService
                .findById(this.contextInstance.getId()+ "_" + SCHEDULED_CONTEXT_INSTANCE).getContextInstance();
            if(this.contextInstance.getStatus().equals(InstanceStatus.ENDED)) {
                NotificationHelper.showUserNotification(getTranslation("notification.cannot-perform-action-against-ended-plan"
                    , UI.getCurrent().getLocale()));
                return false;
            }
            else {
                NotificationHelper.showErrorNotification(getTranslation("error.cannot-locate-job-plan-instance-in-cache-and-is-not-ended"
                    , UI.getCurrent().getLocale()));
                return false;
            }
        }
        else if(this.contextInstance.getStatus().equals(InstanceStatus.PREPARED) && !isAllowedWhenPrepared) {
            NotificationHelper.showUserNotification(getTranslation("notification.cannot-perform-action-against-prepared-plan"
                , UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to set the visibility of all action items associated with a individual job.
     *
     * @param schedulerJobInstanceRecord the job we are setting the action visibility for.
     * @param key the key to the map containing icons for the job
     */
    protected void setIconVisibility(SchedulerJobInstanceRecord schedulerJobInstanceRecord, ComponentKey key) {
        Map<String, Icon> iconMap = this.schedulerJobIconMap.get(key);
        Icon skip = iconMap.get(SKIP_ICON);


        if (skip != null) {
            if ((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                !((schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.LOCK_QUEUED))) {
                skip.setVisible(true &&
                    ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            } else {
                skip.setVisible(false);
            }
        }

        Icon enable = iconMap.get(ENABLE_ICON);

        if(enable != null) {
            if ((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
                (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                    !(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)))) {
                enable.setVisible(false);
            } else if ((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.GLOBAL_EVENT_JOB_INSTANCE) ||
                schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE))) {
                enable.setVisible(true &&
                    ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                        SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                        SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            }
        }


        Icon hold = iconMap.get(HOLD_ICON);
        if((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
            schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.LOCK_QUEUED))) {
            hold.setVisible(false);
        }
        else if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
            schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) {
           hold.setVisible(ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                   SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                   SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        }

        Icon release = iconMap.get(RELEASE_ICON);

        if((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
            schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) &&
            !schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD)) {
            release.setVisible(false);
        }
        else if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
            schedulerJobInstanceRecord.getType().equals(JobConstants.LOCAL_EVENT_JOB_INSTANCE)) {
            release.setVisible(ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        }

        Icon logFile = iconMap.get(LOG_ICON);
        logFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)) &&
            ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

        Icon errorLogFile = iconMap.get(ERROR_LOG_ICON);
        errorLogFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)) &&
            ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

        Icon logFileHistory = iconMap.get(LOG_FILE_HISTORY);
        logFileHistory.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

        Icon eventIcon = iconMap.get(EVENT_ICON);
        eventIcon.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null &&
            ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

        Icon executionDetails = iconMap.get(EXECUTION_ENVIRONMENT_ICON);
        executionDetails.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent().getExecutionDetails() != null &&
            ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

        Icon reset = iconMap.get(RESET_JOB_ICON);
        reset.setVisible((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
            schedulerJobInstanceRecord.getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE)
                || schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)
                || schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.LOCK_QUEUED)) &&
            ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        Icon acknowledge = iconMap.get(ACKNOWLEDGE_ERROR_JOB_ICON);
        acknowledge.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged() == null
                || !schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged()) &&
            ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        Icon submitDownstreamJobs = iconMap.get(SUBMIT_DOWNSTREAM_JOBS_ICON);
        submitDownstreamJobs.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
            ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        Icon submit = iconMap.get(SUBMIT_ICON);
        submit.setVisible(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.WAITING) &&
            ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        SchedulerJobStateChangeEventBroadcaster.register(this);
        ContextInstanceStateChangeEventBroadcaster.register(this);
        ContextInstanceSavedEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        logger.debug("Detaching ContextInstanceTreeView");
        this.ui = null;

        SchedulerJobStateChangeEventBroadcaster.unregister(this);
        ContextInstanceStateChangeEventBroadcaster.unregister(this);
        ContextInstanceSavedEventBroadcaster.unregister(this);

        logger.debug("Finished detaching ContextInstanceTreeView");
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        logger.debug("received SchedulerJobInstanceStateChangeEvent event: " + event.getSchedulerJobInstance().getJobName());
        if(event.getSchedulerJobInstance().getContextInstanceId().equals(this.contextInstance.getId())) {
            manageJobStatusStateChangeEvent(this.ui, event);
            manageContextStatusIndicators(this.ui);
        }
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        logger.debug("received ContextInstanceStateChangeEvent event");
        if(event.getContextInstanceId().equals(this.contextInstance.getId())) {
            if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext();
                this.manageContextInstanceStateChangeEvent(this.ui, event);
                manageContextStatusIndicators(this.ui);
            }
            else {
                this.manageContextInstanceStateChangeEvent(this.ui, event);
                manageContextStatusIndicators(this.ui);
            }
        }
    }

    @Override
    public void receiveBroadcast(ContextInstance event) {
        logger.debug("received ContextInstance event: " + contextInstance.getId());
        if(event.getId().equals(this.contextInstance.getId())) {
            this.contextInstance = event;
            ContextHelper.enrichJobs(contextInstance);
            this.enableDisableScheduledJobs(this.contextInstance, this.ui);
            manageContextStatusIndicators(this.ui);
            this.grid.getDataCommunicator().reset();
            this.createTreeGridDataProvider().refreshAll();
        }
    }

    /**
     * Helper method to create the data provider for the underlying tree grid.
     *
     * @return the initialised data provider.
     */
    private HierarchicalConfigurableFilterDataProvider<Object, Void, TreeFilter> createTreeGridDataProvider() {
        return new AbstractBackEndHierarchicalDataProvider<Object, TreeFilter>() {
                // returns the number of immediate child items
                @Override
                public int getChildCount(HierarchicalQuery<Object, TreeFilter> query) {
                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    int count = getNodeChildren(query.getParent(), query.getFilter(), 0, 0).size();
                    stopWatch.stop();
                    logger.debug("TreeView getChildCount: Elapsed milli: " + stopWatch.getTime());
                    return count;
                }
                // checks if a given item should be expandable
                @Override
                public boolean hasChildren(Object item) {
                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    List<Object> children = new ArrayList<>();
                    if(item instanceof ContextInstance) {
                        if (((ContextInstance)item).getContexts() != null
                            && !((ContextInstance)item).getContexts().isEmpty()) {
                            ((ContextInstance)item).getContexts().sort((a, b) -> a.getOrdinal() < b.getOrdinal() ? -1 : 1);
                            children.addAll(((ContextInstance)item).getContexts().stream()
                                .map(instance -> (Object) instance)
                                .collect(Collectors.toList()));
                        }

                        if (((ContextInstance)item).getScheduledJobs() != null
                            && !((ContextInstance)item).getScheduledJobs().isEmpty()) {
                            children.addAll(((ContextInstance)item).getScheduledJobs().stream()
                                .filter(instance -> !instance.getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                                    && !instance.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
                                .map(instance -> (Object) instance)
                                .collect(Collectors.toList()));
                        }
                    }
                    if(item instanceof SchedulerJobInstance) {
                        SchedulerJobInstance schedulerJobInstance = (SchedulerJobInstance) item;
                        children.addAll(ContextHelper.getPrecedingJobsFromOutsideContext(contextInstance,
                                schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName()
                                , getCommandExecutionJobsForContextInstance(contextInstance.getId()))
                            .stream()
                            .filter(instance -> !instance.getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                                && !instance.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))
                            .map(instance -> (Object) new PrecedingItem(instance))
                            .collect(Collectors.toList()));
                    }
                    stopWatch.stop();
                    logger.debug("TreeView hasChildren: Elapsed milli: " + stopWatch.getTime());
                    return children.stream().distinct().collect(Collectors.toList()).size() > 0;
                }

                // returns the immediate child items based on offset and limit
                @Override
                protected Stream<Object> fetchChildrenFromBackEnd(HierarchicalQuery<Object, TreeFilter> query) {
                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    Stream<Object> results = getNodeChildren(query.getParent(), query.getFilter(), query.getLimit(), query.getOffset()).stream();
                    stopWatch.stop();
                    logger.debug("TreeView fetchChildrenFromBackEnd: Elapsed milli: " + stopWatch.getTime());
                    return results;
                }
            }.withConfigurableFilter();
    }

    /**
     * For a node in the tree determine if there are any children associated with the node
     * and return a list of those children. Children are loaded lazily when a node is
     * expanded or the tree filtered.
     *
     * @param node the node to load the children for.
     * @param filter the filter to apply
     *
     * @return relevant filtered children for the given node.
     */
    private List<Object> getNodeChildren(Object node, Optional<TreeFilter> filter, int limit, int offset) {
        List<Object> children = new ArrayList<>();
        if (node == null) {
            children.add(contextInstance);
        } else if (node instanceof ContextInstance) {
            if (((ContextInstance) node).getContexts() != null
                && !((ContextInstance) node).getContexts().isEmpty()) {
                if (filter != null && filter.isPresent() && filter.get().getJobName().isEmpty()) {
                    ((ContextInstance)node).getContexts().sort((a, b) -> a.getOrdinal() < b.getOrdinal() ? -1 : 1);
                    children.addAll(((ContextInstance) node).getContexts().stream()
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                } else {
                    ((ContextInstance)node).getContexts().sort((a, b) -> a.getOrdinal() < b.getOrdinal() ? -1 : 1);
                    children.addAll(((ContextInstance) node).getContexts().stream()
                        .filter(instance -> (instance.getName().toLowerCase().contains(filter.get().getJobName().toLowerCase())))
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));

                    children.addAll(((ContextInstance) node).getContexts().stream()
                        .filter(instance -> ContextHelper.getContextsWhereJobFilterMatchResides
                            (instance, filter.get().getJobName()).size() > 0)
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                }
            }

            if (((ContextInstance) node).getScheduledJobs() != null
                && !((ContextInstance) node).getScheduledJobs().isEmpty()) {
                children.addAll(((ContextInstance) node).getScheduledJobs().stream()
                    .filter(instance -> !instance.getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                        && !instance.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)
                        && !instance.getAgentName().equals(JobConstants.BRIDGING_JOB))
                    .filter(instance -> filter != null && filter.isPresent()
                        ? instance.getJobName().toLowerCase().contains(filter.get().getJobName().toLowerCase()) ||
                        instance.getChildContextName().toLowerCase().contains(filter.get().getJobName().toLowerCase()) ||
                        (instance.getDisplayName() != null && !instance.getDisplayName().isEmpty() &&
                            instance.getDisplayName().toLowerCase().contains(filter.get().getJobName().toLowerCase()))
                        : true)
                    .sorted(Comparator.comparingInt(SchedulerJob::getOrdinal))
                    .map(instance -> (Object) instance)
                    .collect(Collectors.toList()));
            }
        } else if (node instanceof SchedulerJobInstance) {
            SchedulerJobInstance schedulerJobInstance = (SchedulerJobInstance) node;
            children.addAll(ContextHelper.getPrecedingJobsFromOutsideContext(contextInstance,
                    schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName()
                    , getCommandExecutionJobsForContextInstance(contextInstance.getId()))
                .stream()
                .filter(instance -> !instance.getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                    && !instance.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)
                    && !instance.getAgentName().equals(JobConstants.BRIDGING_JOB))
                .filter(instance -> filter != null && filter.isPresent()
                    ? instance.getJobName().toLowerCase().contains(filter.get().getJobName().toLowerCase()) ||
                    (instance.getDisplayName() != null && !instance.getDisplayName().isEmpty() &&
                        instance.getDisplayName().toLowerCase().contains(filter.get().getJobName().toLowerCase()))
                    : true)
                .map(instance -> (Object) new PrecedingItem(instance))
                .collect(Collectors.toList()));
        }

        children = children.stream().distinct().collect(Collectors.toList());

        if(offset >= 0 && limit > 0 && offset + limit >= children.size()) {
            children = children.subList(offset, children.size());
        }
        else if(offset >= 0 && limit > 0 && offset + limit < children.size()) {
            children = children.subList(offset, offset + limit);
        }

        return children.stream().collect(Collectors.toList());
    }

    /**
     * Add filtering to the tree grid.
     *
     * @param dataProvider the data provider associated with the tree
     */
    private void addGridFiltering(HierarchicalConfigurableFilterDataProvider dataProvider) {

        TreeFilter filter = new TreeFilter();
        dataProvider.setFilter(filter);

        TextField filterField = new TextField();
        filterField.setWidth("100%");
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        filterField.setSuffixComponent(filterIcon);

        filterField.addValueChangeListener(event -> {
            if (event.getValue() == null || event.getValue().isEmpty()) {
                filter.setJobName("");
                dataProvider.refreshAll();
                grid.collapseRecursively(Collections.singleton(contextInstance), 1);
                grid.expandRecursively( Collections.singleton(contextInstance),this.contextInstance.getTreeViewExpandLevel()-1);
                // todo determine if expanding nodes makes sense
                //grid.expand(this.expandedNodes);
            } else {
                filter.setJobName(event.getValue());
                dataProvider.refreshAll();
                grid.expandRecursively(Collections.singleton(contextInstance), 99);
            }
        });

        HeaderRow hr = grid.appendHeaderRow();
        hr.getCell(grid.getColumnByKey("name")).setComponent(filterField);
    }

    /**
     * Helper method to get all command execution jobs associated with an context instance.
     *
     * @param contextInstanceId the id of the context instance that we want the jobs for.
     *
     * @return Map<String, InternalEventDrivenJobInstance> containing the command execution jobs
     * keyed on their identifier.
     */
    private Map<String, InternalEventDrivenJob> getCommandExecutionJobsForContextInstance(String contextInstanceId) {
        Map<String, InternalEventDrivenJob> result = new HashMap<>();
        this.schedulerJobInstanceService
            .getCommandExecutionJobsForContextInstanceChildContext(contextInstanceId)
            .entrySet()
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));

        return result;
    }

    /**
     * Helper method to get all command execution jobs associated with an context instance.
     *
     * @param contextInstanceId the id of the context instance that we want the jobs for.
     *
     * @return Map<String, InternalEventDrivenJobInstance> containing the command execution jobs
     * keyed on their identifier.
     */
    private Map<String, SchedulerJob> getAllSchedulerJobInstancesForContextInstance(String contextInstanceId) {
        Map<String, SchedulerJob> result = new HashMap<>();
        this.schedulerJobInstanceService
            .getSchedulerJobInstancesByContextInstanceId(contextInstanceId, -1, -1, null, null)
            .getResultList()
            .forEach(schedulerJobInstanceRecord
                -> result.put(schedulerJobInstanceRecord.getJobName(), schedulerJobInstanceRecord.getSchedulerJobInstance()));

        return result;
    }

    /**
     * Helper method to get all command execution jobs associated with an context instance.
     *
     * @param contextInstanceId the id of the context instance that we want the jobs for.
     *
     * @return Map<String, InternalEventDrivenJobInstance> containing the command execution jobs
     * keyed on their identifier.
     */
    private Map<String, QuartzScheduleDrivenJobInstance> getQuartzSchedulerJobInstancesForContextInstance(String contextInstanceId) {
        Map<String, QuartzScheduleDrivenJobInstance> result = new HashMap<>();
        this.schedulerJobInstanceService
            .getSchedulerJobInstancesByContextInstanceId(contextInstanceId, -1, -1, null, null)
            .getResultList()
            .stream().filter(schedulerJobInstanceRecord -> schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance)
            .forEach(schedulerJobInstanceRecord
                -> result.put(schedulerJobInstanceRecord.getJobName(), (QuartzScheduleDrivenJobInstance)schedulerJobInstanceRecord.getSchedulerJobInstance()));

        return result;
    }

    private void enableDisableScheduledJobs(ContextInstance contextInstance, UI ui) {
        SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setJobType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        filter.setContextInstanceId(contextInstance.getId());
        SearchResults<SchedulerJobInstanceRecord> results = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter
            (filter, -1, -1, null, null);

        results.getResultList().forEach(scheduledJobInstance -> {
            ComponentKey key = new ComponentKey(contextInstance.getName(),
                scheduledJobInstance.getChildContextName(), scheduledJobInstance.getJobName());
            ComponentKey precedingJobKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+contextInstance.getName(),
                scheduledJobInstance.getChildContextName(), scheduledJobInstance.getJobName());

            if(statusDivMap.containsKey(key)) {
                ui.access(() -> {
                    if(contextInstance.isQuartzScheduleDrivenJobsDisabledForContext()) {
                        statusDivMap.get(key).setStatus(InstanceStatus.DISABLED);
                    }
                    else {
                        statusDivMap.get(key).setStatus(scheduledJobInstance.getStatus());
                    }
                });
            }

            if(statusDivMap.containsKey(precedingJobKey)) {
                ui.access(() -> {
                    if(contextInstance.isQuartzScheduleDrivenJobsDisabledForContext()) {
                        statusDivMap.get(precedingJobKey).setStatus(InstanceStatus.DISABLED);
                    }
                    else {
                        statusDivMap.get(precedingJobKey).setStatus(scheduledJobInstance.getStatus());
                    }
                });
            }
        });
    }

    /**
     * Helper method to manage the representation of all components associated with the a job in the tree.
     *
     * @param ui the current UI
     * @param jobInstanceStateChangeEvent the event received when a jobs state changes.
     */
    private void manageJobStatusStateChangeEvent(UI ui, SchedulerJobInstanceStateChangeEvent jobInstanceStateChangeEvent) {
        if(jobInstanceStateChangeEvent.getSchedulerJobInstance() == null) {
            return;
        }

        if(jobInstanceStateChangeEvent.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_START_JOB) ||
            jobInstanceStateChangeEvent.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
            return;
        }

        if(jobInstanceStateChangeEvent.getPreviousStatus().equals(jobInstanceStateChangeEvent.getNewStatus())) {
            if(!(jobInstanceStateChangeEvent.getSchedulerJobInstance().isErrorAcknowledged() != null
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().isErrorAcknowledged())) {
                return;
            }
        }

        logger.debug("Start manageJobStatusStateChangeEvent " + jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());
        logger.debug("Queue push size " + this.vaadinSession.getPendingAccessQueue().size());
        ComponentKey key;
        ComponentKey precedingJobKey;

        if(jobInstanceStateChangeEvent.getSchedulerJobInstance().getAgentName().equals(JobConstants.GLOBAL_EVENT)) {
            key = new ComponentKey(JobConstants.GLOBAL_EVENT, jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName()
                , jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());

            precedingJobKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+JobConstants.GLOBAL_EVENT,
                jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());
        }
        else {
            key = new ComponentKey(jobInstanceStateChangeEvent.getContextInstance().getName(),
                jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());

            precedingJobKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+jobInstanceStateChangeEvent.getContextInstance().getName(),
                jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());
        }

        SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(jobInstanceStateChangeEvent.getContextInstance().getId()
            , jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName());


        Image statusImage = this.jobImageMap.get(key);

        if(statusImage != null) {
            if(ui.isAttached()) {
                ui.access(() -> {
                    logger.debug(String.format("refreshing status image JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName()
                        , jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(),
                        jobInstanceStateChangeEvent.getNewStatus()));
                    this.setImageBackgroundColour(statusImage, jobInstanceStateChangeEvent.getSchedulerJobInstance());
                });
            }
        }

        Image preccedingJobStatusImage = this.jobImageMap.get(precedingJobKey);

        if(preccedingJobStatusImage != null) {
            if(ui.isAttached()) {
                ui.access(() -> {
                    logger.debug(String.format("refreshing status image JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName()
                        , jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(),
                        jobInstanceStateChangeEvent.getNewStatus()));
                    this.setImageBackgroundColour(preccedingJobStatusImage, jobInstanceStateChangeEvent.getSchedulerJobInstance());
                });
            }
        }

        if(this.schedulerJobIconMap.containsKey(key)) {
            schedulerJobInstanceRecord.setStatus(jobInstanceStateChangeEvent.getNewStatus().name());
            SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
            schedulerJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
            schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
            if(ui.isAttached()) {
                ui.access(() -> this.setIconVisibility(schedulerJobInstanceRecord, key));
            }
        }

        if(this.schedulerJobIconMap.containsKey(precedingJobKey)) {
            schedulerJobInstanceRecord.setStatus(jobInstanceStateChangeEvent.getNewStatus().name());
            SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
            schedulerJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
            schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
            if(ui.isAttached()) {
                ui.access(() -> this.setIconVisibility(schedulerJobInstanceRecord, precedingJobKey));
            }
        }

        if(this.statusDivMap.containsKey(key)) {
            if(ui.isAttached()) {
                ui.access(() -> {
                    if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                        ((InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance()).isKilled()) {
                        this.statusDivMap.get(key).setStatus(InstanceStatus.KILLED);
                    }
                    else {
                        this.statusDivMap.get(key).setStatus(jobInstanceStateChangeEvent.getNewStatus().name()
                            , schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged());
                    }
                });
            }
        }

        if(this.errorAcknowledgedMap.containsKey(key)) {
            if(ui.isAttached()) {
                ui.access(() -> {
                    if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged() != null &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged()) {
                        this.errorAcknowledgedMap.get(key).setVisible(true);
                    }
                    else {
                        this.errorAcknowledgedMap.get(key).setVisible(false);
                    }
                });
            }
        }

        if(this.statusDivMap.containsKey(precedingJobKey)) {
            if(ui.isAttached()) {
                ui.access(() -> {
                    if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                        ((InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance()).isKilled()) {
                        this.statusDivMap.get(precedingJobKey).setStatus(InstanceStatus.KILLED);
                    }
                    else {
                        this.statusDivMap.get(precedingJobKey).setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    }
                });
            }
        }

        if(this.errorAcknowledgedMap.containsKey(precedingJobKey)) {
            if(ui.isAttached()) {
                ui.access(() -> {
                    if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged() != null &&
                        schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged()) {
                        this.errorAcknowledgedMap.get(precedingJobKey).setVisible(true);
                    }
                    else {
                        this.errorAcknowledgedMap.get(precedingJobKey).setVisible(false);
                    }
                });
            }
        }

        if(this.startTimes.containsKey(key) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime() > 0) {
            if(ui.isAttached()) {
                ui.access(() -> this.startTimes.get(key).setText(DateFormatter.instance()
                    .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime())));
            }
        }

        if(this.startTimes.containsKey(precedingJobKey) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime() > 0) {
            if(ui.isAttached()) {
                ui.access(() -> this.startTimes.get(precedingJobKey).setText(DateFormatter.instance()
                    .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime())));
            }
        }

        if(this.endTimes.containsKey(key) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime() > 0) {
            if(ui.isAttached()) {
                ui.access(() -> this.endTimes.get(key).setText(DateFormatter.instance()
                    .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime())));
            }
        }

        if(this.endTimes.containsKey(precedingJobKey) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime() > 0) {
            if(ui.isAttached()) {
                ui.access(() -> this.endTimes.get(precedingJobKey).setText(DateFormatter.instance()
                    .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime())));
            }
        }

        if(this.manuallySubmittedBy.containsKey(key) && schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
            if(ui.isAttached()) {
                ui.access(() -> this.manuallySubmittedBy.get(key).setText(schedulerJobInstanceRecord.getManuallySubmittedBy()));
            }
        }

        if(this.manuallySubmittedBy.containsKey(precedingJobKey) && schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
            if(ui.isAttached()) {
                ui.access(() -> this.manuallySubmittedBy.get(precedingJobKey).setText(schedulerJobInstanceRecord.getManuallySubmittedBy()));
            }
        }

        logger.debug("End manageJobStatusStateChangeEvent " + jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());
    }

    /**
     * Helper method to manage updates related to context components in the tree when a contexts state changes.
     *
     * @param ui the current UI
     * @param contextInstanceStateChangeEvent event received when a context instance state change occurs.
     */
    private void manageContextInstanceStateChangeEvent(UI ui, ContextInstanceStateChangeEvent contextInstanceStateChangeEvent) {
        if(ui == null) return;
        if (contextInstanceStateChangeEvent.getContextInstance() != null && !contextInstanceStateChangeEvent.getPreviousStatus()
                .equals(contextInstanceStateChangeEvent.getNewStatus())) {
            logger.debug("Start manageContextInstanceStateChangeEvent " + contextInstanceStateChangeEvent.getContextInstance().getName());
            logger.debug("Queue push size " + this.vaadinSession.getPendingAccessQueue().size());
            ComponentKey key = new ComponentKey(contextInstance.getName()
                , contextInstanceStateChangeEvent.getContextInstance().getId(), contextInstanceStateChangeEvent.getContextInstance().getName());

            if(this.schedulerStatusFreeTextDivMap.containsKey(key)) {
                if(ui.isAttached()) {
                    ui.access(() -> {
                        this.schedulerStatusFreeTextDivMap.get(key)
                            .setStatus(contextInstanceStateChangeEvent.getNewStatus());
                        this.schedulerStatusFreeTextDivMap.get(key).getElement().getNode().markAsDirty();
                    });
                }
            }
            else {
                this.instanceStatusMap.put(key, contextInstanceStateChangeEvent.getNewStatus());
            }

            logger.debug("End manageContextInstanceStateChangeEvent " + contextInstanceStateChangeEvent.getContextInstance().getName());
        }
    }

    private void manageContextStatusIndicators(UI ui) {
        if(ui == null) return;

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap
            = this.getCommandExecutionJobsForContextInstance(this.contextInstance.getId());

        Map<String, QuartzScheduleDrivenJobInstance> quartzSchedulerJobMap
            = this.getQuartzSchedulerJobInstancesForContextInstance(contextInstance.getId());

        this.statusIconDivMap.keySet().forEach(componentKey -> {
            if (ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()) != null) {
                ContextInstance instance = (ContextInstance) ContextHelper.getChildContext(componentKey.getChildContextName()
                    , ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext());

                if (instance != null) {
                    AggregateContextInstanceStatus aggregateContextInstanceStatus
                        = ContextHelper.getAggregateContextInstanceStatus(instance
                        , internalEventDrivenJobMap, quartzSchedulerJobMap, this.contextInstance);

                    if (componentKey.getJobName().equals(InstanceStatus.ON_HOLD.name())) {
                        if (aggregateContextInstanceStatus.isHeldJobs()) {
                            ui.access(() -> this.statusIconDivMap.get(componentKey).setVisible(true));
                        } else {
                            ui.access(() -> this.statusIconDivMap.get(componentKey).setVisible(false));
                        }
                    } else if (componentKey.getJobName().equals(InstanceStatus.SKIPPED.name())
                        || componentKey.getJobName().equals(InstanceStatus.SKIPPED_COMPLETE.name())
                        || componentKey.getJobName().equals(InstanceStatus.SKIPPED_RUNNING.name())) {
                        if (aggregateContextInstanceStatus.isSkippedJobs()) {
                            ui.access(() -> this.statusIconDivMap.get(componentKey).setVisible(true));
                        } else {
                            ui.access(() -> this.statusIconDivMap.get(componentKey).setVisible(false));
                        }
                    } else if (componentKey.getJobName().equals(InstanceStatus.DISABLED.name())) {
                        if (aggregateContextInstanceStatus.isDisabledJobs()) {
                            ui.access(() -> this.statusIconDivMap.get(componentKey).setVisible(true));
                        } else {
                            ui.access(() -> this.statusIconDivMap.get(componentKey).setVisible(false));
                        }
                    }
                }
            }
        });
    }

    /**
     * Inner class for holding preceding jobs to assist with the tree rendering.
     */
    private class PrecedingItem {
        public PrecedingItem(SchedulerJobInstance schedulerJobInstance) {
            this.schedulerJobInstance = schedulerJobInstance;
        }

        private SchedulerJobInstance schedulerJobInstance;
    }

    /**
     * Inner class to assist with the tree filtering.
     */
    private class TreeFilter {
        private String jobName = "";

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TreeFilter)) return false;
            TreeFilter that = (TreeFilter) o;
            return Objects.equals(jobName, that.jobName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobName);
        }
    }
}
