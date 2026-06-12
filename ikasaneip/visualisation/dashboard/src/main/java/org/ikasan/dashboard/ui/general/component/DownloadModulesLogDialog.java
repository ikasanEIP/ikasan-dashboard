package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.client.DownloadLogFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DownloadModulesLogDialog extends AbstractCloseableResizableDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadModulesLogDialog.class);

    private ModuleMetaData moduleMetaData;
    private DownloadLogFileService downloadLogFileService;
    private Map<String, String> logFilesMap = new HashMap<>();
    private Grid<Map.Entry<String, String>> logFileGrid = new Grid<>();
    private FilenameFilter filter = new FilenameFilter();

    public DownloadModulesLogDialog(ModuleMetaData moduleMetaData, DownloadLogFileService downloadLogFileService) {
        this.moduleMetaData = moduleMetaData;
        this.downloadLogFileService = downloadLogFileService;
        super.setVisible(true);
        super.showResize(false);
        super.title.setText(getTranslation("label.download-module-log-file", UI.getCurrent().getLocale()));

        this.init();
    }

    private void init() {

        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

                Button downloadLogFileButton = new Button(logFile.getKey());

                Anchor downloadAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
                    -> new DownloadResponse(new ByteArrayInputStream(downloadLogFileService.downloadLogFile(moduleMetaData.getUrl(), logFile.getValue()))
                    , logFile.getKey(),
                    "plain/test", -1)), "");
                downloadAnchor.add(downloadLogFileButton);

                verticalLayout.add(downloadAnchor);
                verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, downloadAnchor);

                return verticalLayout;
            })).setFlexGrow(4).setKey("download")
            .setHeader(getTranslation("label.download-module-log-file", UI.getCurrent().getLocale()))
            .setSortable(true)
            .setResizable(true);

        HeaderRow hr = this.logFileGrid.appendHeaderRow();
        this.addGridFiltering(hr, "download", new TextField(), this.filter::setFilename);

        this.logFileGrid.setSizeFull();

        super.content.add(this.logFileGrid);
        this.setWidth("550px");
        this.setHeight("700px");
    }

    @Override
    public void open() {
        // Attempt to get the list of log files. Only modules Ikasan 3.3+ is supported
        try {
            logFilesMap = downloadLogFileService.listLogFiles(moduleMetaData.getUrl());
        } catch (Exception e) {
            LOGGER.warn("There was an issue to get the list of logs files form the module: {}", e.getMessage());
        }

        if (logFilesMap == null || logFilesMap.isEmpty()) {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("header.cannot-download-log-file", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("paragraph.download-module-log-file", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(false);
            confirmDialog.open();
        } else{
            DataProvider<Map.Entry<String, String>, FilenameFilter> dataProvider =
                DataProvider.fromFilteringCallbacks(
                    // First callback fetches items based on a query
                    query -> {
                        // The index of the first item to load
                        int offset = query.getOffset();

                        // The number of items to load
                        int limit = query.getLimit();

                        if(query.getSortOrders().size() > 0) {
                            if(query.getSortOrders().get(0).getDirection().equals(SortDirection.DESCENDING)) {
                                Map<String, String> sortedTreeMap = new TreeMap<>(this.logFilesMap);
                                if(this.filter.getFilename() != null && !this.filter.getFilename().isEmpty()) {
                                    return sortedTreeMap.entrySet().stream()
                                        .filter(entry -> entry.getKey().toLowerCase().contains(filter.getFilename().toLowerCase()))
                                        .skip(offset).limit(limit);
                                }
                                else {
                                    return sortedTreeMap.entrySet().stream().skip(offset).limit(limit);
                                }
                            }
                            else if(query.getSortOrders().get(0).getDirection().equals(SortDirection.ASCENDING)) {
                                Map<String, String> sortedTreeMap = new TreeMap<>(Comparator.reverseOrder());
                                sortedTreeMap.putAll(logFilesMap);
                                if(this.filter.getFilename() != null && !this.filter.getFilename().isEmpty()) {
                                    return sortedTreeMap.entrySet().stream()
                                        .filter(entry -> entry.getKey().toLowerCase().contains(filter.getFilename().toLowerCase()))
                                        .skip(offset).limit(limit);
                                }
                                else {
                                    return sortedTreeMap.entrySet().stream().skip(offset).limit(limit);
                                }
                            }
                            else {
                                Map<String, String> sortedTreeMap = new TreeMap<>(this.logFilesMap);
                                if(this.filter.getFilename() != null && !this.filter.getFilename().isEmpty()) {
                                    return logFilesMap.entrySet().stream()
                                        .filter(entry -> entry.getKey().toLowerCase().contains(filter.getFilename().toLowerCase()))
                                        .skip(offset).limit(limit);
                                }
                                else {
                                    return logFilesMap.entrySet().stream().skip(offset).limit(limit);
                                }
                            }
                        }

                        if(this.filter.getFilename() != null && !this.filter.getFilename().isEmpty()) {
                            return logFilesMap.entrySet().stream()
                                .filter(entry -> entry.getKey().toLowerCase().contains(filter.getFilename().toLowerCase()))
                                .skip(offset).limit(limit);
                        }
                        else {
                            return logFilesMap.entrySet().stream().skip(offset).limit(limit);
                        }
                    },
                    // Second callback fetches the total number of items currently in the Grid.
                    // The grid can then use it to properly adjust the scrollbars.
                    query -> {
                        if(this.filter.getFilename() != null && !this.filter.getFilename().isEmpty()) {
                            return logFilesMap.entrySet().stream()
                                .filter(entry -> entry.getKey().toLowerCase().contains(filter.getFilename().toLowerCase())).collect(Collectors.toList()).size();
                        }
                        else {
                            return logFilesMap.entrySet().stream().collect(Collectors.toList()).size();
                        }
                    });

            dataProvider.withConfigurableFilter().setFilter(this.filter);

            this.logFileGrid.setDataProvider(dataProvider);
            this.logFileGrid.getDataProvider().refreshAll();
            super.open();
        }
    }

    private void addGridFiltering(HeaderRow hr, String columnKey, TextField textField, Consumer<String> setFilter) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev-> {
            setFilter.accept(textField.getValue());
            this.logFileGrid.getDataProvider().refreshAll();
        });

        hr.getCell(this.logFileGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    private class FilenameFilter {
        private String filename;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }
}
