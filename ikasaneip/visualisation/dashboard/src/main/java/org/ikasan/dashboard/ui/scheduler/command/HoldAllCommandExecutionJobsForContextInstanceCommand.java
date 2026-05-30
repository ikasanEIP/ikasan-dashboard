package org.ikasan.dashboard.ui.scheduler.command;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.ikasan.dashboard.internationalisation.IkasanI18NProvider;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HoldAllCommandExecutionJobsForContextInstanceCommand {
    private Logger logger = LoggerFactory.getLogger(HoldAllCommandExecutionJobsForContextInstanceCommand.class);
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
            ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
            dialog.open(this.ikasanI18NProvider.getTranslation("progress-dialog.hold-all-jobs-jobs-header", UI.getCurrent().getLocale()),
                this.ikasanI18NProvider.getTranslation("progress-dialog.hold-all-jobs-jobs-body", UI.getCurrent().getLocale()));

            final UI current = UI.getCurrent();
            Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("HoldAllCommand"));
            executor.execute(() -> {
                ContextMachine contextMachine = ContextMachineCache.instance()
                    .getByContextInstanceId(this.contextInstance.getId());

                if (contextMachine != null) {
                    boolean error = false;
                    try {
                        contextMachine.holdJobs(this.contextInstance.getName());
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_HOLDING_ALL_JOBS,
                            String.format("Job Plan Name[%s], Job Plan Identifier[%s]",
                                this.contextInstance.getName(), this.contextInstance.getId()),
                            this.ikasanAuthentication.getName());
                    } catch (Exception e) {
                        logger.error(String.format("An error has occurred holding all jobs for job plan[%s] with instance id[%s]!",
                            contextInstance.getName(), contextInstance.getId()), e);
                        error = true;
                    } finally {
                        boolean finalError = error;
                        current.access(() -> {
                            dialog.close();
                            if (finalError) {
                                NotificationHelper.showUserNotification(this.ikasanI18NProvider.getTranslation("notification.all-jobs-hold-error"
                                    , UI.getCurrent().getLocale()));
                            } else {
                                NotificationHelper.showUserNotification(this.ikasanI18NProvider.getTranslation("notification.all-jobs-successfully-held"
                                    , UI.getCurrent().getLocale()));
                            }
                        });
                    }
                }
            });
        });
    }
}
