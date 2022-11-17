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
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobInstanceVisualisationDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cronutils.model.CronType.QUARTZ;

public class ContextInstanceTreeViewWidget extends AbstractGridSchedulerJobInstanceActionWidget {
    private static final String PRECEDING_ITEM_COMPONENT = "PRECEDING_ITEM_COMPONENT";
    private static final String SKIP_ICON = "SKIP_ICON";
    private static final String ENABLE_ICON = "ENABLE_ICON";
    private static final String HOLD_ICON = "HOLD_ICON";
    private static final String RELEASE_ICON = "RELEASE_ICON";
    private static final String SUBMIT_ICON = "SUBMIT_ICON";
    private static final String LOG_ICON = "LOG_ICON";
    private static final String ERROR_LOG_ICON = "ERROR_LOG_ICON";
    private static final String RESET_JOB_ICON = "RESET_JOB_ICON";
    private static final String SUBMIT_DOWNSTREAM_JOBS_ICON = "SUBMIT_DOWNSTREAM_JOBS_ICON";
    private Logger logger = LoggerFactory.getLogger(ContextInstanceTreeViewWidget.class);
    private Registration schedulerJobStateChangeRegistration;
    private Registration contextInstanceStateChangeRegistration;
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
    private ContextInstance contextInstance;
    private Map<ComponentKey, Image> jobImageMap;
    private Map<ComponentKey, Map<String, Icon>> schedulerJobIconMap;
    private Map<ComponentKey, SchedulerStatusDiv> statusDivMap;
    private Map<ComponentKey, SchedulerStatusFreeTextDiv> schedulerStatusFreeTextDivMap;
    private Map<ComponentKey, Div> startTimes;
    private Map<ComponentKey, Div> endTimes;
    private Map<ComponentKey, Div> manuallySubmittedBy;
    private TreeGrid<Object> grid;
    private CronParser parser;
    private CronDescriptor descriptor;
    private ArrayList<Object> expandedNodes;

    public ContextInstanceTreeViewWidget(ContextInstance contextInstance, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                         ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                         MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                         LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                         JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService) {
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

        this.jobImageMap = new HashMap<>();
        this.schedulerJobIconMap = new HashMap<>();
        this.statusDivMap = new HashMap<>();
        this.schedulerStatusFreeTextDivMap = new HashMap<>();
        this.startTimes = new HashMap<>();
        this.endTimes = new HashMap<>();
        this.manuallySubmittedBy = new HashMap<>();
        this.expandedNodes = new ArrayList<>();

        parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
        descriptor = CronDescriptor.instance(Locale.UK);

        ContextHelper.enrichJobs(this.contextInstance);

        setSizeFull();
        this.buildGrid();

        Button collapse = new Button(getTranslation("button.collapse", UI.getCurrent().getLocale()), VaadinIcon.COMPRESS.create());
        collapse.setIconAfterText(true);
        collapse.getElement().getStyle().set("margin-left", "auto");
        collapse.addClickListener(event -> {
            this.grid.collapseRecursively(this.expandedNodes, 1);
        });

        Div div = new Div();
        div.setSizeFull();

        HorizontalLayout layout = new HorizontalLayout();


        layout.add(collapse);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, collapse);

        div.add(layout);
        div.add(this.grid);

        super.add(div);
    }

    /**
     * Helper method to build the underlying grid.
     */
    private void buildGrid() {
        grid = new ExplorerTreeGrid<>();

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap
            = getCommandExecutionJobsForContextInstance(contextInstance.getId());

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

                schedulerStatusFreeTextDiv.setStatus(((ContextInstance)value).getStatus()
                    , ((ContextInstance)value).getName());
                horizontalLayout.add(schedulerStatusFreeTextDiv);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, schedulerStatusFreeTextDiv);
                horizontalLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

                ComponentKey componentKey = new ComponentKey(contextInstance.getName()
                    , ((ContextInstance)value).getId(), ((ContextInstance)value).getName());
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

                if (ContextHelper.getPrecedingJobsFromOutsideContext(contextInstance, schedulerJobInstance.getJobName()
                    , schedulerJobInstance.getChildContextName(), internalEventDrivenJobInstanceMap).size() > 0) {
                    horizontalLayout.add(VaadinIcon.ARROW_RIGHT.create());
                }

                Image image = new Image(this.getJobImage(schedulerJobInstance), "");
                horizontalLayout.add(image);
                image.setHeight("30px");
                this.setImageBackgroundColour(image, schedulerJobInstance.getStatus());
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, image);

                this.jobImageMap.put(new ComponentKey(this.contextInstance.getName()
                    , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName()), image);

                Label jobNameLabel =  new Label(((SchedulerJobInstance)value).getJobName());
                horizontalLayout.add(jobNameLabel);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, jobNameLabel);

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
                this.setImageBackgroundColour(image, schedulerJobInstance.getStatus());
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, image);

                this.jobImageMap.put(new ComponentKey(PRECEDING_ITEM_COMPONENT+this.contextInstance.getName()
                    , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName()), image);

                Label jobNameLabel =  new Label(schedulerJobInstance.getJobName());
                horizontalLayout.add(jobNameLabel);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, jobNameLabel);
            }

            return horizontalLayout;
        })
            .setFlexGrow(4)
            .setHeader(getTranslation("table-header.context-job-name", UI.getCurrent().getLocale()))
            .setKey("name")
            .setResizable(true);

        grid.addComponentColumn(value -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            if (value instanceof ContextInstance) {
                this.getComponentInstanceActionComponents((ContextInstance) value, horizontalLayout);
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

                    if (value instanceof SchedulerJobInstance) {
                        key = new ComponentKey(this.contextInstance.getName(),
                            schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());
                    } else {
                        key = new ComponentKey(PRECEDING_ITEM_COMPONENT + this.contextInstance.getName(),
                            schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), schedulerJobInstanceRecord.getJobName());
                    }

                    logger.debug(String.format("refreshing icons JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName()
                        , schedulerJobInstanceRecord.getSchedulerJobInstance().getContextName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(),
                        schedulerJobInstanceRecord.getStatus()));
                    this.getJobInstanceActionComponents(key, schedulerJobInstanceRecord, horizontalLayout);
                    this.setIconVisibility(schedulerJobInstanceRecord, key);
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
                if(value instanceof SchedulerJobInstance) {
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

                    statusDiv.setStatus(schedulerJobInstance.getStatus());

                    ComponentKey componentKey = new ComponentKey(schedulerJobInstance.getContextName()
                        , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getJobName());
                    this.statusDivMap.put(componentKey, statusDiv);
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

                    statusDiv.setStatus(schedulerJobInstance.getStatus());

                    ComponentKey componentKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+schedulerJobInstance.getContextName()
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

                        this.manuallySubmittedBy.put(key, label);

                        horizontalLayout.add(label);

                        if (schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
                            label.setText(schedulerJobInstanceRecord.getManuallySubmittedBy());
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
        grid.expand(contextInstance.getContexts());

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
     * Helper method to set the background colour on an image in order to reflect the status.
     *
     * @param image the image to set the background colour on.
     * @param instanceStatus the status to be reflected.
     */
    private void setImageBackgroundColour(Image image, InstanceStatus instanceStatus) {
        image.getElement().getStyle().remove("background-color");
        if(instanceStatus.equals(InstanceStatus.COMPLETE)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
        }
        else if(instanceStatus.equals(InstanceStatus.RUNNING) || instanceStatus.equals(InstanceStatus.SKIPPED_RUNNING)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RUNNING);
        }
        else if(instanceStatus.equals(InstanceStatus.WAITING)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_WAITING);
        }
        else if(instanceStatus.equals(InstanceStatus.ERROR)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
        }
        else if(instanceStatus.equals(InstanceStatus.LOCK_QUEUED)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
        }
        else if(instanceStatus.equals(InstanceStatus.SKIPPED) || instanceStatus.equals(InstanceStatus.SKIPPED_COMPLETE)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
        }
        else if(instanceStatus.equals(InstanceStatus.ON_HOLD)) {
            image.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
        }
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
            JobInstanceVisualisationDialog jobInstanceVisualisationDialog = new JobInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
                this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService);

            if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                this.contextInstance = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId()).getContext();
                ContextHelper.enrichJobs(this.contextInstance);
            }

            ContextInstance childContext = ContextHelper.getChildContextInstance(contextInstance.getName(), this.contextInstance);

            try {
                jobInstanceVisualisationDialog.createSchedulerVisualisation(this.contextInstance, childContext);
                jobInstanceVisualisationDialog.open();
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

        return image;
    }

    protected void getComponentInstanceActionComponents(ContextInstance contextInstance, HorizontalLayout horizontalLayout) {
        if(contextInstance.getScheduledJobs() != null
            && !contextInstance.getScheduledJobs().isEmpty()){
            horizontalLayout.add(this.createContextVisualisationIcon(contextInstance));
        }
        Icon hold = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-all-nested-jobs", UI.getCurrent().getLocale()
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        hold.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.hold-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.hold-jobs-body", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                boolean error = false;
                try {
                    if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                        List<SchedulerJobInstanceRecord> updatedJobs = schedulerJobInstanceService.holdJobsWithinContext(ContextMachineCache
                            .instance().getByContextInstanceId(this.contextInstance.getId()).getContext(), contextInstance.getName());

                        if (updatedJobs.size() > 0) {
                            updatedJobs.forEach(schedulerJobInstanceRecord -> {
                                SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
                                    = new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstanceRecord.getSchedulerJobInstance(),
                                    this.contextInstance, InstanceStatus.WAITING, InstanceStatus.ON_HOLD);
                                SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
                            });
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    error = true;
                }
                finally {
                    if (error) {
                        NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-hold-error"
                            , UI.getCurrent().getLocale()));
                    } else {
                        NotificationHelper.showUserNotification(getTranslation("notification.all-jobs-successfully-held"
                            , UI.getCurrent().getLocale()));
                    }
                }
            });
        });

        horizontalLayout.add(hold);

        Icon release = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-all-nested-jobs", UI.getCurrent().getLocale()
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        release.addClickListener(event -> {
            List<SchedulerJobInstanceRecord> jobsToReleaseWithinContext = schedulerJobInstanceService.getJobsToReleaseWithinContext(ContextMachineCache
                .instance().getByContextInstanceId(this.contextInstance.getId()).getContext(), contextInstance.getName());

            if(jobsToReleaseWithinContext.size() > 0) {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.release-jobs-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(String.format(getTranslation("confirm-dialog.release-jobs-body", UI.getCurrent().getLocale())
                    , jobsToReleaseWithinContext.size()));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                    dialog.setWidth("600px");
                    dialog.setHeight("250px");
                    dialog.open(getTranslation("progress-dialog.release-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                        getTranslation("progress-dialog.release-all-jobs-jobs-body", UI.getCurrent().getLocale()));

                    final UI current = UI.getCurrent();
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        boolean error = false;
                        try {
                            if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                                if (jobsToReleaseWithinContext.size() > 0) {
                                    for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : jobsToReleaseWithinContext) {
                                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                                        contextMachine.releaseJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                                            schedulerJobInstanceRecord.getChildContextName());
                                    }
                                }
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
        });

        horizontalLayout.add(release);
    }

    /**
     * Method to initialise and create all action icons for a given job record. All icons are added to the provided layout.
     *
     * @param key
     * @param schedulerJobInstanceRecord
     * @param layout
     */
    protected void getJobInstanceActionComponents(ComponentKey key, SchedulerJobInstanceRecord schedulerJobInstanceRecord, HorizontalLayout layout) {
        layout.setWidthFull();

        if(!this.schedulerJobIconMap.containsKey(key)) {
            this.schedulerJobIconMap.put(key, new HashMap<>());
        }

        Map<String, Icon> iconMap = this.schedulerJobIconMap.get(key);

        Icon modal = IconDecorator.decorate(new Icon(VaadinIcon.MODAL), getTranslation("tooltip.open-visualisation"
            , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
        modal.addClickListener(event -> {
            SchedulerJobInstanceRecord refreshedRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(schedulerJobInstanceRecord.getContextInstanceId(),
                schedulerJobInstanceRecord.getJobName(), schedulerJobInstanceRecord.getChildContextName());
            if(refreshedRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                FileEventJobInstanceDialog fileEventJobInstanceDialog
                    = new FileEventJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName()),
                    this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService);
                fileEventJobInstanceDialog.setJob(refreshedRecord);
                fileEventJobInstanceDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance) {
                QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobInstanceDialog
                    = new QuartzDrivenScheduledJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName()),
                    this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService);
                quartzDrivenScheduledJobInstanceDialog.setJob(refreshedRecord);
                quartzDrivenScheduledJobInstanceDialog.open();
            }
            else if(refreshedRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                InternalEventDrivenJobInstanceDialog internalEventDrivenJobDialog
                    = new InternalEventDrivenJobInstanceDialog(this.moduleMetaDataService.findById(refreshedRecord.getSchedulerJobInstance().getAgentName())
                    , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService
                    , this.systemEventLogger, this.schedulerJobInstanceService, this.contextInstance, this.jobInitiationService, this.moduleMetaDataService
                    , this.logStreamingService, this.jobUtilsService);
                internalEventDrivenJobDialog.setJob(refreshedRecord);
                internalEventDrivenJobDialog.open();
            }
        });

        layout.add(modal);

        Icon skip;
        if(!iconMap.containsKey(SKIP_ICON)) {
            skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");

            skip.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));

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
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));

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
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));

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
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));

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
                if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    InternalEventDrivenJobSubmissionDialog internalEventDrivenJobSubmissionDialog = new InternalEventDrivenJobSubmissionDialog(this.systemEventLogger,
                        this.moduleMetaDataService, this.contextInstance, this.jobInitiationService, schedulerJobInstanceRecord, this.schedulerJobInstanceService);

                    internalEventDrivenJobSubmissionDialog.open();
                } else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-file-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-file-job", UI.getCurrent().getLocale()));

                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                            schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                            schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                            this.jobInitiationService.raiseFileEventSchedulerJob(agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getJobName());

                            logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                                , this.authentication.getName());

                            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                        }
                    });
                } else if (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance) {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-quartz-job", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm-dialog-text.submit-quartz-job", UI.getCurrent().getLocale()));

                    confirmDialog.setCancelable(true);

                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        try {
                            ModuleMetaData agent = this.moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
                            this.jobInitiationService.raiseQuartzSchedulerJob(agent.getUrl(), agent.getName(), schedulerJobInstanceRecord.getJobName());

                            logger.info("Submitting job[{}] to [{}]", schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), agent.getUrl());

                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                                , this.authentication.getName());

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

        Icon reset;

        if(!iconMap.containsKey(RESET_JOB_ICON)) {
            reset = IconDecorator.decorate(new Icon(VaadinIcon.ARROW_BACKWARD), getTranslation("tooltip.reset-job"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            reset.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.reset-job-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.reset-job-body", UI.getCurrent().getLocale()));

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

        Icon submitDownstreamJobs;

        if(!iconMap.containsKey(SUBMIT_DOWNSTREAM_JOBS_ICON)) {
            submitDownstreamJobs = IconDecorator.decorate(new Icon(VaadinIcon.FAST_FORWARD), getTranslation("button.initiate-downstream-jobs"
                , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            submitDownstreamJobs.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
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
     * Helper method to set the visibility of all action items associated with a individual job.
     *
     * @param schedulerJobInstanceRecord the job we are setting the action visibility for.
     * @param key the key to the map containing icons for the job
     */
    protected void setIconVisibility(SchedulerJobInstanceRecord schedulerJobInstanceRecord, ComponentKey key) {
        Map<String, Icon> iconMap = this.schedulerJobIconMap.get(key);
        Icon skip = iconMap.get(SKIP_ICON);


        if (skip != null) {
            if (schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                !((schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED_COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.LOCK_QUEUED))) {
                skip.setVisible(true);
            } else {
                skip.setVisible(false);

            }
        }

        Icon enable = iconMap.get(ENABLE_ICON);

        if(enable != null) {
            if (schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
                (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                    !(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)))) {
                enable.setVisible(false);
            } else if (schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
                enable.setVisible(true);
            }
        }


        Icon hold = iconMap.get(HOLD_ICON);
        if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
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
        else if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
           hold.setVisible(true);
        }

        Icon release = iconMap.get(RELEASE_ICON);

        if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            !schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD)) {
            release.setVisible(false);
        }
        else if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
            release.setVisible(true);
        }

        Icon logFile = iconMap.get(LOG_ICON);
        logFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)));

        Icon errorLogFile = iconMap.get(ERROR_LOG_ICON);
        errorLogFile.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.RUNNING) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)));

        Icon reset = iconMap.get(RESET_JOB_ICON);
        reset.setVisible((schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) ||
            schedulerJobInstanceRecord.getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) &&
            (schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE)
                || schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR)));

        Icon submitDownstreamJobs = iconMap.get(SUBMIT_DOWNSTREAM_JOBS_ICON);
        submitDownstreamJobs.setVisible(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR));

        Icon submit = iconMap.get(SUBMIT_ICON);
        submit.setVisible(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.WAITING));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            manageJobStatusStateChangeEvent(ui, jobInstanceStateChangeEvent);
        });

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            this.manageContextInstanceStateChangeEvent(ui, contextInstanceStateChangeEvent);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.schedulerJobStateChangeRegistration != null) {
            this.schedulerJobStateChangeRegistration.remove();
            this.schedulerJobStateChangeRegistration = null;
        }

        if(this.contextInstanceStateChangeRegistration != null) {
            this.contextInstanceStateChangeRegistration.remove();
            this.contextInstanceStateChangeRegistration = null;
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
                    return getNodeChildren(query.getParent(), query.getFilter()).size();
                }
                // checks if a given item should be expandable
                @Override
                public boolean hasChildren(Object item) {
                    List<Object> children = new ArrayList<>();
                    if(item instanceof ContextInstance) {
                        if (((ContextInstance)item).getContexts() != null
                            && !((ContextInstance)item).getContexts().isEmpty()) {
                            children.addAll(((ContextInstance)item).getContexts().stream()
                                .map(instance -> (Object) instance)
                                .collect(Collectors.toList()));
                        }

                        if (((ContextInstance)item).getScheduledJobs() != null
                            && !((ContextInstance)item).getScheduledJobs().isEmpty()) {
                            children.addAll(((ContextInstance)item).getScheduledJobs().stream()
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
                            .map(instance -> (Object) new PrecedingItem(instance))
                            .collect(Collectors.toList()));
                    }
                    return children.stream().distinct().collect(Collectors.toList()).size() > 0;
                }

                // returns the immediate child items based on offset and limit
                @Override
                protected Stream<Object> fetchChildrenFromBackEnd(HierarchicalQuery<Object, TreeFilter> query) {
                    return getNodeChildren(query.getParent(), query.getFilter()).stream();
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
    private List<Object> getNodeChildren(Object node, Optional<TreeFilter> filter) {
        List<Object> children = new ArrayList<>();
        if(node == null) {
            if (contextInstance.getContexts() != null
                && !(contextInstance.getContexts().isEmpty())) {
                if(filter != null && filter.isPresent() && filter.get().getJobName().isEmpty()) {
                    children.addAll(contextInstance.getContexts().stream()
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                }
                else {
                    children.addAll(contextInstance.getContexts().stream()
                        .filter(instance -> ContextHelper.getContextsWhereJobFilterMatchResides
                            (instance, filter.get().getJobName()).size() > 0)
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                }
            }

            if (contextInstance.getScheduledJobs() != null
                && !contextInstance.getScheduledJobs().isEmpty()) {
                children.addAll(contextInstance.getScheduledJobs().stream()
                    .filter(instance -> filter != null && filter.isPresent()
                        ? instance.getJobName().toLowerCase().contains(filter.get().getJobName().toLowerCase())
                        : true)
                    .map(instance -> (Object) instance)
                    .collect(Collectors.toList()));
            }
        }
        else if(node instanceof ContextInstance) {
            if (((ContextInstance)node).getContexts() != null
                && !((ContextInstance)node).getContexts().isEmpty()) {
                if(filter != null && filter.isPresent() && filter.get().getJobName().isEmpty()) {
                    children.addAll(((ContextInstance) node).getContexts().stream()
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                }
                else {
                    children.addAll(((ContextInstance) node).getContexts().stream()
                        .filter(instance -> ContextHelper.getContextsWhereJobFilterMatchResides
                            (instance, filter.get().getJobName()).size() > 0)
                        .map(instance -> (Object) instance)
                        .collect(Collectors.toList()));
                }
            }

            if (((ContextInstance)node).getScheduledJobs() != null
                && !((ContextInstance)node).getScheduledJobs().isEmpty()) {
                children.addAll(((ContextInstance)node).getScheduledJobs().stream()
                    .filter(instance -> filter != null && filter.isPresent()
                        ? instance.getJobName().toLowerCase().contains(filter.get().getJobName().toLowerCase())
                        : true)
                    .map(instance -> (Object) instance)
                    .collect(Collectors.toList()));
            }
        }
        if(node instanceof SchedulerJobInstance) {
            SchedulerJobInstance schedulerJobInstance = (SchedulerJobInstance) node;
            children.addAll(ContextHelper.getPrecedingJobsFromOutsideContext(contextInstance,
                    schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName()
                    , getCommandExecutionJobsForContextInstance(contextInstance.getId()))
                .stream()
                .filter(instance -> filter != null && filter.isPresent()
                    ? instance.getJobName().toLowerCase().contains(filter.get().getJobName().toLowerCase())
                    : true)
                .map(instance -> (Object) new PrecedingItem(instance))
                .collect(Collectors.toList()));
        }
        return children.stream().distinct().collect(Collectors.toList());
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
                grid.expand(this.expandedNodes);
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
    private Map<String, InternalEventDrivenJobInstance> getCommandExecutionJobsForContextInstance(String contextInstanceId) {
        return this.schedulerJobInstanceService.getCommandExecutionJobsForContextInstanceChildContext(contextInstanceId);
    }

    /**
     * Helper method to manage the representation of all components associated with the a job in the tree.
     *
     * @param ui the current UI
     * @param jobInstanceStateChangeEvent the event received when a jobs state changes.
     */
    private void manageJobStatusStateChangeEvent(UI ui, SchedulerJobInstanceStateChangeEvent jobInstanceStateChangeEvent) {
        ComponentKey key = new ComponentKey(jobInstanceStateChangeEvent.getContextInstance().getName(),
            jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());
        ComponentKey precedingJobKey = new ComponentKey(PRECEDING_ITEM_COMPONENT+jobInstanceStateChangeEvent.getContextInstance().getName(),
            jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());


        SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(jobInstanceStateChangeEvent.getContextInstance().getId()
            , jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName());


        Image statusImage = this.jobImageMap.get(key);

        if(statusImage != null) {
            ui.access(() -> {
                logger.debug(String.format("refreshing status image JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName()
                    , jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(),
                    jobInstanceStateChangeEvent.getNewStatus()));
                this.setImageBackgroundColour(statusImage, jobInstanceStateChangeEvent.getNewStatus());
            });
        }

        Image preccedingJobStatusImage = this.jobImageMap.get(precedingJobKey);

        if(preccedingJobStatusImage != null) {
            ui.access(() -> {
                logger.debug(String.format("refreshing status image JobName[%s], ContextName[%s], ChildContextName[%s], Status[%s]", jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName()
                    , jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextName(), jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName(),
                    jobInstanceStateChangeEvent.getNewStatus()));
                this.setImageBackgroundColour(preccedingJobStatusImage, jobInstanceStateChangeEvent.getNewStatus());
            });
        }

        if(this.schedulerJobIconMap.containsKey(key)) {
            schedulerJobInstanceRecord.setStatus(jobInstanceStateChangeEvent.getNewStatus().name());
            SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
            schedulerJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
            schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
            ui.access(() -> this.setIconVisibility(schedulerJobInstanceRecord, key));
        }

        if(this.schedulerJobIconMap.containsKey(precedingJobKey)) {
            schedulerJobInstanceRecord.setStatus(jobInstanceStateChangeEvent.getNewStatus().name());
            SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
            schedulerJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
            schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
            ui.access(() -> this.setIconVisibility(schedulerJobInstanceRecord, precedingJobKey));
        }

        if(this.statusDivMap.containsKey(key)) {
            ui.access(() -> this.statusDivMap.get(key).setStatus(jobInstanceStateChangeEvent.getNewStatus()));
        }

        if(this.statusDivMap.containsKey(precedingJobKey)) {
            ui.access(() -> this.statusDivMap.get(precedingJobKey).setStatus(jobInstanceStateChangeEvent.getNewStatus()));
        }

        if(this.startTimes.containsKey(key) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime() > 0) {
            ui.access(() -> this.startTimes.get(key).setText(DateFormatter.instance()
                .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime())));
        }

        if(this.startTimes.containsKey(precedingJobKey) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime() > 0) {
            ui.access(() -> this.startTimes.get(precedingJobKey).setText(DateFormatter.instance()
                .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime())));
        }

        if(this.endTimes.containsKey(key) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime() > 0) {
            ui.access(() -> this.endTimes.get(key).setText(DateFormatter.instance()
                .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime())));
        }

        if(this.endTimes.containsKey(precedingJobKey) && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime() > 0) {
            ui.access(() -> this.endTimes.get(precedingJobKey).setText(DateFormatter.instance()
                .getFormattedDate(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime())));
        }

        if(this.manuallySubmittedBy.containsKey(key) && schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
            ui.access(() -> this.manuallySubmittedBy.get(key).setText(schedulerJobInstanceRecord.getManuallySubmittedBy()));
        }

        if(this.manuallySubmittedBy.containsKey(precedingJobKey) && schedulerJobInstanceRecord.getManuallySubmittedBy() != null) {
            ui.access(() -> this.manuallySubmittedBy.get(precedingJobKey).setText(schedulerJobInstanceRecord.getManuallySubmittedBy()));
        }
    }

    /**
     * Helper method to manage updates related to context components in the tree when a contexts state changes.
     *
     * @param ui the current UI
     * @param contextInstanceStateChangeEvent event received when a context instance state change occurs.
     */
    private void manageContextInstanceStateChangeEvent(UI ui, ContextInstanceStateChangeEvent contextInstanceStateChangeEvent) {
        if (contextInstanceStateChangeEvent.getContextInstance() != null) {

            ComponentKey key = new ComponentKey(contextInstance.getName()
                , contextInstanceStateChangeEvent.getContextInstance().getId(), contextInstanceStateChangeEvent.getContextInstance().getName());

            if(this.schedulerStatusFreeTextDivMap.containsKey(key)) {
                ui.access(() -> this.schedulerStatusFreeTextDivMap.get(key)
                    .setStatus(contextInstanceStateChangeEvent.getNewStatus()));
            }
        }
    }

    private class PrecedingItem {
        public PrecedingItem(SchedulerJobInstance schedulerJobInstance) {
            this.schedulerJobInstance = schedulerJobInstance;
        }

        private SchedulerJobInstance schedulerJobInstance;
    }

    private class TreeFilter {
        private String jobName = "";

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }
    }
}
