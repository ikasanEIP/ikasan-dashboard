package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.DownloadModulesLogDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.DownloadLogFileService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SchedulerAgentManagementDialog extends AbstractCloseableResizableDialog {

    private IkasanAuthentication authentication;


    /**
     * Dialog for managing scheduler agent settings and actions.
     *
     * @param agent The metadata of the agent being managed
     * @param downloadLogFileService The service for downloading log files
     * @param scheduledContextService The service for handling scheduled contexts
     * @param jobProvisionService The service for provisioning jobs
     * @param schedulerJobService The service for managing scheduler jobs
     */
    public SchedulerAgentManagementDialog(ModuleMetaData agent, DownloadLogFileService downloadLogFileService
        , ScheduledContextService scheduledContextService, JobProvisionService jobProvisionService,
                                          SchedulerJobService schedulerJobService) {
        super.showResize(false);
        super.setResizable(false);
        super.title.setText(getTranslation("header.scheduler-agent-management", UI.getCurrent().getLocale()));

        this.setHeight("350px");
        this.setWidth("70%");

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        H4 agentDetails = new H4(getTranslation("header.agent-details", UI.getCurrent().getLocale()));

        FormLayout formLayout = new FormLayout();

        TextField agentName = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        agentName.setId("agentName");
        agentName.setValue(agent.getName());
        agentName.setEnabled(false);
        formLayout.add(agentName);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setId("link");
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        TextField agentUrlLf = new TextField(getTranslation("label.agent-url", UI.getCurrent().getLocale()));
        agentUrlLf.setId("agentUrlLf");
        agentUrlLf.setPrefixComponent(link);
        agentUrlLf.setValue(" ");
        formLayout.add(agentUrlLf);

        Button downloadLogFileButton = new Button(getTranslation("button.download-log-file", UI.getCurrent().getLocale()), VaadinIcon.DOWNLOAD.create());
        downloadLogFileButton.setIconAfterText(false);
        downloadLogFileButton.addClickListener(buttonClickEvent -> {
            DownloadModulesLogDialog downloadModulesLogDialog = new DownloadModulesLogDialog(agent, downloadLogFileService);
            downloadModulesLogDialog.open();
        });
        ComponentSecurityVisibility.applySecurity((IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication(), downloadLogFileButton,
            SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE,
            SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN,
            SecurityConstants.SCHEDULER_ALL_WRITE);

        Button synchroniseAllJobPlans = new Button(getTranslation("button.sychronise-all-enable-job-plans", UI.getCurrent().getLocale()), VaadinIcon.ARROWS_LONG_H.create());
        synchroniseAllJobPlans.setIconAfterText(false);
        synchroniseAllJobPlans.addClickListener(buttonClickEvent -> {
            this.synchroniseAllJobsOnAgent(scheduledContextService, jobProvisionService, schedulerJobService, agent);
        });

        ComponentSecurityVisibility.applySecurity((IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication(), downloadLogFileButton,
            SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE,
            SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN,
            SecurityConstants.SCHEDULER_ALL_WRITE);

        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.add(downloadLogFileButton, synchroniseAllJobPlans);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(agentDetails, formLayout, buttonsLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonsLayout);
        buttonsLayout.getElement().getStyle().set("position", "absolute");
        buttonsLayout.getElement().getStyle().set("bottom", "30px");
        super.content.add(layout);
    }


    /**
     * Synchronises all jobs on a given agent.
     *
     * @param scheduledContextService the ScheduledContextService instance
     * @param jobProvisionService the JobProvisionService instance
     * @param schedulerJobService the SchedulerJobService instance
     * @param agent the agent for which to synchronise the jobs
     */
    private void synchroniseAllJobsOnAgent(ScheduledContextService scheduledContextService, JobProvisionService jobProvisionService,
                                           SchedulerJobService schedulerJobService, ModuleMetaData agent) {
        List<ScheduledContextRecord> contextTemplateList = scheduledContextService.findAll().getResultList().stream()
            .filter(scheduledContextRecord -> !scheduledContextRecord.isDisabled())
            .collect(Collectors.toList());
        List<SchedulerJob> schedulerJobs = new ArrayList<>();

        contextTemplateList.forEach(record -> {
            schedulerJobs.addAll((Collection<? extends SchedulerJob>) schedulerJobService.findByContext(record.getContextName(), -1,-1).getResultList().stream()
                .filter(jobRecord -> ((SchedulerJobRecord)jobRecord).getAgentName().equals(agent.getName()))
                .map(jobRecord -> ((SchedulerJobRecord)jobRecord).getJob())
                .collect(Collectors.toList()));
        });

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setCancelable(true);
        confirmDialog.setHeader(getTranslation("confirm-dialog.provision-job-header", UI.getCurrent().getLocale()));
        confirmDialog.setText(getTranslation("confirm-dialog.provision-job-body", UI.getCurrent().getLocale()));

        confirmDialog.open();

        confirmDialog.addConfirmListener(confirmEvent -> {
            ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
            dialog.setWidth("600px");
            dialog.setHeight("250px");
            dialog.open(getTranslation("progress-dialog.provision-job-header", UI.getCurrent().getLocale()),
                getTranslation("progress-dialog.provision-job-body", UI.getCurrent().getLocale()));

            final UI current = UI.getCurrent();
            Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextTemplateManagementWidget"));
            executor.execute(() -> {
                boolean success = true;
                try {
                    jobProvisionService.provisionJobs(schedulerJobs, this.authentication.getName());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                    dialog.close();
                    current.access(() -> NotificationHelper.showErrorNotification(getTranslation("error.provisioning-jobs", UI.getCurrent().getLocale())));
                }

                if(success) {
                    current.access(() -> {
                        dialog.close();
                        NotificationHelper.showUserNotification(getTranslation("notification.provisioned-jobs", UI.getCurrent().getLocale()));
                    });
                }
            });

        });
    }
}
