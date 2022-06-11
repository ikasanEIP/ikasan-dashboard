package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrInternalEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public class InternalEventDrivenJobInstanceDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(InternalEventDrivenJobInstanceDialog.class);

    private Registration schedulerJobStateChangeRegistration;

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private ComboBox<String> agentCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;


    // Fields to capture job execution properties.
    private AceEditor commandLineTa;
    private TextField workingDirectoryTf;
    private TextField minExecutionTimeTf;
    private TextField maxExecutionTimeTf;

    private boolean skipped = false;
    private Button skipButton;
    private Button enableButton;
    private Button holdButton;
    private Button releaseButton;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private InternalEventDrivenJobInstance internalEventDrivenJobInstance;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    private Binder<InternalEventDrivenJobInstance> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private SchedulerStatusDiv statusDiv;

    private IkasanAuthentication authentication;


    /**
     * Constructor
     *
     * @param agent
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     */
    public InternalEventDrivenJobInstanceDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                                ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                                SchedulerJobInstanceService schedulerJobInstanceService) {
        super.showResize(false);
        super.title.setText(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobInstanceService = schedulerJobInstanceService;

        this.internalEventDrivenJobInstance = new SolrInternalEventDrivenJobInstanceImpl();

        authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void init() {
        this.formBinder
            = new Binder<>(InternalEventDrivenJobInstance.class);

        this.setHeight("1100px");
        this.setWidth("1400px");


        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.add(this.createConfigurationForm());
        layout.getStyle().set("padding-top", "0px");
        layout.getStyle().set("padding-bottom", "10px");
        super.content.getStyle().set("padding-top", "0px");
        super.content.add(layout);
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();
        formLayout.getStyle().set("padding-top", "0px");
        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.setStatus(this.schedulerJobInstanceRecord.getStatus());

        formLayout.add(this.statusDiv, 2);

        Icon holdIcon = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        this.holdButton = new Button("Hold", holdIcon);
        this.holdButton.setIconAfterText(true);
        this.holdButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Hold Job");
            confirmDialog.setText("Are you sure that you would like to hold this job?");

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                this.statusDiv.setStatus(InstanceStatus.ON_HOLD);
                this.releaseButton.setVisible(true);
                this.holdButton.setVisible(false);
                this.skipButton.setVisible(false);

                this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.ON_HOLD);
            });
        });

        Icon releaseIcon = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        this.releaseButton = new Button("Release", releaseIcon);
        this.releaseButton.setIconAfterText(true);
        this.releaseButton.setVisible(false);
        this.releaseButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Release Job");
            confirmDialog.setText("Are you sure that you would like to release this job?");

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                this.statusDiv.setStatus(InstanceStatus.RELEASED);
                this.releaseButton.setVisible(false);
                this.holdButton.setVisible(true);
                this.skipButton.setVisible(true);

                this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.RELEASED);
            });
        });

        Icon skipIcon = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        this.skipButton = new Button("Skip", skipIcon);
        this.skipButton.setIconAfterText(true);
        this.skipButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Skipping Job");
            confirmDialog.setText("Are you sure that you would like to skip this job?");

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                this.statusDiv.setStatus(InstanceStatus.SKIPPED);
                this.releaseButton.setVisible(false);
                this.holdButton.setVisible(false);
                this.skipped = true;

                this.enableButton.setVisible(true);
                this.skipButton.setVisible(false);

                this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.SKIPPED);
            });
        });

        Icon enableIcon = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        this.enableButton = new Button("Enable", enableIcon);
        this.enableButton.setIconAfterText(true);
        this.enableButton.setVisible(false);
        this.enableButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Enable Job");
            confirmDialog.setText("Are you sure that you would like to enable this job?");

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                this.statusDiv.setStatus(this.schedulerJobInstanceRecord.getStatus());
                this.releaseButton.setVisible(false);
                this.holdButton.setVisible(true);
                this.enableButton.setVisible(false);
                this.skipButton.setVisible(true);
                this.skipped = false;

                this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.WAITING);
            });
        });

        Icon submitIcon = IconDecorator.decorate(new Icon(VaadinIcon.PAPERPLANE), getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        Button submitButton = new Button("Submit", submitIcon);
        submitButton.setIconAfterText(true);

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.add(holdButton, releaseButton, skipButton, enableButton, submitButton);
        actionsLayout.setMargin(false);

        VerticalLayout actionsButtonLayout = new VerticalLayout();
        actionsButtonLayout.setWidth("100%");
        actionsButtonLayout.add(actionsLayout);
        actionsButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionsLayout);
        actionsButtonLayout.setMargin(false);

        H3 jobExecutionLabel = new H3(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));
        formLayout.add(jobExecutionLabel, actionsButtonLayout);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobName, InternalEventDrivenJobInstance::setJobName);
        formLayout.add(jobNameTf);

        this.agentCb = new ComboBox<>(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentCb.setId("agentCb");
        this.agentCb.setRequired(true);
        this.agentCb.setClearButtonVisible(true);
        this.agentCb.setItems(this.scheduledProcessManagementService.getAllAgentNames());
        if(agent != null) {
            this.agentCb.setValue(agent.getName());
            this.agentCb.setEnabled(false);
        }
        formBinder.forField(this.agentCb)
            .withValidator(agentValue -> agentValue != null && !agentValue.isEmpty(), getTranslation("error.missing-agent", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getAgentName, InternalEventDrivenJobInstance::setAgentName);
        formLayout.add(agentCb);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobDescription, InternalEventDrivenJobInstance::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.minExecutionTimeTf = new TextField("Minimum execution time");
        formBinder.forField(this.minExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMinExecutionTime, InternalEventDrivenJobInstance::setMinExecutionTime);

        this.maxExecutionTimeTf = new TextField("Maximum execution time");
        formBinder.forField(this.maxExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMaxExecutionTime, InternalEventDrivenJobInstance::setMaxExecutionTime);

        formLayout.add(minExecutionTimeTf, maxExecutionTimeTf);

        this.workingDirectoryTf = new TextField(getTranslation("label.working-directory", UI.getCurrent().getLocale()));
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJobInstance::getWorkingDirectory, InternalEventDrivenJobInstance::setWorkingDirectory);
        formLayout.add(workingDirectoryTf, 2);

        Icon calendarIcon = IconDecorator.decorate(new Icon(VaadinIcon.CALENDAR), getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        Button executionDaysButton = new Button("Execution Days", calendarIcon);
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

        Icon parametersIcon = IconDecorator.decorate(new Icon(VaadinIcon.SLIDERS), getTranslation("label.job-parameters", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        Button parametersButton = new Button("Parameters", parametersIcon);
        parametersButton.setIconAfterText(true);
        parametersButton.addClickListener(event -> {
            ContextParameterDialog contextParameterDialog = new ContextParameterDialog(false);
            contextParameterDialog.initParams(this.internalEventDrivenJobInstance.getContextParameters() == null ? new ArrayList<>() : this.internalEventDrivenJobInstance.getContextParameters());
            contextParameterDialog.open();

            contextParameterDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && contextParameterDialog.isSaveClose()) {
                    this.internalEventDrivenJobInstance.setContextParameters(contextParameterDialog.getContextParameters());
                }
            });
        });

        Icon successfulReturnCodesIcon = IconDecorator.decorate(new Icon(VaadinIcon.CHECK), getTranslation("label.successful-return-codes", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        Button successfulReturnCodesButton = new Button("Return Codes", successfulReturnCodesIcon);
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

        Icon downloadIcon = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        Button downloadButton = new Button("Download", downloadIcon);
        downloadButton.setIconAfterText(true);
        StreamResource streamResource = new StreamResource(this.internalEventDrivenJobInstance.getJobName()+".json"
            , () -> {
            try {
                return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.internalEventDrivenJobInstance));
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        });

        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(streamResource);
        buttonWrapper.wrapComponent(downloadButton);

        Icon externalIcon = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("label.expand-text-editor", UI.getCurrent().getLocale()), "18pt", "rgba(241, 90, 35, 1.0)");
        Button expandButton = new Button("Expand", externalIcon);
        expandButton.setIconAfterText(true);

        HorizontalLayout jobActionsButtonLayout = new HorizontalLayout();
        jobActionsButtonLayout.add(executionDaysButton, parametersButton, successfulReturnCodesButton);
        VerticalLayout cbLayout = new VerticalLayout();
        cbLayout.setWidth("100%");
        cbLayout.add(jobActionsButtonLayout);
        cbLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, jobActionsButtonLayout);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(buttonWrapper, expandButton);

        VerticalLayout newButtonLayout = new VerticalLayout();
        newButtonLayout.setWidth("100%");
        newButtonLayout.add(horizontalLayout);
        newButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, horizontalLayout);

        formLayout.add(cbLayout, newButtonLayout);

        this.commandLineTa = new AceEditor();
        this.commandLineTa.setHeight("450px");
        this.commandLineTa.setMode(AceMode.batchfile);
        this.commandLineTa.setTheme(AceTheme.dracula);
        this.commandLineTa.setId("commandLineTa");

        formLayout.add(commandLineTa, 2);
        commandLineTa.getStyle().set("minHeight", "100px");

        return formLayout;
    }

    private boolean skipJob(boolean skipFlag) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification("This job is not part of an active context and cannot be skipped.");
            return false;
        }

        try {
            contextMachine.skipJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName(), skipFlag);
            this.updateScheduledJob(this.internalEventDrivenJobInstance, this.authentication);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), skipFlag), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification("An error has occurred attempting to skip the job. Please contact Ikasan Support.");
            return false;
        }

        return true;
    }
    

    /**
     *
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
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.agentCb.setEnabled(enabled);
        this.commandLineTa.setEnabled(enabled);
        this.commandLineTa.setReadOnly(!enabled);

        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        this.jobDescriptionTa.setEnabled(enabled);

        this.commandLineTa.setEnabled(enabled);
        this.workingDirectoryTf.setEnabled(enabled);

        this.minExecutionTimeTf.setEnabled(enabled);
        this.maxExecutionTimeTf.setEnabled(enabled);
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param internalEventDrivenJob
     * @param editMode
     */
    private void setJob(InternalEventDrivenJobInstance internalEventDrivenJob, EditMode editMode) {
        this.enabled = editMode == EditMode.NEW || editMode == EditMode.EDIT ? true : false;
        this.internalEventDrivenJobInstance = internalEventDrivenJob;

        this.init();

        this.formBinder.readBean(this.internalEventDrivenJobInstance);
        this.commandLineTa.setValue(internalEventDrivenJob.getCommandLine());
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    private void updateJobState(InternalEventDrivenJobInstance internalEventDrivenJobInstance, InstanceStatus newStatus) {
        InstanceStatus previousStatus = internalEventDrivenJobInstance.getStatus();
        internalEventDrivenJobInstance.setStatus(newStatus);
        this.updateScheduledJob(internalEventDrivenJobInstance, this.authentication);

        // todo sort out context instance
        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(internalEventDrivenJobInstance,
            null, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
    }

    public void setJob(SchedulerJobInstanceRecord internalEventDrivenJobRecord, EditMode editMode) {
        this.schedulerJobInstanceRecord = internalEventDrivenJobRecord;
        this.setJob((InternalEventDrivenJobInstance) this.schedulerJobInstanceService
            .findById(internalEventDrivenJobRecord.getId()).getSchedulerJobInstance(), editMode);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null) {
                this.internalEventDrivenJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus());
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulerJobStateChangeRegistration.remove();
        this.schedulerJobStateChangeRegistration = null;
    }
}
