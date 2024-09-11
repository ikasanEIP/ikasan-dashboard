package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;

public class CommandExecutionJobPositionedDialog extends PositionedDialog {
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private LogStreamingService logStreamingService;

    /**
     * This class represents a dialog for executing a command within a scheduled job.
     *
     * @param schedulerJobInstanceRecord The record of the scheduler job instance.
     * @param logStreamingService        The service for streaming logs.
     */
    public CommandExecutionJobPositionedDialog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, LogStreamingService logStreamingService) {
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.logStreamingService = logStreamingService;
        this.init();
    }

    private void init() {
        FormLayout formLayout = new FormLayout();
        TextField startTime = new TextField("Start time");
        if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null) {
            startTime.setValue(this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getFireTime() + " milliseconds");
        }
        startTime.setEnabled(false);
        formLayout.add(startTime);

        super.add(formLayout);
    }
}
