package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.DownloadLogFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class DownloadModulesLogDialog extends AbstractCloseableResizableDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadModulesLogDialog.class);

    private ModuleMetaData moduleMetaData;
    private DownloadLogFileService downloadLogFileService;
    private Map<String, String> logFilesMap = new HashMap<>();

    public DownloadModulesLogDialog(ModuleMetaData moduleMetaData, DownloadLogFileService downloadLogFileService) {
        this.moduleMetaData = moduleMetaData;
        this.downloadLogFileService = downloadLogFileService;
        super.setVisible(true);
        super.title.setText(getTranslation("label.download-module-log-file", UI.getCurrent().getLocale()));

        // Attempt to get the list of log files. Only modules Ikasan 3.3+ is supported
        try {
            logFilesMap = downloadLogFileService.listLogFiles(moduleMetaData.getUrl());
        } catch (Exception e) {
            LOGGER.error("There was an issue to get the list of logs files form the module: {}", e.getMessage());
        }
        this.init();
    }

    private void init() {

        FormLayout formLayout = new FormLayout();
        formLayout.setVisible(true);
        formLayout.setWidth("350px");
        formLayout.setHeight("220px");
        formLayout.setResponsiveSteps(
            // Use two columns by default
            new FormLayout.ResponsiveStep("0", 1)
        );

        if (logFilesMap == null || logFilesMap.isEmpty()) {
            Paragraph notAvailableMsg = new Paragraph(getTranslation("paragraph.download-module-log-file", UI.getCurrent().getLocale()));
            formLayout.add(notAvailableMsg);

        } else{
            for(Map.Entry<String, String> logFile : logFilesMap.entrySet()) {
                Button downloadLogFileButton = new Button(logFile.getKey());

                StreamResource streamResource = new StreamResource(logFile.getKey(), () -> {
                    try {
                        byte[] file = downloadLogFileService.downloadLogFile(moduleMetaData.getUrl(), logFile.getValue());
                        return new ByteArrayInputStream(file);
                    } catch (Exception e) {
                        return null;
                    }
                });

                FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
                exportWrapper.wrapComponent(downloadLogFileButton);
                formLayout.add(exportWrapper);
            }
        }

        super.content.add(formLayout);
        this.setWidth("350px");
        this.setHeight("220px");
    }
}
