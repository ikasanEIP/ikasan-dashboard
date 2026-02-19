package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.ErrorAcknowledgedPositionedDialog;
import org.ikasan.dashboard.ui.scheduler.component.JsonViewerDialog;
import org.ikasan.dashboard.ui.scheduler.component.LogFileHistoryDialog;
import org.ikasan.dashboard.ui.scheduler.component.TextViewerDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class BridgingJobPositionedDialog extends PositionedDialog {
    private Logger logger = LoggerFactory.getLogger(BridgingJobPositionedDialog.class);
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private LogStreamingService logStreamingService;
    private ModuleMetaDataService moduleMetaDataService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextInstance contextInstance;


    /**
     * Constructor for creating a BridgingJobPositionedDialog object.
     *
     * @param schedulerJobInstanceRecord The SchedulerJobInstanceRecord object associated with the dialog.
     * @param logStreamingService The LogStreamingService used for log streaming.
     * @param moduleMetaDataService The ModuleMetaDataService used for module metadata operations.
     * @param schedulerJobInstanceService The SchedulerJobInstanceService for scheduler job instance operations.
     * @param scheduledContextInstanceService The ScheduledContextInstanceService for scheduled context instance operations.
     * @param contextInstance The ContextInstance object associated with the dialog.
     */
    public BridgingJobPositionedDialog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, LogStreamingService logStreamingService,
                                       ModuleMetaDataService moduleMetaDataService, SchedulerJobInstanceService schedulerJobInstanceService,
                                       ScheduledContextInstanceService scheduledContextInstanceService, ContextInstance contextInstance) {
        super(90, 270);
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.logStreamingService = logStreamingService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextInstance = contextInstance;
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
                    if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                        try {
                            ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                            contextMachine.broadcastLocalEvent(this.createSchedulerJobInitiationEvent((BridgingJobInstance) this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                                , contextMachine.getContext()));
                        } catch (IOException e) {
                            e.printStackTrace();
                            NotificationHelper.showErrorNotification(getTranslation("error.execute-bridging-job"));
                        } finally {
                            this.close();
                            NotificationHelper.showErrorNotification(getTranslation("message.execute-bridging-job"));
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
                if(ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
                    try {
                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                        contextMachine.resetJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                            schedulerJobInstanceRecord.getChildContextName());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NotificationHelper.showErrorNotification(getTranslation("error.reset-bridging-job"));
                    }
                    finally {
                        this.close();
                        NotificationHelper.showErrorNotification(getTranslation("message.reset-bridging-job"));
                    }
                }
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
