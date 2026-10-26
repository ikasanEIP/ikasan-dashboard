package org.ikasan.dashboard.ui.scheduler.component;

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
import org.ikasan.spec.scheduled.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractGridSchedulerJobInstanceActionWidget extends Div {
    Logger logger = LoggerFactory.getLogger(SchedulerJobInstanceGridWidget.class);
    protected JsonMapper objectMapper = ObjectMapperFactory.newInstance();
    protected IkasanAuthentication authentication;
    protected SchedulerJobInstanceService schedulerJobInstanceService;
    protected ContextInstance contextInstance;
    protected SystemEventLogger systemEventLogger;
    protected ModuleMetaDataService moduleMetaDataService;
    protected LogStreamingService logStreamingService;

    /**
     * Constructs an instance of AbstractGridSchedulerJobInstanceActionWidget.
     *
     * @param moduleMetaDataService the service responsible for handling module metadata; must not be null.
     * @param systemEventLogger the logger used for recording system events; must not be null.
     * @param logStreamingService the service responsible for handling log streaming; must not be null.
     * @param contextInstance the context instance associated with this widget; must not be null.
     * @param schedulerJobInstanceService the service responsible for managing scheduler job instances; must not be null.
     * @throws IllegalArgumentException if any of the provided parameters are null.
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
     * Streams the log (either error or output) associated with a given scheduler job instance.
     * If a valid log is identified, it will be displayed in a log viewer dialog. Otherwise,
     * a notification is shown to inform the user that no log is available for the specified job.
     *
     * @param schedulerJobInstanceRecord the record containing details about the scheduler job instance
     * @param getErrorLog a flag indicating whether to retrieve the error log (true) or the output log (false)
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
     * Skips the execution of a scheduler job instance associated with a specific context.
     * This method first checks if the context machine for the specified job instance
     * is active. If the context machine is inactive, an error notification is displayed,
     * and the operation is aborted. If the context machine is active, the job is skipped,
     * the job state is updated to "SKIPPED", and a corresponding system event is logged.
     *
     * @param schedulerJobInstanceRecord the record representing the scheduler job instance to be skipped
     * @return true if the job is successfully skipped, false otherwise
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
     * Enables a scheduler job instance associated with a specific context.
     * This method verifies if the context machine for the given job instance is active before proceeding
     * with the enable operation. If the context machine is inactive, an error notification is displayed,
     * and the operation is aborted. On successful enablement, the job state is updated, and a corresponding
     * system event is logged.
     *
     * @param schedulerJobInstanceRecord the record representing the scheduler job instance to be enabled
     * @return true if the job is successfully enabled, false otherwise
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
     * Attempts to reinstate a disabled job instance associated with a specific context machine.
     * If the associated context machine is inactive or an error occurs during reinstatement,
     * an error notification is displayed, and the operation fails.
     *
     * @param schedulerJobInstanceRecord The record of the scheduler job instance to be reinstated.
     *                                   Contains details about the job instance and its context.
     */
    protected void reinstateJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-reinstated", UI.getCurrent().getLocale()));
            return;
        }

        try {
            contextMachine.reinstateDisabledJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_DISABLED_SCHEDULED_JOB_REINSTATED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Child Context Name[%s], Job Plan Instance Id[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), this.contextInstance.getId()), this.authentication.getName());
        }
        catch (Exception e) {
            logger.error("An error has occurred reinstating disabled job[{}]!", schedulerJobInstanceRecord.getSchedulerJobInstance(), e);
            NotificationHelper.showErrorNotification(getTranslation("error.reinstated-general-error"));
        }

    }

    /**
     * Puts the specified job into a hold state. This action prevents the job from being processed further
     * until it is explicitly released.
     *
     * @param schedulerJobInstanceRecord the record representing the job instance to be held
     * @return true if the job is successfully put on hold, false if the hold operation fails
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
     * Disables the specified scheduler job instance. This operation prevents the job
     * from being executed while maintaining its record in the system. If the corresponding
     * context is not active, an error notification is displayed.
     *
     * @param schedulerJobInstanceRecord the record representing the scheduler job instance to be disabled
     * @return true if the job is successfully disabled, false otherwise
     */
    protected boolean disableJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-disable"));
            return false;
        }

        try {
            contextMachine.disableJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.DISABLED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_DISABLED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Child Context Name[%s], Job Plan Instance Id[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), this.contextInstance.getName()
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), this.contextInstance.getId()), this.authentication.getName());
        }
        catch (Exception e) {
            logger.error("An error has occurred disabling job[{}]!", schedulerJobInstanceRecord.getSchedulerJobInstance(), e);
            NotificationHelper.showErrorNotification(getTranslation("error.held-general-disable"));
            return false;
        }

        return true;
    }

    /**
     * Releases a scheduler job instance associated with a specific context. This method
     * checks if the context machine for the given job instance is active before attempting
     * the release operation. If the context is not active or an error occurs during the
     * release, an error notification is displayed. On successful release, the job state
     * is updated and a system event is logged.
     *
     * @param schedulerJobInstanceRecord the record representing the scheduler job instance to be released
     * @return true if the job is successfully released, false otherwise
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
     * Updates the state of a scheduler job instance and broadcasts a state change event.
     *
     * @param schedulerJobInstanceRecord the record representing the scheduler job instance to be updated
     * @param newStatus the new status to assign to the scheduler job instance
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

        SchedulerJobStateChangeEventBroadcaster.instance().broadcast(schedulerJobInstanceStateChangeEvent);
    }

    /**
     * Updates the specified scheduled job instance record with the current timestamp,
     * the name of the authenticated user, and the status of the associated scheduler job.
     *
     * @param schedulerJobInstanceRecord the record representing the scheduled job instance to be updated
     * @param authentication the authentication object containing the details of the authenticated user
     */
    protected void updateScheduledJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord, IkasanAuthentication authentication) {

        schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        schedulerJobInstanceRecord.setStatus(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().name());

        this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);
    }

    /**
     * Resets the specified scheduler job instance. If the job belongs to a context that is no longer active,
     * an error notification is displayed and the reset operation is aborted. Otherwise, the job is reset,
     * and a corresponding system event is logged.
     *
     * @param schedulerJobInstance the scheduler job instance to be reset
     * @return true if the job is successfully reset, false if an error occurs during the operation
     */
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
                        ContextHelper.getContextsWhereJobNameMatchResides
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

    /**
     * Submits downstream jobs for the given job instance by evaluating the current context
     * and initializing the initiation events for downstream processing.
     * If no downstream jobs are identified, a user notification is displayed.
     * Otherwise, a confirmation dialog is shown, and upon confirmation, the relevant events
     * are raised to process the downstream jobs.
     *
     * @param internalEventDrivenJobInstance the instance of the job for which downstream jobs will be submitted
     */
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

    /**
     * Represents a unique key for identifying a component in a specific context.
     * The key is defined by a combination of a context name, a child context name,
     * and a job name.
     *
     * This class is primarily used to manage and retrieve components
     * associated with particular contexts and jobs.
     *
     * Fields:
     * - contextName: Represents the name of the primary context.
     * - childContextName: Represents the name of the child context under the primary context.
     * - jobName: Represents the name of the job within the context.
     *
     * Methods:
     * - getContextName(): Returns the primary context name.
     * - getChildContextName(): Returns the name of the child context.
     * - getJobName(): Returns the job name.
     *
     * This class provides implementations of the `equals`, `hashCode`, and
     * `toString` methods to enable its usage in collections and for debugging purposes.
     */
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
