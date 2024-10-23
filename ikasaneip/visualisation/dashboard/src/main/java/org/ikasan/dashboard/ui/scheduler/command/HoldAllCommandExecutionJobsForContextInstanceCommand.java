package org.ikasan.dashboard.ui.scheduler.command;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.ikasan.dashboard.internationalisation.IkasanI18NProvider;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.util.ContextInstanceSavedEventBroadcaster;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;

import java.util.List;

public class HoldAllCommandExecutionJobsForContextInstanceCommand {
    private ContextInstance contextInstance;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private SystemEventLogger systemEventLogger;
    private IkasanAuthentication ikasanAuthentication;

    private IkasanI18NProvider ikasanI18NProvider;

    public HoldAllCommandExecutionJobsForContextInstanceCommand(ContextInstance contextInstance, SchedulerJobInstanceService schedulerJobInstanceService
        , SystemEventLogger systemEventLogger, IkasanAuthentication ikasanAuthentication) {
        this.contextInstance = contextInstance;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.systemEventLogger = systemEventLogger;
        this.ikasanAuthentication = ikasanAuthentication;

        this.ikasanI18NProvider = new IkasanI18NProvider();
    }

    public void execute() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(this.ikasanI18NProvider.getTranslation("confirm-dialog.hold-jobs-header", UI.getCurrent().getLocale()));
        confirmDialog.setText(this.ikasanI18NProvider.getTranslation("confirm-dialog.hold-jobs-body", UI.getCurrent().getLocale()));
        confirmDialog.setConfirmText(this.ikasanI18NProvider.getTranslation("button.ok", UI.getCurrent().getLocale()));
        confirmDialog.setCancelText(this.ikasanI18NProvider.getTranslation("button.cancel", UI.getCurrent().getLocale()));
        confirmDialog.setCancelable(true);
        confirmDialog.open();

        confirmDialog.addConfirmListener(confirmEvent -> {
            ContextMachine contextMachine = ContextMachineCache.instance()
                .getByContextInstanceId(this.contextInstance.getId());

            if (contextMachine != null) {
                boolean error = false;
                try {
                    List<SchedulerJobInstanceRecord> updatedRecords = this.schedulerJobInstanceService
                        .holdJobsWithinContext(contextMachine.getContext(), contextMachine.getContext().getName());

                    if (updatedRecords.size() > 0) {
                        updatedRecords.forEach(schedulerJobInstanceRecord -> {
                            SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
                                = new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstanceRecord.getSchedulerJobInstance(),
                                contextMachine.getContext(), InstanceStatus.WAITING, InstanceStatus.ON_HOLD);
                            SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
                        });
                    }

                    this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_HOLDING_ALL_JOBS, String.format("Job Plan Name[%s], Job Plan Identifier[%s]"
                        , contextMachine.getContext().getName(), contextMachine.getContext().getId()), this.ikasanAuthentication.getName());

                    ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
                } catch (Exception e) {
                    e.printStackTrace();
                    error = true;
                } finally {
                    if (error) {
                        NotificationHelper.showUserNotification(this.ikasanI18NProvider.getTranslation("notification.all-jobs-hold-error"
                            , UI.getCurrent().getLocale()));
                    } else {
                        NotificationHelper.showUserNotification(this.ikasanI18NProvider.getTranslation("notification.all-jobs-successfully-held"
                            , UI.getCurrent().getLocale()));
                    }
                }
            }
        });
    }
}
