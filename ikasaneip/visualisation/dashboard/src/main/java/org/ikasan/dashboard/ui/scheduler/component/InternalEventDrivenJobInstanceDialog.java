package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToLongConverter;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class InternalEventDrivenJobInstanceDialog extends AbstractCloseableResizableDialog implements SchedulerJobStateChangeEventLocalBroadcastListener {

    Logger logger = LoggerFactory.getLogger(InternalEventDrivenJobInstanceDialog.class);

    private JsonMapper objectMapper = ObjectMapperFactory.newInstance();

    private TextField agentTf;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobNameAliasTf;
    private TextArea jobDescriptionTa;


    // Fields to capture job execution properties.
    private AceEditor commandLineTa;
    private TextField workingDirectoryTf;
    private TextField minExecutionTimeTf;
    private TextField maxExecutionTimeTf;
    private TextField executionEnvironmentPropertiesTf;

    private Checkbox targetResidingContextOnlyCb;
    private Checkbox isRepeatingJobCb;

    private Button skipButton;
    private Button enableButton;
    private Button holdButton;
    private Button releaseButton;
    private Button submitButton;
    private Button resetButton;
    private Button submitDownstreamJobsButton;
    private Button killButton;

    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private InternalEventDrivenJobInstance internalEventDrivenJobInstance;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    private Binder<InternalEventDrivenJobInstance> formBinder;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private SchedulerStatusDiv statusDiv;

    private IkasanAuthentication authentication;

    private ContextInstance contextInstance;

    private ModuleMetaDataService moduleMetaDataService;

    private JobInitiationService jobInitiationService;

    private JobUtilsService jobUtilsService;

    private SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog;

    private Button viewErrorLogButton;
    private Button viewOutputLogButton;
    private Button executionHistoryButton;
    private Button viewProcessEventButton;
    private Button viewExecutionDetailsButton;
    private Icon acknowledgedIcon;
    private Button acknowledgedButton;
    private ScheduledProcessEvent scheduledProcessEvent;
    private LogStreamingService logStreamingService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private UI ui;

    /**
     * Constructor
     *
     * @param agent
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     * @param contextInstance
     * @param jobInitiationService
     * @param moduleMetaDataService
     * @param logStreamingService
     */
    public InternalEventDrivenJobInstanceDialog(ModuleMetaData agent,
                                                ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                                SchedulerJobInstanceService schedulerJobInstanceService, ContextInstance contextInstance,
                                                JobInitiationService jobInitiationService, ModuleMetaDataService moduleMetaDataService,
                                                LogStreamingService logStreamingService, JobUtilsService jobUtilsService,
                                                ScheduledContextInstanceService scheduledContextInstanceService) {
        super.showResize(false);
        super.title.setText(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));

        this.agent = agent;
        if(this.agent ==  null) {
            throw new IllegalArgumentException("agent cannot be null!");
        }

        this.configurationRestService = configurationRestService;
        if(this.configurationRestService ==  null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService ==  null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.metaDataRestService = metaDataRestService;
        if(this.metaDataRestService ==  null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger ==  null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if(this.contextInstance ==  null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if(this.jobInitiationService ==  null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService ==  null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if(this.logStreamingService ==  null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if(this.jobUtilsService ==  null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService ==  null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }

        this.internalEventDrivenJobInstance = new InternalEventDrivenJobInstanceImpl();

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void init() {
        this.formBinder
            = new Binder<>(InternalEventDrivenJobInstance.class);

        this.setHeight("95vh");
        this.setWidth("95vw");


        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.add(this.createJobForm(), this.createEditorLayout());
        layout.getStyle().set("padding-top", "0px");
        layout.getStyle().set("padding-bottom", "10px");
        super.content.getStyle().set("padding-top", "0px");
        super.content.add(layout);
    }

    private VerticalLayout createEditorLayout() {
        VerticalLayout editorLayout = new VerticalLayout();
        editorLayout.setPadding(false);
        editorLayout.setSpacing(false);
        editorLayout.setSizeFull();

        this.commandLineTa = new AceEditor();
        this.commandLineTa.setSizeFull();
        this.commandLineTa.setMode(AceMode.batchfile);
        this.commandLineTa.setTheme(AceTheme.dracula);
        this.commandLineTa.setId("commandLineTa");
        this.commandLineTa.setEnabled(false);
        this.commandLineTa.setReadOnly(true);

        editorLayout.add(commandLineTa);
        editorLayout.expand(this.commandLineTa);

        commandLineTa.getStyle().set("minHeight", "100px");

        return editorLayout;
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createJobForm() {
        formLayout = new FormLayout();
        formLayout.getStyle().set("padding-top", "0px");
        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setMargin(false);
        statusLayout.setWidth("100%");

        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        if(this.internalEventDrivenJobInstance.isKilled()) {
            this.statusDiv.setStatus(InstanceStatus.KILLED);
        }
        else {
            this.statusDiv.setStatus(this.internalEventDrivenJobInstance.getStatus().name(), this.internalEventDrivenJobInstance.isErrorAcknowledged());
        }

        this.acknowledgedIcon = VaadinIcon.THUMBS_UP.create();
        this.acknowledgedIcon.getStyle().set("cursor", "pointer");
        this.acknowledgedIcon.getElement().setAttribute("title", getTranslation("tooltip.view-acknowledgement-details"));
        this.acknowledgedIcon.addClickListener(event -> {
            SchedulerJobInstanceRecord dbRecord = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
            ErrorAcknowledgedPositionedDialog errorAcknowledgedPositionedDialog = new ErrorAcknowledgedPositionedDialog(dbRecord,
                this.contextInstance, this.scheduledContextInstanceService, this.moduleMetaDataService, this.logStreamingService,
                this.schedulerJobInstanceService);
            PositionedDialog.Position position = new PositionedDialog.Position(event.getScreenY(), event.getScreenX());
            errorAcknowledgedPositionedDialog.setPosition(position);
            errorAcknowledgedPositionedDialog.open();
        });

        this.acknowledgedIcon.setVisible(false);
        statusLayout.add(this.statusDiv, this.acknowledgedIcon);
        statusLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.acknowledgedIcon);

        formLayout.add(statusLayout, 2);

        this.holdButton = new Button(getTranslation("button.hold", UI.getCurrent().getLocale()), new Icon(VaadinIcon.HAND));
        this.holdButton.setIconAfterText(true);

        this.holdButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.holdJob()) {
                    this.statusDiv.setStatus(InstanceStatus.ON_HOLD);
                    this.internalEventDrivenJobInstance.setStatus(InstanceStatus.ON_HOLD);
                    this.setButtonVisibility();
                }
            });
        });

        this.releaseButton = new Button(getTranslation("button.release", UI.getCurrent().getLocale()), new Icon(VaadinIcon.HANDS_UP));
        this.releaseButton.setIconAfterText(true);

        this.releaseButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.releaseJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    this.internalEventDrivenJobInstance.setStatus(InstanceStatus.WAITING);
                    this.setButtonVisibility();
                }
            });
        });

        this.skipButton = new Button(getTranslation("button.skip", UI.getCurrent().getLocale()), new Icon(VaadinIcon.BAN));
        this.skipButton.setIconAfterText(true);

        this.skipButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.skipJob()) {
                    this.statusDiv.setStatus(InstanceStatus.SKIPPED);
                    this.internalEventDrivenJobInstance.setStatus(InstanceStatus.SKIPPED);
                    this.setButtonVisibility();
                }
            });
        });

        this.enableButton = new Button(getTranslation("button.enable", UI.getCurrent().getLocale()), new Icon(VaadinIcon.PLAY));
        this.enableButton.setIconAfterText(true);

        this.enableButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.enableJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    this.internalEventDrivenJobInstance.setStatus(InstanceStatus.WAITING);
                    this.setButtonVisibility();
                }
            });
        });

        this.submitButton = new Button(getTranslation("button.submit", UI.getCurrent().getLocale()), new Icon(VaadinIcon.PAPERPLANE));
        this.submitButton.setIconAfterText(true);

        this.submitButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            InternalEventDrivenJobSubmissionDialog internalEventDrivenJobSubmissionDialog = new InternalEventDrivenJobSubmissionDialog(this.systemEventLogger,
                this.moduleMetaDataService, this.contextInstance, this.jobInitiationService, schedulerJobInstanceRecord, this.schedulerJobInstanceService);

            internalEventDrivenJobSubmissionDialog.open();
        });

        this.submitDownstreamJobsButton = new Button(getTranslation("button.initiate-downstream-jobs", UI.getCurrent().getLocale()), VaadinIcon.FAST_FORWARD.create());
        this.submitDownstreamJobsButton.setIconAfterText(true);
        this.submitDownstreamJobsButton.setVisible(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ERROR));
        this.submitDownstreamJobsButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
            ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
            contextualisedScheduledProcessEvent.setJobStarting(false);
            contextualisedScheduledProcessEvent.setJobName(this.internalEventDrivenJobInstance.getJobName());
            contextualisedScheduledProcessEvent.setAgentName(this.internalEventDrivenJobInstance.getAgentName());
            contextualisedScheduledProcessEvent.setContextName(this.internalEventDrivenJobInstance.getContextName());
            contextualisedScheduledProcessEvent.setInternalEventDrivenJob(this.internalEventDrivenJobInstance);
            contextualisedScheduledProcessEvent.setRaisedDueToFailureResubmission(true);

            List<SchedulerJobInitiationEvent> initiationEvents = contextMachine
                .getEventsThatCanRun(contextualisedScheduledProcessEvent);

            if(initiationEvents.isEmpty()) {
                NotificationHelper.showUserNotification(getTranslation("notification.no-downstream-jobs-to-initiate"
                    , UI.getCurrent().getLocale()));
                return;
            }

            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setCancelable(true);
            confirmDialog.setHeader(getTranslation("confirm-dialog.downstream-job-initiation-header"
                , UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            VerticalLayout verticalLayout = new VerticalLayout();
            initiationEvents.forEach(initiationEvent -> {
                Div jobName = new Div();
                jobName.setText(initiationEvent.getJobName());
                verticalLayout.add(jobName);
            });
            confirmDialog.setText(verticalLayout);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    List<String> childContextNames = ContextHelper.getContextsWhereJobFilterMatchResides
                        (this.contextInstance, contextualisedScheduledProcessEvent.getJobName());

                    for(String name: childContextNames) {
                        contextualisedScheduledProcessEvent.getInternalEventDrivenJob().setChildContextName(name);
                        contextMachine.raiseEvent(contextualisedScheduledProcessEvent);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.downstream-job-initiation"
                        , UI.getCurrent().getLocale()));
                }

                NotificationHelper.showUserNotification(getTranslation("notification.downstream-job-initiation", UI.getCurrent().getLocale()));
            });
        });

        this.resetButton = new Button("Reset", new Icon(VaadinIcon.ARROW_BACKWARD));
        this.resetButton.setIconAfterText(true);
        this.resetButton.getElement().setAttribute("title", "Reset Job");
        this.resetButton.addClickListener(event -> {
            if(!canPerformAction()) {
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
                if(this.resetJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    this.internalEventDrivenJobInstance.setStatus(InstanceStatus.WAITING);
                    setButtonVisibility();
                }
            });
        });

        this.killButton = new Button(getTranslation("button.kill-job"
            , UI.getCurrent().getLocale()), new Icon(VaadinIcon.CLOSE_BIG));

        this.killButton.setIconAfterText(true);

        this.killButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }

            try {
                this.internalEventDrivenJobInstance.setKilled(true);
                this.updateScheduledJob(this.internalEventDrivenJobInstance, this.authentication);
                this.internalEventDrivenJobInstance.getChildContextNames().forEach(name -> {
                    SchedulerJobInstanceRecord record = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.internalEventDrivenJobInstance.getContextInstanceId(),
                        this.internalEventDrivenJobInstance.getJobName(), name);

                    if(record != null) {
                        SchedulerJobInstance instance = record.getSchedulerJobInstance();
                        if(instance instanceof InternalEventDrivenJobInstance) {
                            ((InternalEventDrivenJobInstance) instance).setKilled(true);
                            record.setSchedulerJobInstance(instance);
                            updateScheduledJob(record, authentication);
                        }
                    }
                });
                this.jobUtilsService.killJob(agent.getUrl(), scheduledProcessEvent.getPid(), true);

                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_KILLED, String.format("Agent Name[%s], Scheduled Job Name[%s],Job Plan Name[%s], Job Plan Id[%s]"
                    , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), internalEventDrivenJobInstance.getContextName()
                    , internalEventDrivenJobInstance.getContextInstanceId()), this.authentication.getName());

                NotificationHelper.showUserNotification(getTranslation("notification.job-killed", UI.getCurrent().getLocale()));
            }
            catch (Exception e) {
                e.printStackTrace();
                this.internalEventDrivenJobInstance.setKilled(false);
                this.updateScheduledJob(this.internalEventDrivenJobInstance, this.authentication);
                NotificationHelper.showErrorNotification(getTranslation("error.kill-job", UI.getCurrent().getLocale()));
            }
        });

        this.acknowledgedButton = new Button("Acknowledge Error", VaadinIcon.THUMBS_UP.create());
        this.acknowledgedButton.setVisible(false);

        acknowledgedButton.addClickListener(event -> {
            AcknowledgeErrorDialog errorDialog = new AcknowledgeErrorDialog(this.contextInstance, this.schedulerJobInstanceService,
                schedulerJobInstanceRecord, this.systemEventLogger);
            errorDialog.open();
        });

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.add(this.holdButton, this.releaseButton, this.skipButton, this.enableButton, this.submitButton, this.acknowledgedButton
            , this.killButton, this.resetButton, this.submitDownstreamJobsButton);
        actionsLayout.setMargin(false);

        VerticalLayout actionsButtonLayout = new VerticalLayout();
        actionsButtonLayout.setWidth("100%");
        actionsButtonLayout.add(actionsLayout);
        actionsButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionsLayout);
        actionsButtonLayout.setMargin(false);

        formLayout.add(actionsButtonLayout, 2);

        H3 jobExecutionLabel = new H3(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));

        this.targetResidingContextOnlyCb = new Checkbox(getTranslation("label.target-residing-context-only", UI.getCurrent().getLocale()));
        formBinder.forField(this.targetResidingContextOnlyCb)
            .bind(InternalEventDrivenJob::isTargetResidingContextOnly, InternalEventDrivenJob::setTargetResidingContextOnly);
        this.targetResidingContextOnlyCb.setEnabled(false);

        this.isRepeatingJobCb = new Checkbox(getTranslation("label.is-repeating-job", UI.getCurrent().getLocale()));
        formBinder.forField(this.isRepeatingJobCb)
            .bind(InternalEventDrivenJob::isJobRepeatable, InternalEventDrivenJob::setJobRepeatable);
        this.isRepeatingJobCb.setEnabled(false);


        HorizontalLayout checkboxLayout = new HorizontalLayout(this.targetResidingContextOnlyCb, this.isRepeatingJobCb);

        formLayout.add(jobExecutionLabel, checkboxLayout);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(false);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobName, InternalEventDrivenJobInstance::setJobName);
        formLayout.add(jobNameTf);

        this.agentTf = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        if(agent != null) {
            this.agentTf.setValue(agent.getName());
            this.agentTf.setEnabled(false);
        }
        formBinder.forField(this.agentTf)
            .withValidator(agentValue -> agentValue != null && !agentValue.isEmpty(), getTranslation("error.missing-agent", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getAgentName, InternalEventDrivenJobInstance::setAgentName);
        formLayout.add(agentTf);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        this.jobDescriptionTa.setEnabled(false);
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobDescription, InternalEventDrivenJobInstance::setJobDescription);

        if(this.contextInstance.isUseDisplayName()) {
            this.jobNameAliasTf = new TextField(getTranslation("label.job-name-alias", UI.getCurrent().getLocale()));
            this.jobNameAliasTf.setId("jobNameAliasTf");
            this.jobNameAliasTf.setRequired(false);
            this.jobNameAliasTf.setEnabled(false);
            formBinder.forField(this.jobNameAliasTf)
                .bind(InternalEventDrivenJob::getDisplayName, InternalEventDrivenJob::setDisplayName);
            formLayout.add(jobNameAliasTf, jobDescriptionTa);
        }
        else {
            formLayout.add(jobDescriptionTa, 2);
        }


        this.minExecutionTimeTf = new TextField(getTranslation("label.minimum-execution-time", UI.getCurrent().getLocale()));
        this.minExecutionTimeTf.setEnabled(false);
        formBinder.forField(this.minExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMinExecutionTime, InternalEventDrivenJobInstance::setMinExecutionTime);

        this.maxExecutionTimeTf = new TextField(getTranslation("label.maximum-execution-time", UI.getCurrent().getLocale()));
        this.maxExecutionTimeTf.setEnabled(false);
        formBinder.forField(this.maxExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMaxExecutionTime, InternalEventDrivenJobInstance::setMaxExecutionTime);

        formLayout.add(minExecutionTimeTf, maxExecutionTimeTf);

        this.workingDirectoryTf = new TextField(getTranslation("label.working-directory", UI.getCurrent().getLocale()));
        this.workingDirectoryTf.setEnabled(false);
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJobInstance::getWorkingDirectory, InternalEventDrivenJobInstance::setWorkingDirectory);

        this.executionEnvironmentPropertiesTf = new TextField(getTranslation("label.execution-environment-properties", UI.getCurrent().getLocale()));
        this.executionEnvironmentPropertiesTf.setEnabled(false);
        formBinder.forField(this.executionEnvironmentPropertiesTf)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJobInstance::getExecutionEnvironmentProperties, InternalEventDrivenJobInstance::setExecutionEnvironmentProperties);

        formLayout.add(workingDirectoryTf, executionEnvironmentPropertiesTf);

        Button executionDaysButton = new Button(getTranslation("button.execution-days", UI.getCurrent().getLocale()), new Icon(VaadinIcon.CALENDAR));
        executionDaysButton.setIconAfterText(true);
        executionDaysButton.addClickListener(event -> {
            DayOfWeekJobDialog dayOfWeekJobDialog = new DayOfWeekJobDialog(this.internalEventDrivenJobInstance.getDaysOfWeekToRun() == null
                ? null : new ArrayList<>(this.internalEventDrivenJobInstance.getDaysOfWeekToRun()), false);
            dayOfWeekJobDialog.open();

            dayOfWeekJobDialog.addOpenedChangeListener(openedChangeEvent -> {
               if(!openedChangeEvent.isOpened() && dayOfWeekJobDialog.isSaveClose()) {
                   this.internalEventDrivenJobInstance.setDaysOfWeekToRun(dayOfWeekJobDialog.getDaysOfWeek());
               }
            });
        });

        Button parametersButton = new Button(getTranslation("button.parameters", UI.getCurrent().getLocale()), new Icon(VaadinIcon.SLIDERS));
        parametersButton.setIconAfterText(true);
        parametersButton.addClickListener(event -> {
            ContextParameterDialog contextParameterDialog = new ContextParameterDialog(false,
                this.internalEventDrivenJobInstance.isTemplateBased() != null && this.internalEventDrivenJobInstance.isTemplateBased() == false);
            contextParameterDialog.initParams(this.internalEventDrivenJobInstance.getContextParameters() == null ? new ArrayList<>() : this.internalEventDrivenJobInstance.getContextParameters());
            contextParameterDialog.open();

            contextParameterDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && contextParameterDialog.isSaveClose()) {
                    this.internalEventDrivenJobInstance.setContextParameters(contextParameterDialog.getContextParameters());
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(parametersButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        Button successfulReturnCodesButton = new Button(getTranslation("button.return-codes", UI.getCurrent().getLocale()), new Icon(VaadinIcon.CHECK));
        successfulReturnCodesButton.setIconAfterText(true);
        successfulReturnCodesButton.addClickListener(event -> {
            SuccessfulReturnCodesDialog successfulReturnCodesDialog = new SuccessfulReturnCodesDialog(false);
            successfulReturnCodesDialog.initReturnCodes(this.internalEventDrivenJobInstance.getSuccessfulReturnCodes());
            successfulReturnCodesDialog.open();

            successfulReturnCodesDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && successfulReturnCodesDialog.isSaveClose()) {
                    this.internalEventDrivenJobInstance.setSuccessfulReturnCodes(successfulReturnCodesDialog.getSuccessfulReturnCodes());
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(successfulReturnCodesButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        this.viewOutputLogButton = new Button(getTranslation("button.view-output-log", UI.getCurrent().getLocale()), VaadinIcon.FILE_PROCESS.create());
        this.viewOutputLogButton.setVisible(this.scheduledProcessEvent != null &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
        this.viewOutputLogButton.setIconAfterText(true);
        this.viewOutputLogButton.addClickListener(event -> {
           this.streamLog(false);
        });

        this.viewErrorLogButton = new Button(getTranslation("button.view-error-log", UI.getCurrent().getLocale()), VaadinIcon.FILE_REMOVE.create());
        this.viewErrorLogButton.setVisible(this.scheduledProcessEvent != null &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
        this.viewErrorLogButton.setIconAfterText(true);
        this.viewErrorLogButton.addClickListener(event -> {
            this.streamLog(true);
        });

        this.executionHistoryButton = new Button(getTranslation("label.log-file-history"
            , UI.getCurrent().getLocale()), VaadinIcon.CLIPBOARD_HEART.create());
        this.executionHistoryButton.addClickListener(event -> {
            LogFileHistoryDialog logFileHistoryDialog = new LogFileHistoryDialog(this.scheduledContextInstanceService
                , this.contextInstance, schedulerJobInstanceRecord.getSchedulerJobInstance()
                , this.moduleMetaDataService, this.logStreamingService);
            logFileHistoryDialog.open();
        });

        this.viewProcessEventButton = new Button(getTranslation("button.view-event", UI.getCurrent().getLocale()), VaadinIcon.CALENDAR_CLOCK.create());
        this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
        this.viewProcessEventButton.setIconAfterText(true);
        this.viewProcessEventButton.addClickListener(event -> {
            JsonViewerDialog dialog = new JsonViewerDialog(this.scheduledProcessEvent
                , getTranslation("header.scheduled-process-event", UI.getCurrent().getLocale()));
            dialog.open();
        });

        this.viewExecutionDetailsButton = new Button(getTranslation("button.process-execution-details", UI.getCurrent().getLocale()), VaadinIcon.COG.create());
        this.viewExecutionDetailsButton.setVisible(this.scheduledProcessEvent != null && this.scheduledProcessEvent.getExecutionDetails() != null &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
        this.viewExecutionDetailsButton.setIconAfterText(true);
        this.viewExecutionDetailsButton.addClickListener(event -> {
            TextViewerDialog dialog = new TextViewerDialog(this.scheduledProcessEvent.getExecutionDetails()
                .substring(1,this.scheduledProcessEvent.getExecutionDetails().length()-1)
                , getTranslation("header.process-execution-details", UI.getCurrent().getLocale()));
            dialog.open();
        });

        Button downloadButton = new Button(getTranslation("button.download", UI.getCurrent().getLocale()), new Icon(VaadinIcon.DOWNLOAD_ALT));
        downloadButton.setIconAfterText(true);

        Anchor downloadAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
            -> new DownloadResponse(new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.internalEventDrivenJobInstance))
            , this.internalEventDrivenJobInstance.getJobName()+".json",
            "application/json", -1)), "");

        downloadAnchor.add(downloadButton);

        ComponentSecurityVisibility.applySecurity(downloadAnchor, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        HorizontalLayout jobActionsButtonLayout = new HorizontalLayout();
        jobActionsButtonLayout.add(executionDaysButton, parametersButton, successfulReturnCodesButton
            , this.viewOutputLogButton, this.viewErrorLogButton, this.executionHistoryButton, this.viewProcessEventButton, viewExecutionDetailsButton);
        jobActionsButtonLayout.setMargin(false);
        VerticalLayout wrapperLayout = new VerticalLayout();
        wrapperLayout.setWidth("100%");
        wrapperLayout.add(jobActionsButtonLayout);
        wrapperLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, jobActionsButtonLayout);
        wrapperLayout.setMargin(false);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(downloadAnchor);

        VerticalLayout newButtonLayout = new VerticalLayout();
        newButtonLayout.setWidth("100%");
        newButtonLayout.add(horizontalLayout);
        newButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, horizontalLayout);
        newButtonLayout.setMargin(false);

        formLayout.add(wrapperLayout, newButtonLayout);

        this.setButtonVisibility();

        return formLayout;
    }

    /**
     * Stream the job file.
     *
     * @param getErrorLog
     */
    private void streamLog(boolean getErrorLog) {
        boolean displayLog = false;
        String host = null;
        String endPoint = null;
        String outputLog = null;

        if (this.scheduledProcessEvent != null && agent != null) {
            host = agent.getUrl();
            endPoint = "/rest/logs";
            outputLog = getErrorLog ? this.scheduledProcessEvent.getResultError() : this.scheduledProcessEvent.getResultOutput();
            logger.info(String.format("Streaming log for host %s, endPoint %s, log %s", host, endPoint, outputLog));
            if (outputLog != null && host != null) {
                displayLog = true;
            }
        }

        if (displayLog) {
            schedulerJobLogFileViewerDialog = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
            schedulerJobLogFileViewerDialog.open();
        } else {
            String message = "There is no " + (getErrorLog ? "error" : "output") + " log for the job";
            NotificationHelper.showUserNotification(message);
        }
    }

    /**
     * Helper method to skip the job.
     *
     * @return
     */
    private boolean skipJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-skipped"
                , UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName(), true);
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.SKIPPED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s], Job Plan Name[%s], Job Plan Id[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), true, internalEventDrivenJobInstance.getContextName()
                , internalEventDrivenJobInstance.getContextInstanceId()), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.skipped-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to enable the job.
     *
     * @return
     */
    private boolean enableJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-enabled", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName(), false);
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_ENABLED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s], Job Plan Name[%s], Job Plan Id[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), false, internalEventDrivenJobInstance.getContextName()
                , internalEventDrivenJobInstance.getContextInstanceId()), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.enabled-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to hold the job.
     *
     * @return
     */
    private boolean holdJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-held", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.holdJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName());
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.ON_HOLD);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, String.format("Agent Name[%s], Scheduled Job Name[%s], Held[%s], Job Plan Name[%s], Job Plan Id[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), true, internalEventDrivenJobInstance.getContextName()
                , internalEventDrivenJobInstance.getContextInstanceId()), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.held-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to release the job.
     *
     * @return
     */
    private boolean releaseJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-released", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.releaseJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName());
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RELEASED, String.format("Agent Name[%s], Scheduled Job Name[%s], Released[%s], Job Plan Name[%s], Job Plan Id[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), true, internalEventDrivenJobInstance.getContextName()
                , internalEventDrivenJobInstance.getContextInstanceId()), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.released-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    private boolean resetJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.job-reset-error-no-active-context", UI.getCurrent().getLocale()));
            return false;
        }

        AtomicReference<Boolean> result = new AtomicReference<>(true);

        ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
        dialog.open(getTranslation("progress-dialog.reset-job-header", UI.getCurrent().getLocale()),
            getTranslation("progress-dialog.reset-job-body", UI.getCurrent().getLocale()));

        final UI current = UI.getCurrent();
        Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("AbstractGridSchedulerJobInstanceActionWidget"));
        executor.execute(() -> {
            boolean error = false;
            try {
                if(this.internalEventDrivenJobInstance.isTargetResidingContextOnly()) {
                    contextMachine.resetJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName());
                }
                else {
                    this.internalEventDrivenJobInstance.getChildContextNames().forEach(name
                        -> contextMachine.resetJob(this.internalEventDrivenJobInstance.getIdentifier(), name));
                }

                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RESET, String.format("Agent Name[%s], Scheduled Job Name[%s], Reset[%s], Job Plan Name[%s], Job Plan Id[%s]"
                    , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), true, internalEventDrivenJobInstance.getContextName()
                    , internalEventDrivenJobInstance.getContextInstanceId()), this.authentication.getName());
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error(String.format("An error has occurred resetting job[%s], context name[%s], context instance id[%s]"
                    , internalEventDrivenJobInstance.getJobName(), internalEventDrivenJobInstance.getContextName()
                    , this.schedulerJobInstanceRecord.getContextInstanceId()), e);
                error = true;
            }
            finally {
                boolean finalError = error;
                current.access(() -> {
                    dialog.close();

                    if (finalError) {
                        NotificationHelper.showErrorNotification(getTranslation("error.reset-general-error", UI.getCurrent().getLocale()));
                        result.set(false);
                    }
                });
            }
        });

        return result.get();
    }
    

    /**
     * Update and persist the associated job.
     *
     * @param internalEventDrivenJobInstance
     */
    public void updateScheduledJob(InternalEventDrivenJobInstance internalEventDrivenJobInstance, IkasanAuthentication authentication) {

        this.schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        this.schedulerJobInstanceRecord.setSchedulerJobInstance(internalEventDrivenJobInstance);
        this.schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        this.schedulerJobInstanceRecord.setStatus(internalEventDrivenJobInstance.getStatus().name());

        this.schedulerJobInstanceService.save(this.schedulerJobInstanceRecord);
     }


    /**
     * Update and persist the given scheduler job instance record.
     *
     * @param schedulerJobInstanceRecord The scheduler job instance record to update
     * @param authentication The authentication information of the user performing the update
     */
    public void updateScheduledJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord, IkasanAuthentication authentication) {

        schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        schedulerJobInstanceRecord.setModifiedBy(authentication.getName());

        schedulerJobInstanceService.save(schedulerJobInstanceRecord);
    }

    /**
     * Helper method to confirm that actions can be performed on a job plan
     * @return
     */
    private boolean canPerformAction() {
        if(!ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
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

        return true;
    }

    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param internalEventDrivenJob
     */
    private void setJob(InternalEventDrivenJobInstance internalEventDrivenJob) {
        this.internalEventDrivenJobInstance = internalEventDrivenJob;
        this.scheduledProcessEvent = this.internalEventDrivenJobInstance.getScheduledProcessEvent();

        this.init();

        this.formBinder.readBean(this.internalEventDrivenJobInstance);
        this.commandLineTa.setValue(internalEventDrivenJob.getCommandLine());

        // make sure all value are bound before calling set enabled
        this.setEnabled(false);
    }

    /**
     * Helper method to update a jobs state and persist it before broadcasting the state change.
     *
     * @param internalEventDrivenJobInstance
     * @param newStatus
     */
    private void updateJobState(InternalEventDrivenJobInstance internalEventDrivenJobInstance, InstanceStatus newStatus) {
        InstanceStatus previousStatus = internalEventDrivenJobInstance.getStatus();
        internalEventDrivenJobInstance.setStatus(newStatus);
        this.updateScheduledJob(internalEventDrivenJobInstance, this.authentication);

        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(internalEventDrivenJobInstance,
            this.contextInstance, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(schedulerJobInstanceStateChangeEvent);
    }

    /**
     * Set the job record on this object.
     *
     * @param internalEventDrivenJobRecord
     */
    public void setJob(SchedulerJobInstanceRecord internalEventDrivenJobRecord) {
        this.schedulerJobInstanceRecord = internalEventDrivenJobRecord;
        this.setJob((InternalEventDrivenJobInstance)this.schedulerJobInstanceRecord.getSchedulerJobInstance());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();
        SchedulerJobStateChangeEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        SchedulerJobStateChangeEventBroadcaster.instance().unregister(this);
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent jobInstanceStateChangeEvent) {
        if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId().equals(this.internalEventDrivenJobInstance.getContextInstanceId())
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName().equals(this.internalEventDrivenJobInstance.getChildContextName())
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName().equals(this.internalEventDrivenJobInstance.getJobName())) {
            if(this.ui.isAttached()) {
                this.ui.access(() -> {
                    if(jobInstanceStateChangeEvent.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                        this.internalEventDrivenJobInstance = (InternalEventDrivenJobInstance) jobInstanceStateChangeEvent.getSchedulerJobInstance();
                    }
                    else {
                        this.internalEventDrivenJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    }

                    if(this.internalEventDrivenJobInstance.isKilled() && jobInstanceStateChangeEvent.getNewStatus().equals(InstanceStatus.ERROR)) {
                        this.statusDiv.setStatus(InstanceStatus.KILLED);
                    }
                    else {
                        this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus().name(), this.internalEventDrivenJobInstance.isErrorAcknowledged());
                    }

                    this.scheduledProcessEvent = jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent();

                    this.viewOutputLogButton.setVisible(this.scheduledProcessEvent != null &&
                        ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
                    this.viewErrorLogButton.setVisible(this.scheduledProcessEvent != null &&
                        ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
                    this.executionHistoryButton.setVisible(this.scheduledProcessEvent != null &&
                        ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
                    this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null &&
                        ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));
                    this.viewExecutionDetailsButton.setVisible(this.scheduledProcessEvent != null &&
                        ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

                    this.setButtonVisibility();
                });
            }
        }
    }

    private void setButtonVisibility() {
        if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.RUNNING)) {
            this.killButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.submitButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.resetButton.setVisible(false);
            this.submitDownstreamJobsButton.setVisible(false);
            this.acknowledgedButton.setVisible(false);
            this.acknowledgedIcon.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.LOCK_QUEUED)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.resetButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.submitDownstreamJobsButton.setVisible(false);
            this.acknowledgedButton.setVisible(false);
            this.acknowledgedIcon.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ERROR)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            if(internalEventDrivenJobInstance.isErrorAcknowledged() == null
                || !internalEventDrivenJobInstance.isErrorAcknowledged()) {
                this.acknowledgedButton.setVisible(true);
            }
            else {
                this.acknowledgedButton.setVisible(false);
            }

            if(this.internalEventDrivenJobInstance.isErrorAcknowledged() != null &&
                this.internalEventDrivenJobInstance.isErrorAcknowledged()) {
                this.acknowledgedIcon.setVisible(true);
            }

            this.resetButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.submitDownstreamJobsButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.WAITING)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.holdButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.enableButton.setVisible(false);
            this.resetButton.setVisible(false);
            this.submitDownstreamJobsButton.setVisible(false);
            this.acknowledgedButton.setVisible(false);
            this.acknowledgedIcon.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.resetButton.setVisible(false);
            this.submitDownstreamJobsButton.setVisible(false);
            this.acknowledgedButton.setVisible(false);
            this.acknowledgedIcon.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.SKIPPED) ||
            this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
            this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.resetButton.setVisible(false);
            this.submitDownstreamJobsButton.setVisible(false);
            this.acknowledgedButton.setVisible(false);
            this.acknowledgedIcon.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.COMPLETE)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.resetButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.submitDownstreamJobsButton.setVisible(false);
            this.acknowledgedButton.setVisible(false);
            this.acknowledgedIcon.setVisible(false);
        }
    }
}
