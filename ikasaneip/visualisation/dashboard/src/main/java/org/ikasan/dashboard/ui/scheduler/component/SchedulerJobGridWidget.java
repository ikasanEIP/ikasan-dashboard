package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SchedulerJobGridWidget extends Div {

    private SchedulerJobFilteringGrid schedulerJobFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private SystemEventLogger systemEventLogger;
    private ModuleMetaDataService moduleMetaDataService;
    private JobInitiationService jobInitiationService;
    private ContextTemplate contextTemplate;
    private ModuleControlService moduleControlRestService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private MetaDataService metaDataRestService;
    private SchedulerJobService schedulerJobService;
    private JobProvisionService jobProvisionService;

    /**
     * Constructor
     */
    public SchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, ContextTemplate contextTemplate, JobInitiationService jobInitiationService,
                                  JobProvisionService jobProvisionService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.systemEventLogger = systemEventLogger;
        this.moduleMetaDataService = moduleMetaDataService;
        this.jobInitiationService = jobInitiationService;
        this.contextTemplate = contextTemplate;
        this.moduleControlRestService = moduleControlRestService;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.metaDataRestService = metaDataRestService;
        this.schedulerJobService =  schedulerJobService;
        this.jobProvisionService =  jobProvisionService;

        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate);

        this.schedulerJobFilteringGrid.init();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.add(this.createButtonLayout(), this.schedulerJobFilteringGrid);

        this.add(layout);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService, ContextTemplate contextTemplate) {
        // Create a modulesGrid bound to the list
        SolrSchedulerJobSearchFilterImpl schedulerJobSearchFilter = new SolrSchedulerJobSearchFilterImpl();
        schedulerJobFilteringGrid = new SchedulerJobFilteringGrid(schedulerJobService, schedulerJobSearchFilter);
        schedulerJobFilteringGrid.getElement().getStyle().set("margin-top", "40px");
        schedulerJobFilteringGrid.removeAllColumns();
        schedulerJobFilteringGrid.setVisible(true);
        schedulerJobFilteringGrid.setWidthFull();
        schedulerJobFilteringGrid.setHeight("75vh");
        schedulerJobFilteringGrid.setContextName(contextTemplate.getName());


        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getJobName());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("flowName")
            .setFlexGrow(3);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(schedulerJobRecord.getType()));

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("type")
            .setFlexGrow(2);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), "Delete job template", "14pt", "rgba(0, 0, 0, 1.0)");
            delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setCancelable(true);
                confirmDialog.setHeader(getTranslation("confirm-dialog.delete-job-template-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.delete-job-template-body", UI.getCurrent().getLocale()));

                confirmDialog.addConfirmListener(event -> {
                    this.schedulerJobService.delete(schedulerJobRecord);
                    this.schedulerJobFilteringGrid.refresh();
                });

                confirmDialog.open();
            });

            layout.add(delete);

            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            StreamResource streamResource = new StreamResource(schedulerJobRecord.getJobName()+".json"
                , () -> {
                try {
                    return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedulerJobRecord.getJob()));
                }
                catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);
            layout.add(exportWrapper);

            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(1);

        this.schedulerJobFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobRecord>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobRecord>of(
            "<div>[[item.modified]]</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true)
        .setFlexGrow(1);

        this.schedulerJobFilteringGrid.addItemDoubleClickListener(event -> {
            if(event.getItem().getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB)) {
                FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(moduleMetaDataService.findById(event.getItem().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                fileEventJobDialog.setJob(event.getItem(), EditMode.EDIT);

                fileEventJobDialog.open();

                fileEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });
            }
            else if(event.getItem().getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB)) {
                QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(moduleMetaDataService.findById(event.getItem().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                quartzDrivenScheduledJobDialog.setJob(event.getItem(), EditMode.EDIT);

                quartzDrivenScheduledJobDialog.open();

                quartzDrivenScheduledJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });
            }
            else if(event.getItem().getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)) {
                InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(moduleMetaDataService.findById(event.getItem().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                internalEventDrivenJobDialog.setJob(event.getItem(), EditMode.EDIT);

                internalEventDrivenJobDialog.open();

                internalEventDrivenJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobFilteringGrid.refresh();
                    }
                });

            }
        });

        HeaderRow hr = schedulerJobFilteringGrid.appendHeaderRow();
        this.schedulerJobFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setJobNameFilter, "flowName");
        this.schedulerJobFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setJobTypeFilter
            , SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");

    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);
        buttonLayout.getElement().getStyle().set("position", "absolute");
        buttonLayout.getElement().getStyle().set("right", "30px");
        buttonLayout.getElement().getStyle().set("margin-top", "0px");
        Button refreshButton = this.createRefreshButton();
        Button provisionButton = this.createProvisionButton();
        buttonLayout.add(this.createJobUploadMenuBar(), this.createNewJobMenuBar(), provisionButton, refreshButton);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, refreshButton, provisionButton);

        return buttonLayout;
    }

    private Button createRefreshButton() {
        Button refreshJobsButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshJobsButton.setIconAfterText(true);

        refreshJobsButton.addClickListener(event -> this.schedulerJobFilteringGrid.init());

        return refreshJobsButton;
    }

    private Button createProvisionButton() {
        Button provisionJobsButton = new Button("Provision Jobs", VaadinIcon.BOAT.create());
        provisionJobsButton.setIconAfterText(true);

        provisionJobsButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setCancelable(true);
            confirmDialog.setHeader("Provision Jobs");
            confirmDialog.setText("By confirming, all job templates that are associated with this context template will " +
                "be provisioned on the scheduler agents that they are associated with.");

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.setWidth("600px");
                dialog.setHeight("400px");
                dialog.open("Provisioning Jobs");

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        SearchResults<SchedulerJobRecord> jobRecords = this.schedulerJobService.findByContext(this.contextTemplate.getName(), -1, -1);

                        List<SchedulerJob> schedulerJobs = jobRecords.getResultList().stream()
                            .map(record -> record.getJob())
                            .map(job -> {
                                if (job instanceof InternalEventDrivenJob) {
                                    InternalEventDrivenJobImpl internalEventDrivenJob = new InternalEventDrivenJobImpl();
                                    internalEventDrivenJob.setIdentifier(job.getIdentifier());
                                    internalEventDrivenJob.setCommandLine(((InternalEventDrivenJob) job).getCommandLine());
                                    internalEventDrivenJob.setContextParameters(((InternalEventDrivenJob) job).getContextParameters()
                                        .stream()
                                        .map(p -> {
                                            ContextParameterImpl contextParameter = new ContextParameterImpl();
                                            contextParameter.setName(p.getName());
                                            contextParameter.setType(p.getType());

                                            return contextParameter;
                                        }).collect(Collectors.toList()));
                                    internalEventDrivenJob.setDaysOfWeekToRun(((InternalEventDrivenJob) job).getDaysOfWeekToRun());
                                    internalEventDrivenJob.setMaxExecutionTime(((InternalEventDrivenJob) job).getMaxExecutionTime());
                                    internalEventDrivenJob.setMinExecutionTime(((InternalEventDrivenJob) job).getMinExecutionTime());
                                    internalEventDrivenJob.setSuccessfulReturnCodes(((InternalEventDrivenJob) job).getSuccessfulReturnCodes());
                                    internalEventDrivenJob.setWorkingDirectory(((InternalEventDrivenJob) job).getWorkingDirectory());
                                    internalEventDrivenJob.setAgentName(job.getAgentName());
                                    internalEventDrivenJob.setChildContextIds(job.getChildContextIds());
                                    internalEventDrivenJob.setContextId(job.getContextId());
                                    internalEventDrivenJob.setChildContextIds(job.getChildContextIds());
                                    internalEventDrivenJob.setStartupControlType(job.getStartupControlType());
                                    internalEventDrivenJob.setJobName(job.getJobName());
                                    internalEventDrivenJob.setJobDescription(job.getJobDescription());

                                    return internalEventDrivenJob;
                                } else if (job instanceof FileEventDrivenJob) {
                                    FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
                                    fileEventDrivenJob.setContextId(job.getContextId());
                                    fileEventDrivenJob.setDirectoryDepth(((FileEventDrivenJob) job).getDirectoryDepth());
                                    fileEventDrivenJob.setEncoding(((FileEventDrivenJob) job).getEncoding());
                                    fileEventDrivenJob.setFilenames(((FileEventDrivenJob) job).getFilenames());
                                    fileEventDrivenJob.setFilePath(((FileEventDrivenJob) job).getFilePath());
                                    fileEventDrivenJob.setIgnoreFileRenameWhilstScanning(((FileEventDrivenJob) job).isIgnoreFileRenameWhilstScanning());
                                    fileEventDrivenJob.setIncludeHeader(((FileEventDrivenJob) job).isIncludeHeader());
                                    fileEventDrivenJob.setIncludeTrailer(((FileEventDrivenJob) job).isIncludeTrailer());
                                    fileEventDrivenJob.setLogMatchedFilenames(((FileEventDrivenJob) job).isLogMatchedFilenames());
                                    fileEventDrivenJob.setMinFileAgeSeconds(((FileEventDrivenJob) job).getMinFileAgeSeconds());
                                    fileEventDrivenJob.setMoveDirectory(((FileEventDrivenJob) job).getMoveDirectory());
                                    fileEventDrivenJob.setSortAscending(((FileEventDrivenJob) job).isSortAscending());
                                    fileEventDrivenJob.setSortByModifiedDateTime(((FileEventDrivenJob) job).isSortByModifiedDateTime());
                                    fileEventDrivenJob.setAgentName(job.getAgentName());
                                    fileEventDrivenJob.setChildContextIds(job.getChildContextIds());
                                    fileEventDrivenJob.setCronExpression(((FileEventDrivenJob) job).getCronExpression());
                                    fileEventDrivenJob.setEager(((FileEventDrivenJob) job).isEager());
                                    fileEventDrivenJob.setIdentifier(job.getIdentifier());
                                    fileEventDrivenJob.setIgnoreMisfire(((FileEventDrivenJob) job).isIgnoreMisfire());
                                    fileEventDrivenJob.setJobGroup(((FileEventDrivenJob) job).getJobGroup());
                                    fileEventDrivenJob.setMaxEagerCallbacks(((FileEventDrivenJob) job).getMaxEagerCallbacks());
                                    fileEventDrivenJob.setTimeZone(((FileEventDrivenJob) job).getTimeZone());
                                    fileEventDrivenJob.setPassthroughProperties(((FileEventDrivenJob) job).getPassthroughProperties());
                                    fileEventDrivenJob.setPersistentRecovery(((FileEventDrivenJob) job).isPersistentRecovery());
                                    fileEventDrivenJob.setRecoveryTolerance(((FileEventDrivenJob) job).getRecoveryTolerance());
                                    fileEventDrivenJob.setStartupControlType(job.getStartupControlType());
                                    fileEventDrivenJob.setJobName(job.getJobName());

                                    return fileEventDrivenJob;
                                } else {
                                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
                                    quartzScheduleDrivenJob.setContextId(job.getContextId());
                                    quartzScheduleDrivenJob.setCronExpression(((QuartzScheduleDrivenJob) job).getCronExpression());
                                    quartzScheduleDrivenJob.setEager(((QuartzScheduleDrivenJob) job).isEager());
                                    quartzScheduleDrivenJob.setIgnoreMisfire(((QuartzScheduleDrivenJob) job).isIgnoreMisfire());
                                    quartzScheduleDrivenJob.setJobGroup(((QuartzScheduleDrivenJob) job).getJobGroup());
                                    quartzScheduleDrivenJob.setMaxEagerCallbacks(((QuartzScheduleDrivenJob) job).getMaxEagerCallbacks());
                                    quartzScheduleDrivenJob.setPassthroughProperties(((QuartzScheduleDrivenJob) job).getPassthroughProperties());
                                    quartzScheduleDrivenJob.setPersistentRecovery(((QuartzScheduleDrivenJob) job).isPersistentRecovery());
                                    quartzScheduleDrivenJob.setRecoveryTolerance(((QuartzScheduleDrivenJob) job).getRecoveryTolerance());
                                    quartzScheduleDrivenJob.setStartupControlType(job.getStartupControlType());
                                    quartzScheduleDrivenJob.setJobName(job.getJobName());
                                    quartzScheduleDrivenJob.setJobDescription(job.getJobDescription());
                                    quartzScheduleDrivenJob.setIdentifier(job.getIdentifier());
                                    quartzScheduleDrivenJob.setChildContextIds(job.getChildContextIds());
                                    quartzScheduleDrivenJob.setAgentName(job.getAgentName());
                                    quartzScheduleDrivenJob.setTimeZone(((QuartzScheduleDrivenJob) job).getTimeZone());

                                    return quartzScheduleDrivenJob;
                                }
                            })
                            .collect(Collectors.toList());

                        this.jobProvisionService.provisionJobs(schedulerJobs);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        current.access(() ->NotificationHelper.showErrorNotification("An error has occurred provisioning jobs. Please contact Ikasan support."));
                    }
                    finally {
                        current.access(() -> dialog.close());
                    }
                });

            });
        });

        return provisionJobsButton;
    }

    private MenuBar createJobUploadMenuBar() {
        MenuBar uploadJobMenuBar = new MenuBar();
        uploadJobMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem quickAccess = createIconItem(uploadJobMenuBar, VaadinIcon.UPLOAD_ALT, getTranslation("menu-item.upload-job-template", UI.getCurrent().getLocale()));

        SubMenu activeContextInstancesSubMenu = quickAccess.getSubMenu();
        MenuItem activeContexts = activeContextInstancesSubMenu.addItem(getTranslation("menu-item.job-type", UI.getCurrent().getLocale()));
        SubMenu activeContextSubMenu = activeContexts.getSubMenu();

        activeContextSubMenu.addItem("Command Execution Job", event -> {});
        activeContextSubMenu.addItem("File Watcher Job", event -> {});
        activeContextSubMenu.addItem("Scheduled Job", event -> {});

        return uploadJobMenuBar;
    }

    private MenuBar createNewJobMenuBar() {
        MenuBar newJobMenuBar = new MenuBar();
        newJobMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem quickAccess = createIconItem(newJobMenuBar, VaadinIcon.PLUS, getTranslation("menu-item.create-new-job", UI.getCurrent().getLocale()));

        SubMenu activeContextInstancesSubMenu = quickAccess.getSubMenu();
        MenuItem activeContexts = activeContextInstancesSubMenu.addItem(getTranslation("menu-item.job-type", UI.getCurrent().getLocale()));
        SubMenu activeContextSubMenu = activeContexts.getSubMenu();

        activeContextSubMenu.addItem(getTranslation("menu-item.command-execution-job", UI.getCurrent().getLocale()), event -> {
            InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

            InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
            internalEventDrivenJob.setContextId(this.contextTemplate.getName());

            internalEventDrivenJobDialog.setJob(internalEventDrivenJob, EditMode.NEW);
            internalEventDrivenJobDialog.open();

            internalEventDrivenJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened()) {
                    this.schedulerJobFilteringGrid.refresh();
                }
            });
        });
        activeContextSubMenu.addItem(getTranslation("menu-item.file-watcher-job", UI.getCurrent().getLocale()), event -> {
            FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

            FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
            fileEventDrivenJob.setContextId(contextTemplate.getName());

            fileEventJobDialog.setJob(fileEventDrivenJob, EditMode.NEW);

            fileEventJobDialog.open();

            fileEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened()) {
                    this.schedulerJobFilteringGrid.refresh();
                }
            });
        });
        activeContextSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale()), event -> {
            QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);

            QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
            quartzScheduleDrivenJob.setContextId(this.contextTemplate.getName());

            quartzDrivenScheduledJobDialog.setJob(quartzScheduleDrivenJob, EditMode.NEW);

            quartzDrivenScheduledJobDialog.open();

            quartzDrivenScheduledJobDialog  .addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened()) {
                    this.schedulerJobFilteringGrid.refresh();
                }
            });
        });

        return newJobMenuBar;
    }

    private MenuItem createIconItem(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);

        Button menuButton = new Button(label, icon);
        menuButton.setIconAfterText(true);

        MenuItem item = menu.addItem(menuButton);
        item.getElement().getStyle().set("padding", "0px");
        item.getElement().getStyle().set("padding-right", "5px");

        return item;
    }
}
