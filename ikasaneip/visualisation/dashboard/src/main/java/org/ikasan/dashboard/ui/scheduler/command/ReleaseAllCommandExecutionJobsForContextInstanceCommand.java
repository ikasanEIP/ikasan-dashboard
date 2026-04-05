package org.ikasan.dashboard.ui.scheduler.command;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.ikasan.dashboard.internationalisation.IkasanI18NProvider;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReleaseAllCommandExecutionJobsForContextInstanceCommand {
    private ContextInstance contextInstance;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private SystemEventLogger systemEventLogger;
    private IkasanAuthentication ikasanAuthentication;

    private IkasanI18NProvider ikasanI18NProvider;

    public ReleaseAllCommandExecutionJobsForContextInstanceCommand(ContextInstance contextInstance, SchedulerJobInstanceService schedulerJobInstanceService
        , SystemEventLogger systemEventLogger, IkasanAuthentication ikasanAuthentication) {
        this.contextInstance = contextInstance;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.systemEventLogger = systemEventLogger;
        this.ikasanAuthentication = ikasanAuthentication;

        this.ikasanI18NProvider = new IkasanI18NProvider();
    }

    public void execute() {
        ContextMachine contextMachine = ContextMachineCache.instance()
            .getByContextInstanceId(this.contextInstance.getId());
        if (contextMachine != null) {
            List<SchedulerJobInstanceRecord> jobsToReleaseWithinContext = this.schedulerJobInstanceService
                .getJobsToReleaseWithinContext(contextMachine.getContext(), contextMachine.getContext().getName());

            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(this.ikasanI18NProvider.getTranslation("confirm-dialog.release-jobs-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(String.format(this.ikasanI18NProvider.getTranslation("confirm-dialog.release-jobs-body", UI.getCurrent().getLocale())
                , jobsToReleaseWithinContext.size()));
            confirmDialog.setConfirmText(this.ikasanI18NProvider.getTranslation("button.ok", UI.getCurrent().getLocale()));
            confirmDialog.setCancelText(this.ikasanI18NProvider.getTranslation("button.cancel", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.open(this.ikasanI18NProvider.getTranslation("progress-dialog.release-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                    this.ikasanI18NProvider.getTranslation("progress-dialog.release-all-jobs-jobs-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ReleaseAllCommand"));
                executor.execute(() -> {
                    boolean error = false;
                    try {
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_RELEASING_ALL_JOBS_START, String.format("Job Plan Name[%s], Job Plan Identifier[%s]"
                            ,  contextMachine.getContext().getName(), contextMachine.getContext().getId()), this.ikasanAuthentication.getName());
                        if (jobsToReleaseWithinContext.size() > 0) {
                            for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : jobsToReleaseWithinContext) {
                                contextMachine.releaseJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                                    schedulerJobInstanceRecord.getChildContextName());
                            }
                        }
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_RELEASING_ALL_JOBS_END, String.format("Job Plan Name[%s], Job Plan Identifier[%s]"
                            , contextMachine.getContext().getName(), contextMachine.getContext().getId()), this.ikasanAuthentication.getName());
                        ContextInstanceSavedEventBroadcaster.broadcast(contextMachine.getContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = true;
                    } finally {
                        boolean finalError = error;
                        current.access(() -> {
                            dialog.close();

                            if (finalError) {
                                NotificationHelper.showErrorNotification(this.ikasanI18NProvider.getTranslation("notification.all-jobs-released-error"
                                    , UI.getCurrent().getLocale()));
                            } else {
                                NotificationHelper.showUserNotification(this.ikasanI18NProvider.getTranslation("notification.all-jobs-successfully-released"
                                    , UI.getCurrent().getLocale()));
                            }
                        });
                    }
                });
            });
        }
    }
}
