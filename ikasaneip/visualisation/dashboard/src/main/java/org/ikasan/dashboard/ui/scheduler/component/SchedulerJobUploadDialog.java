package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.job.orchestration.broadcast.NewSchedulerJobEventBroadcaster;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

public class SchedulerJobUploadDialog extends AbstractCloseableResizableDialog {
    Logger logger = LoggerFactory.getLogger(SchedulerJobUploadDialog.class);

    private byte[] uploadedJob;
    private SchedulerJobService schedulerJobService;
    private ContextTemplate contextTemplate;
    private Class schedulerJobClass;
    private String label;

    private JsonMapper objectMapper;

    /**
     * Constructor
     */
    public SchedulerJobUploadDialog(ContextTemplate contextTemplate, SchedulerJobService schedulerJobService
        , Class schedulerJobClass, String label)
    {
        this.contextTemplate = contextTemplate;
        if(this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.schedulerJobClass = schedulerJobClass;
        if(this.schedulerJobClass == null) {
            throw new IllegalArgumentException("schedulerJobClass cannot be null!");
        }
        this.label = label;
        if(this.label == null) {
            throw new IllegalArgumentException("label cannot be null!");
        }

        objectMapper = ObjectMapperFactory.newInstance();
        this.init();
    }

    private void init()
    {
        this.setModality(ModalityMode.STRICT);

        VerticalLayout verticalLayout = new VerticalLayout();

        H3 uploadContextHeader = new H3(this.label);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setHeight("40px");
        header.add(uploadContextHeader);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, uploadContextHeader);
        header.getElement().getStyle().set("padding-bottom", "40px");

        verticalLayout.add(header);

        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        UploadHandler inMemoryHandler = UploadHandler.inMemory((metadata, dataStream) -> {
            uploadedJob = dataStream;
        });
        Upload upload = new Upload(inMemoryHandler);
        upload.setMaxFiles(1);

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            if (this.schedulerJobClass.getName().equals(InternalEventDrivenJob.class.getName())) {
                InternalEventDrivenJob schedulerJob;
                try {
                    schedulerJob = (InternalEventDrivenJob) objectMapper
                        .readValue(uploadedJob, this.schedulerJobClass);
                }
                catch (Exception e) {
                    NotificationHelper.showErrorNotification(getTranslation("error.bad-job-template-format", UI.getCurrent().getLocale()));
                    return;
                }

                if(!this.isJobUnique(schedulerJob)) {
                    this.openConfirmDialogForExistingJob(schedulerJob, authentication.getName());
                }
                else {
                    this.saveJob(schedulerJob, authentication.getName());
                }
            }
            else if(this.schedulerJobClass.getName().equals(FileEventDrivenJob.class.getName())) {
                FileEventDrivenJob schedulerJob;
                try {
                    schedulerJob = (FileEventDrivenJob) objectMapper
                        .readValue(uploadedJob, this.schedulerJobClass);
                }
                catch (Exception e) {
                    NotificationHelper.showErrorNotification(getTranslation("error.bad-job-template-format", UI.getCurrent().getLocale()));
                    return;
                }

                if(!this.isJobUnique(schedulerJob)) {
                    this.openConfirmDialogForExistingJob(schedulerJob, authentication.getName());
                }
                else {
                    this.saveJob(schedulerJob, authentication.getName());
                }
            }
            else if(this.schedulerJobClass.getName().equals(QuartzScheduleDrivenJob.class.getName())) {
                QuartzScheduleDrivenJob schedulerJob;
                try {
                    schedulerJob = (QuartzScheduleDrivenJob) objectMapper
                        .readValue(uploadedJob, this.schedulerJobClass);
                }
                catch (Exception e) {
                    NotificationHelper.showErrorNotification(getTranslation("error.bad-job-template-format", UI.getCurrent().getLocale()));
                    return;
                }

                if(!this.isJobUnique(schedulerJob)) {
                    this.openConfirmDialogForExistingJob(schedulerJob, authentication.getName());
                }
                else {
                    this.saveJob(schedulerJob, authentication.getName());
                }
            }

            this.close();
        });

        ComponentSecurityVisibility.applySecurity(saveButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);

        verticalLayout.add(upload, buttonLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, upload, buttonLayout);
        this.content.add(verticalLayout);
        super.showResize(false);
        super.setResizable(false);
        super.setWidth("600px");
        super.setHeight("300px");
    }

    private boolean isJobUnique(SchedulerJob schedulerJob) {
        SchedulerJobRecord schedulerJobRecord = this.schedulerJobService
            .findByContextNameAndJobName(this.contextTemplate.getName(), schedulerJob.getJobName());

        return schedulerJobRecord == null;
    }

    private void openConfirmDialogForExistingJob(SchedulerJob schedulerJob, String actor) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(getTranslation("confirm-dialog.job-exists-header", UI.getCurrent().getLocale()));
        confirmDialog.setText(getTranslation("confirm-dialog.job-exists-text", UI.getCurrent().getLocale()));
        confirmDialog.setConfirmText(getTranslation("button.ok"));
        confirmDialog.setCancelText(getTranslation("button.cancel"));
        confirmDialog.setCancelable(true);
        confirmDialog.open();
        confirmDialog.addConfirmListener(event -> this.saveJob(schedulerJob, actor));
    }

    private void saveJob(SchedulerJob schedulerJob, String actor) {
        try {
            if (schedulerJob instanceof InternalEventDrivenJob) {
                this.schedulerJobService.saveInternalEventDrivenJob((InternalEventDrivenJob) schedulerJob, actor);
                NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);
            } else if (schedulerJob instanceof FileEventDrivenJob) {
                this.schedulerJobService.saveFileEventDrivenJob((FileEventDrivenJob) schedulerJob, actor);
                NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);
            } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                this.schedulerJobService.saveQuartzScheduledJob((QuartzScheduleDrivenJob) schedulerJob, actor);
                NewSchedulerJobEventBroadcaster.broadcast(schedulerJob);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showUserNotification(getTranslation("error.job-upload", UI.getCurrent().getLocale()));
        }
    }
}
