package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.commons.lang.SerializationUtils;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InternalEventDrivenJobSubmissionDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(InternalEventDrivenJobSubmissionDialog.class);

    private ModuleMetaDataService moduleMetaDataService;

    private InternalEventDrivenJobInstance internalEventDrivenJobInstance;

    private SystemEventLogger systemEventLogger;
    private IkasanAuthentication authentication;
    private ContextInstance contextInstance;
    private JobInitiationService jobInitiationService;
    private Map<String, ContextParameterInstance> contextParameterInstanceMap;
    private List<ContextParameterInstance> contextParameterInstances;


    /**
     * Constructor
     *
     * @param systemEventLogger
     * @param moduleMetaDataService
     * @param contextInstance
     * @param jobInitiationService
     * @param internalEventDrivenJobInstance
     */
    public InternalEventDrivenJobSubmissionDialog(SystemEventLogger systemEventLogger, ModuleMetaDataService moduleMetaDataService,
                                                  ContextInstance contextInstance, JobInitiationService jobInitiationService,
                                                  InternalEventDrivenJobInstance internalEventDrivenJobInstance) {
        super.showResize(false);
        super.title.setText(getTranslation("label.command-execution-job-submission", UI.getCurrent().getLocale()));

        this.systemEventLogger = systemEventLogger;
        this.moduleMetaDataService = moduleMetaDataService;
        this.contextInstance = contextInstance;
        this.jobInitiationService = jobInitiationService;
        this.internalEventDrivenJobInstance = internalEventDrivenJobInstance;

        authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.init();
    }

    private void init() {
        this.setHeight("500px");
        this.setWidth("1000px");

        this.initialiseContextInstanceParameterMap();

        this.createJobSubmissionParametersForm();
        super.content.getStyle().set("padding-top", "0px");
        super.content.getStyle().set("padding-bottom", "10px");
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private void createJobSubmissionParametersForm() {
        H3 jobSubmissionParametersLabel = new H3(getTranslation("label.job-submission-parameters", UI.getCurrent().getLocale()));

        contextParameterInstances = new ArrayList<>();

        this.internalEventDrivenJobInstance.getContextParameters().forEach(param -> {
            contextParameterInstances.add((ContextParameterInstance) SerializationUtils.clone(this.contextParameterInstanceMap.get(param.getName())));
        });

        GridPro<ContextParameterInstance> grid = new GridPro<>();
        grid.addColumn(ContextParameterInstance::getName).setHeader(getTranslation("table-header.name", UI.getCurrent().getLocale()));
        grid.addColumn(ContextParameterInstance::getType)
            .setHeader(getTranslation("table-header.type", UI.getCurrent().getLocale()));
        grid.addEditColumn(ContextParameterInstance::getValue)
            .text(ContextParameterInstance::setValue)
            .setHeader(getTranslation("table-header.value", UI.getCurrent().getLocale()));

        grid.setWidth("100%");
        grid.setHeight("400px");

        grid.setItems(contextParameterInstances);

        Button submitButton = new Button(getTranslation("button.submit-job", UI.getCurrent().getLocale()));
        submitButton.addClickListener(event -> {
            this.submitJob();
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(event -> {
            this.close();
        });

        VerticalLayout buttonWrapper = new VerticalLayout();
        buttonWrapper.setMargin(false);
        buttonWrapper.setWidth("100%");

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.add(submitButton, cancelButton);

        buttonWrapper.add(buttonLayout);
        buttonWrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

        super.content.add(jobSubmissionParametersLabel, grid, buttonWrapper);
    }

    private void initialiseContextInstanceParameterMap() {
        this.contextParameterInstanceMap = this.contextInstance.getContextParameters().stream()
            .collect(Collectors.toMap(ContextParameterInstance::getName, Function.identity()));
    }

    /**
     * Helper method to submit the job.
     *
     * @return
     */
    private void submitJob() {
        try {
            ModuleMetaData agent = this.moduleMetaDataService.findById(this.internalEventDrivenJobInstance.getAgentName());
            SchedulerJobInitiationEvent schedulerJobInitiationEvent = createSchedulerJobInitiationEvent(agent, this.internalEventDrivenJobInstance
                , this.contextParameterInstances, this.contextInstance);
            logger.info("Submitting job[{}] to [{}]", schedulerJobInitiationEvent, agent.getUrl());
            this.jobInitiationService.raiseSchedulerJobInitiationEvent(agent.getUrl(), schedulerJobInitiationEvent);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName()), this.authentication.getName());

            NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
            this.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
        }
    }

    private SchedulerJobInitiationEvent createSchedulerJobInitiationEvent(ModuleMetaData agent, InternalEventDrivenJobInstance internalEventDrivenJob
        , List<ContextParameterInstance> contextParameters, ContextInstance contextInstance) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(internalEventDrivenJob.getAgentName());
        schedulerJobInitiationEvent.setJobName(internalEventDrivenJob.getJobName());
        schedulerJobInitiationEvent.setContextName(contextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(contextInstance.getId());
        schedulerJobInitiationEvent.setContextParameters(contextParameters);
        schedulerJobInitiationEvent.setAgentUrl(agent.getUrl());
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        if(internalEventDrivenJob.getChildContextNames() != null && internalEventDrivenJob.isTargetResidingContextOnly()){
            schedulerJobInitiationEvent.setChildContextNames(List.of(internalEventDrivenJob.getChildContextName()));
        }
        else {
            schedulerJobInitiationEvent.setChildContextNames(internalEventDrivenJob.getChildContextNames());
        }

        return schedulerJobInitiationEvent;
    }
}
