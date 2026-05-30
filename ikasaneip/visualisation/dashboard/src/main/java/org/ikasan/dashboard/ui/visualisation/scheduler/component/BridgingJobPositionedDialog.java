package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.BridgingJobInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class BridgingJobPositionedDialog extends PositionedDialog {
    private Logger logger = LoggerFactory.getLogger(BridgingJobPositionedDialog.class);
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private LogStreamingService logStreamingService;
    private ModuleMetaDataService moduleMetaDataService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextInstance contextInstance;
    private SystemEventLogger systemEventLogger;

    /**
     * Constructs a new instance of the BridgingJobPositionedDialog class.
     * This dialog is designed to manage the interaction and display of a scheduler job instance record
     * along with associated services and context information.
     *
     * @param schedulerJobInstanceRecord The record containing details about the scheduler job instance.
     * @param logStreamingService The service responsible for handling log streaming functionality.
     * @param moduleMetaDataService The service providing metadata information about the module.
     * @param schedulerJobInstanceService The service to manage or retrieve scheduler job instance details.
     * @param scheduledContextInstanceService The service to handle scheduled context instances.
     * @param contextInstance The context instance associated with the current job.
     * @param systemEventLogger The logger used to record system events and actions.
     */
    public BridgingJobPositionedDialog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, LogStreamingService logStreamingService,
                                       ModuleMetaDataService moduleMetaDataService, SchedulerJobInstanceService schedulerJobInstanceService,
                                       ScheduledContextInstanceService scheduledContextInstanceService, ContextInstance contextInstance,
                                       SystemEventLogger systemEventLogger) {
        super(90, 270);
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.logStreamingService = logStreamingService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextInstance = contextInstance;
        this.systemEventLogger = systemEventLogger;
        this.init();
    }

    /**
     * Initialize the dialog by setting up the button layout and adding necessary buttons based on the status of the
     * scheduler job instance. If the status is WAITING, a button to execute the job is added with a confirmation dialog.
     * If the status is COMPLETE, a button to reset the job is added.
     */
    private void init() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);

        NativeLabel label = new NativeLabel(getTranslation("menu-item.bridging-job"));
        buttonLayout.add(label);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label);

        IkasanAuthentication ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.WAITING)) {
            Button executeJobButton = new Button(getTranslation("button.execute"), VaadinIcon.PLAY.create());
            executeJobButton.setIconAfterText(true);
            executeJobButton.getElement().setAttribute("title", getTranslation("tooltip.execute-bridging-job"
                , UI.getCurrent().getLocale()));
            executeJobButton.addClickListener( event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.execute-bridging-job-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.execute-bridging-job-text", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                    if (contextMachine != null) {
                        boolean error = false;
                        try {
                            contextMachine.broadcastLocalEvent(this.createSchedulerJobInitiationEvent((BridgingJobInstance) this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                                , contextMachine.getContext()));
                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED
                                , String.format("Bridging job submitted[%s], context[%s], context instance[%s], user[%s]!"
                                , this.schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                , this.contextInstance.getId(), ikasanAuthentication.getName()), ikasanAuthentication.getName());
                            logger.info(String.format("Successfully executed bridging job[%s], context[%s], context instance[%s], user[%s]!"
                                , this.schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                , this.contextInstance.getId(), ikasanAuthentication.getName()));
                        } catch (IOException e) {
                            logger.error(String.format("Error executing  bridging job[%s], context[%s], context instance[%s], user[%s]!"
                                , this.schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                , this.contextInstance.getId(), ikasanAuthentication.getName()), e);
                            NotificationHelper.showErrorNotification(getTranslation("error.execute-bridging-job"));
                            error = true;
                        } finally {
                            this.close();
                            if(!error) {
                                NotificationHelper.showErrorNotification(getTranslation("message.execute-bridging-job"));
                            }
                        }
                    }
                });
            });
            buttonLayout.add(executeJobButton);
        }
        else if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.COMPLETE)) {
            Button resetJobButton = new Button(getTranslation("button.reset"), VaadinIcon.ARROW_BACKWARD.create());
            resetJobButton.setIconAfterText(true);
            resetJobButton.getElement().setAttribute("title", getTranslation("tooltip.reset-bridging-job"
                , UI.getCurrent().getLocale()));
            resetJobButton.addClickListener(iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.reset-bridging-job-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.reset-bridging-job-text", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                    if (contextMachine != null) {
                        boolean error = false;
                        try {
                            contextMachine.resetJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                                schedulerJobInstanceRecord.getChildContextName());
                            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RESET
                                , String.format("Bridging job reset[%s], context[%s], context instance[%s], user[%s]!"
                                    , this.schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                    , this.contextInstance.getId(), ikasanAuthentication.getName()), ikasanAuthentication.getName());
                            logger.info(String.format("Successfully reset bridging job[%s], context[%s], context instance[%s], user[%s]!"
                                , this.schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                , this.contextInstance.getId(), ikasanAuthentication.getName()));
                        } catch (Exception e) {
                            logger.error(String.format("Error resetting bridging job[%s], context[%s], context instance[%s], user[%s]!"
                                , this.schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                                , this.contextInstance.getId(), ikasanAuthentication.getName()), e);
                            NotificationHelper.showErrorNotification(getTranslation("error.reset-bridging-job"));
                            error = true;
                        } finally {
                            this.close();
                            if(!error) {
                                NotificationHelper.showErrorNotification(getTranslation("message.reset-bridging-job"));
                            }
                        }
                    }
                });
            });
            buttonLayout.add(resetJobButton);
        }

        super.add(buttonLayout);
    }

    /**
     * Creates a SchedulerJobInitiationEvent based on the provided BridgingJobInstance and ContextInstance.
     *
     * @param bridgingJobInstance The BridgingJobInstance object to get agent name, job name, and child context names from.
     * @param contextInstance The ContextInstance object to get name and ID from.
     * @return The created SchedulerJobInitiationEvent with the agent name, job name, context name, context instance ID,
     * child context names, and other necessary fields set.
     */
    private SchedulerJobInitiationEvent createSchedulerJobInitiationEvent(BridgingJobInstance bridgingJobInstance
        , ContextInstance contextInstance) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(bridgingJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(bridgingJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(contextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(contextInstance.getId());
        schedulerJobInitiationEvent.setChildContextNames(bridgingJobInstance.getChildContextNames());


        return schedulerJobInitiationEvent;
    }
}
