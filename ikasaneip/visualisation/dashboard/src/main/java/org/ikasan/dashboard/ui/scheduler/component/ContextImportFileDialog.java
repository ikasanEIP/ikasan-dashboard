package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// TODO write a vaadin test if we keep this dialogue
public class ContextImportFileDialog extends AbstractCloseableResizableDialog {
    private static final Logger LOG = LoggerFactory.getLogger(ContextImportFileDialog.class);

    private byte[] contextZipFile;

    private ContextProvisionService contextProvisionService;

    private boolean provisionJobs;

    public ContextImportFileDialog(ContextProvisionService contextProvisionService, boolean provisionJobs) {
        this.contextProvisionService = contextProvisionService;
        if (this.contextProvisionService == null) {
            throw new IllegalArgumentException("contextUploadInitialisationService cannot be null!");
        }
        this.provisionJobs = provisionJobs;
        this.init();
    }

    private void init() {
        this.setModal(true);

        VerticalLayout verticalLayout = new VerticalLayout();

        Image mrSquidImage = new Image("/frontend/images/mr-squid-head.png", "");
        mrSquidImage.setHeight("35px");

        Label uploadContextHeader = new Label("Upload Context And Jobs");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setWidthFull();
        horizontalLayout.setHeight("40px");
        horizontalLayout.add(mrSquidImage, uploadContextHeader);
        horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, mrSquidImage, uploadContextHeader);

        verticalLayout.add(horizontalLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, horizontalLayout);

        MemoryBuffer fileBuffer = new MemoryBuffer();
        Upload upload = new Upload(fileBuffer);
        upload.setMaxFiles(1);
        upload.addFinishedListener(event -> {
            InputStream inputStream = fileBuffer.getInputStream();
            try {
                contextZipFile = new byte[inputStream.available()];
                inputStream.read(contextZipFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            if (this.contextZipFile == null) {
                NotificationHelper.showUserNotification(getTranslation("error.provisioning-context-jobs-no-file", UI.getCurrent().getLocale()));
                return;
            }

            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setCancelable(true);
            if (this.provisionJobs) {
                confirmDialog.setHeader(getTranslation("confirm-dialog.provision-context-job-header", UI.getCurrent().getLocale()));
            } else {
                confirmDialog.setHeader(getTranslation("confirm-dialog.provision-context-header", UI.getCurrent().getLocale()));
            }

            confirmDialog.setText(getTranslation("confirm-dialog.provision-context-job-body", UI.getCurrent().getLocale()));

            confirmDialog.open();
            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.setWidth("600px");
                dialog.setHeight("250px");
                dialog.open(getTranslation("progress-dialog.provision-job-header", UI.getCurrent().getLocale()),
                    getTranslation("progress-dialog.provision-job-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        ContextBundle contextBundle
                            = ContextImportZipUtils.extractZipFile(new ByteArrayInputStream(contextZipFile));

                        this.contextProvisionService.provisionContext(contextBundle);

                    } catch (Exception e) {
                        LOG.warn("Could not upload context and jobs error " + e.getMessage());
                        current.access(() -> NotificationHelper.showErrorNotification(getTranslation("error.provisioning-context-jobs", UI.getCurrent().getLocale())));
                    } finally {
                        current.access(() -> {
                            dialog.close();
                            if (this.provisionJobs) {
                                NotificationHelper.showUserNotification(getTranslation("notification.provisioned-context-jobs", UI.getCurrent().getLocale()));
                            } else {
                                NotificationHelper.showUserNotification(getTranslation("notification.provisioned-context", UI.getCurrent().getLocale()));
                            }
                        });
                    }
                });
                this.close();
            });
            confirmDialog.addCancelListener(cancelEvent -> {
                confirmDialog.close();
                this.close();
            });
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);

        verticalLayout.add(upload, buttonLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, upload, buttonLayout);
        this.content.add(verticalLayout);
        super.setWidth("600px");
        super.setHeight("400px");
    }
}