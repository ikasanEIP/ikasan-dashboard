package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

public class AcknowledgeErrorDialog extends AbstractCloseableResizableDialog {
    private TextField acknowledgementTicketId;
    private TextArea acknowledgementReason;
    private ContextInstance contextInstance;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private SystemEventLogger systemEventLogger;

    /**
     * Create an AcknowledgeErrorDialog.
     *
     * @param contextInstance               The context instance.
     * @param schedulerJobInstanceService   The scheduler job instance service.
     * @param schedulerJobInstanceRecord    The scheduler job instance record.
     */
    public AcknowledgeErrorDialog(ContextInstance contextInstance,
                                  SchedulerJobInstanceService schedulerJobInstanceService,
                                  SchedulerJobInstanceRecord schedulerJobInstanceRecord,
                                  SystemEventLogger systemEventLogger) {
        this.contextInstance = contextInstance;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.systemEventLogger = systemEventLogger;
        this.init();
    }

    /**
     * Initializes the AcknowledgeErrorDialog by setting up the layout, components, and event listeners.
     * The dialog is used to acknowledge an error in a scheduler job instance.
     * Upon acknowledgment, the error details are updated in the database and a notification is shown to the user.
     */
    private void init() {
        Div text = new Div(getTranslation("text.error-acknowledgment", UI.getCurrent().getLocale()));

        FormLayout formLayout = new FormLayout();
        this.acknowledgementTicketId = new TextField(getTranslation("label.ticket-id", UI.getCurrent().getLocale()));
        this.acknowledgementTicketId.getElement().getThemeList().add("always-float-label");
        this.acknowledgementTicketId.setRequired(true);
        this.acknowledgementTicketId.setErrorMessage("A ticket ID is required!");
        formLayout.add(acknowledgementTicketId);
        this.acknowledgementReason = new TextArea(getTranslation("label.reason", UI.getCurrent().getLocale()));
        this.acknowledgementReason.getElement().getThemeList().add("always-float-label");
        this.acknowledgementReason.setRequired(true);
        this.acknowledgementReason.setErrorMessage("A reason is required!");
        this.acknowledgementReason.setMinHeight("200px");
        formLayout.add(acknowledgementReason);

        super.title.setText(getTranslation("label.acknowledge-error", UI.getCurrent().getLocale()));

        Button save = new Button(getTranslation("button.acknowledge", UI.getCurrent().getLocale()));
        save.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            boolean error = false;
            if(this.acknowledgementTicketId.getValue() == null || this.acknowledgementTicketId.getValue().isEmpty()) {
                this.acknowledgementTicketId.setInvalid(true);
                error = true;
            }
            if(this.acknowledgementReason.getValue() == null || this.acknowledgementReason.getValue().isEmpty()) {
                this.acknowledgementReason.setInvalid(true);
                error = true;
            }

            if(error)return;

            try {
                if (ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {

                    if(!((InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance()).isTargetResidingContextOnly()) {
                        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
                        filter.setContextInstanceId(this.contextInstance.getId());
                        filter.setJobName(schedulerJobInstanceRecord.getJobName());

                        SearchResults<SchedulerJobInstanceRecord> searchResults
                            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter
                            (filter, -1, -1, null, null);

                        searchResults.getResultList().forEach(dbRecord -> this.acknowledgeJob(dbRecord));
                    }
                    else {
                        SchedulerJobInstanceRecord dbRecord = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
                        this.acknowledgeJob(dbRecord);
                    }

                    NotificationHelper.showUserNotification(getTranslation("message.error-acknowledged"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showUserNotification(getTranslation("error.error-acknowledged"));
            }
            finally {
                this.close();
            }
        });

        Button cancel = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancel.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(save, cancel);
        buttonLayout.getStyle().set("position", "absolute");
        buttonLayout.getStyle().set("bottom", "30px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(text, formLayout, buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        super.content.add(layout);
        super.setResizable(false);
        super.setWidth("500px");
        super.setHeight("600px");
    }

    /**
     * Acknowledges a job by updating the error details in the scheduler job instance record and
     * broadcasting a state change event.
     *
     * @param schedulerJobInstanceRecord The scheduler job instance record.
     * @throws RuntimeException if an error occurs while acknowledging the job.
     */
    private void acknowledgeJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) throws RuntimeException {
        InternalEventDrivenJobInstance instance = (InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance();
        ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId())
            .acknowledgeSchedulerJobError(instance);
        instance.setErrorAcknowledged(true);
        instance.setErrorAcknowledgmentTicketId(this.acknowledgementTicketId.getValue());
        instance.setErrorAcknowledgedMessage(this.acknowledgementReason.getValue());
        instance.setErrorAcknowledgeTimestamp(System.currentTimeMillis());
        instance.setErrorAcknowledgeUser(SecurityContextHolder.getContext().getAuthentication().getName());
        schedulerJobInstanceRecord.setSchedulerJobInstance(instance);
        this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);


        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(instance,
            this.contextInstance, instance.getStatus(), instance.getStatus());
        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(schedulerJobInstanceStateChangeEvent);

        this.systemEventLogger.logEvent(SystemEventConstants.JOB_ERROR_ACKNOWLEDGED, String.format("Job Plan " +
                "Name[%s], Child Job Plan Name[%s], Job Plan Identifier[%s], Job Name[%s], Ticket Id[%s], Reason[%s]"
            ,this.contextInstance.getName() , instance.getChildContextName(), contextInstance.getId()
            , instance.getJobName(), instance.getErrorAcknowledgmentTicketId(),
            instance.getErrorAcknowledgedMessage()), instance.getErrorAcknowledgeUser());
    }
}
