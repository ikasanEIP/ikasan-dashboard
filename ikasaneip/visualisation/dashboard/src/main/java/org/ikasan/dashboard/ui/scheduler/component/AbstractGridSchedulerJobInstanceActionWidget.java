package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractGridSchedulerJobInstanceActionWidget extends Div {
    Logger logger = LoggerFactory.getLogger(SchedulerJobInstanceGridWidget.class);
    protected ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    protected IkasanAuthentication authentication;
    protected SchedulerJobInstanceService schedulerJobInstanceService;
    protected ContextInstance contextInstance;
    protected SystemEventLogger systemEventLogger;
    protected ModuleMetaDataService moduleMetaDataService;
    protected LogStreamingService logStreamingService;

    /**
     * Constructor
     *
     * @param moduleMetaDataService
     * @param systemEventLogger
     * @param logStreamingService
     * @param contextInstance
     * @param schedulerJobInstanceService
     */
    public AbstractGridSchedulerJobInstanceActionWidget(ModuleMetaDataService moduleMetaDataService, SystemEventLogger systemEventLogger,
                                                        LogStreamingService logStreamingService, ContextInstance contextInstance,
                                                        SchedulerJobInstanceService schedulerJobInstanceService) {

        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if(this.contextInstance ==  null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger ==  null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService ==  null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.logStreamingService = logStreamingService;
        if(this.logStreamingService ==  null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.setSizeFull();
    }

    /**
     * Method to initialise and create all action icons for a given job record. All icons are added to the provided layout.
     *
     * @param componentKey
     * @param schedulerJobInstanceRecord
     * @param horizontalLayout
     */
    protected abstract void getJobInstanceActionComponents(ComponentKey componentKey, SchedulerJobInstanceRecord schedulerJobInstanceRecord, HorizontalLayout horizontalLayout);

    /**
     * Helper method to stream job log files.
     *
     * @param schedulerJobInstanceRecord
     * @param getErrorLog
     */
    protected void streamLog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, boolean getErrorLog) {
        boolean displayLog = false;
        String host = null;
        String endPoint = null;
        String outputLog = null;

        ModuleMetaData agent = moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
        ScheduledProcessEvent scheduledProcessEvent = schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent();

        if (scheduledProcessEvent != null && agent != null) {
            host = agent.getUrl();
            endPoint = "/rest/logs";
            outputLog = getErrorLog ? scheduledProcessEvent.getResultError() : scheduledProcessEvent.getResultOutput();
            logger.info(String.format("Streaming log for host %s, endPoint %s, log %s", host, endPoint, outputLog));
            if (outputLog != null && host != null) {
                displayLog = true;
            }
        }

        if (displayLog) {
            SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
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
    protected boolean skipJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-skipped", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), true);
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.SKIPPED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true, this.contextInstance.getName()
                    , this.contextInstance.getId()), this.authentication.getName());

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
    protected boolean enableJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-enabled", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), false);
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_ENABLED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), false, this.contextInstance.getName()
                    , this.contextInstance.getId()), this.authentication.getName());
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
    protected boolean holdJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-held", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.holdJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.ON_HOLD);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, String.format("Agent Name[%s], Scheduled Job Name[%s], Held[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true, this.contextInstance.getName()
                    , this.contextInstance.getId()), this.authentication.getName());
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
    protected boolean releaseJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-released", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.releaseJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RELEASED, String.format("Agent Name[%s], Scheduled Job Name[%s], Released[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                    , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true, this.contextInstance.getName()
                    , this.contextInstance.getId()), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.released-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to update the state of a job and to broadcast that state change.
     *
     * @param schedulerJobInstanceRecord
     * @param newStatus
     */
    protected void updateJobState(SchedulerJobInstanceRecord schedulerJobInstanceRecord, InstanceStatus newStatus) {
        InstanceStatus previousStatus = schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus();
        SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
        schedulerJobInstance.setStatus(newStatus);
        schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
        updateScheduledJob(schedulerJobInstanceRecord, this.authentication);

        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance,
            this.contextInstance, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
    }

    /**
     * Update a scheduled job.
     *
     * @param schedulerJobInstanceRecord
     * @param authentication
     */
    protected void updateScheduledJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord, IkasanAuthentication authentication) {

        schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        schedulerJobInstanceRecord.setStatus(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().name());

        this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);
    }

    protected boolean resetJob(SchedulerJobInstance schedulerJobInstance) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstance.getContextInstanceId());

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
                if(schedulerJobInstance instanceof InternalEventDrivenJobInstance) {
                    if(((InternalEventDrivenJobInstance) schedulerJobInstance).isTargetResidingContextOnly()) {
                        contextMachine.resetJob(schedulerJobInstance.getIdentifier(), schedulerJobInstance.getChildContextName());
                    }
                    else {
                        ContextHelper.getContextsWhereJobFilterMatchResides
                            (this.contextInstance, schedulerJobInstance.getJobName())
                            .forEach(name
                            -> contextMachine.resetJob(schedulerJobInstance.getIdentifier(), name));
                    }
                }
                else {
                    contextMachine.resetJob(schedulerJobInstance.getIdentifier(), schedulerJobInstance.getChildContextName());
                }

                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RESET, String.format("Agent Name[%s], Scheduled Job Name[%s], Reset[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                    , schedulerJobInstance.getAgentName(), schedulerJobInstance.getJobName(), true, this.contextInstance.getName(), this.contextInstance.getId()), this.authentication.getName());
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error(String.format("An error has occurred resetting job[%s], context name[%s], context instance id[%s]"
                    , schedulerJobInstance.getJobName(), schedulerJobInstance.getContextName()
                    , schedulerJobInstance.getContextInstanceId()), e);
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

    protected void submitDownstreamJobs(InternalEventDrivenJobInstance internalEventDrivenJobInstance) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        contextualisedScheduledProcessEvent.setJobStarting(false);
        contextualisedScheduledProcessEvent.setJobName(internalEventDrivenJobInstance.getJobName());
        contextualisedScheduledProcessEvent.setAgentName(internalEventDrivenJobInstance.getAgentName());
        contextualisedScheduledProcessEvent.setContextName(internalEventDrivenJobInstance.getContextName());
        contextualisedScheduledProcessEvent.setRaisedDueToFailureResubmission(true);
        contextualisedScheduledProcessEvent.setInternalEventDrivenJob(internalEventDrivenJobInstance);

        List<SchedulerJobInitiationEvent> initiationEvents = contextMachine.getEventsThatCanRun(contextualisedScheduledProcessEvent);

        if(initiationEvents.isEmpty()) {
            NotificationHelper.showUserNotification(getTranslation("notification.no-downstream-jobs-to-initiate", UI.getCurrent().getLocale()));
            return;
        }

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setCancelable(true);
        confirmDialog.setHeader(getTranslation("confirm-dialog.downstream-job-initiation-header", UI.getCurrent().getLocale()));

        VerticalLayout verticalLayout = new VerticalLayout();
        initiationEvents.forEach(initiationEvent -> {
            Div jobName = new Div();
            jobName.setText(initiationEvent.getJobName());
            verticalLayout.add(jobName);
        });
        confirmDialog.setText(verticalLayout);
        confirmDialog.setConfirmText(getTranslation("button.ok"));
        confirmDialog.setCancelText(getTranslation("button.cancel"));

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
                NotificationHelper.showErrorNotification(getTranslation("error.downstream-job-initiation", UI.getCurrent().getLocale()));
            }

            NotificationHelper.showUserNotification(getTranslation("notification.downstream-job-initiation", UI.getCurrent().getLocale()));
        });
    }

    protected class ComponentKey {
        String contextName;
        String childContextName;
        String jobName;

        public ComponentKey(String contextName, String childContextName, String jobName) {
            this.contextName = contextName;
            this.childContextName = childContextName;
            this.jobName = jobName;
        }

        public String getContextName() {
            return contextName;
        }

        public String getChildContextName() {
            return childContextName;
        }

        public String getJobName() {
            return jobName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComponentKey that = (ComponentKey) o;
            return Objects.equals(contextName, that.contextName)
                && Objects.equals(childContextName, that.childContextName)
                && Objects.equals(jobName, that.jobName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextName, childContextName, jobName);
        }

        @Override
        public String toString() {
            return "StatusImageKey{" +
                "contextName='" + contextName + '\'' +
                ", childContextName='" + childContextName + '\'' +
                ", jobName='" + jobName + '\'' +
                '}';
        }
    }
}
