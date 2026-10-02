package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.esb.service.support.DirectoryZipUtil;
import org.ikasan.esb.service.support.LogFileDownloadAndZipUtil;
import org.ikasan.esb.service.systemevent.SystemEventSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.systemevent.SystemEventRecord;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.security.PermitAll;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Route(value = "adminSupportView", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Administration Support")
@PermitAll
@PreserveOnRefresh
@Component
public class AdministrationSupportView extends HorizontalLayout implements BeforeEnterObserver
{
    private Logger logger = LoggerFactory.getLogger(AdministrationSupportView.class);

    private final LogFileDownloadAndZipUtil logFileDownloadAndZipUtil = new LogFileDownloadAndZipUtil();

    @Value("${max.download.bytes:50000000}")
    private int maxDownloadBytes;

    @Value("${solr.install.dir:}")
    String solrInstallDirectory;

    @Value("${dashboard.log.dir:}")
    String dashboardLogDirectory;

    @Value("${rest.module.username}")
    String restUserName;

    @Value("${rest.module.password}")
    String restPassword;

    @Autowired
    @Qualifier("moduleMetadataEntityService")
    ModuleMetaDataService moduleMetaDataService;

    @Autowired
    @Qualifier("systemEventSearchService")
    private SystemEventSearchService systemEventSearchService;

    private boolean initialised = false;

    /**
     * Constructor
     */
    public AdministrationSupportView() {
        super();
        this.setWidthFull();
    }

    /**
     * Initializes and configures a dashboard widget for downloading dashboard logs as a ZIP file.
     * This widget includes a button to trigger the download and provides context-specific information
     * and use cases for the log download functionality.
     *
     * The method sets up the download process, which streams the logs from the configured log
     * directory as a ZIP file. If the directory does not exist, it displays an error notification.
     *
     * @return a fully configured {@link DashboardWidget} for downloading dashboard logs.
     */
    private DashboardWidget initDashboardLogsDownloadWidget()
    {
        DownloadHandler downloadHandler = event -> {
            // Set download configurations natively on the event context
            event.setFileName("dashboard-logs-" + System.currentTimeMillis() + ".zip");
            event.setContentType("application/zip");

            // Streams the response directly from your REST service to the browser client
            try {
                this.initiateDashboardLogsZipOutputStream(event.getOutputStream());
            } catch (Exception e) {
                throw new RuntimeException("Error processing Dashbord Logs ZIP download", e);
            }
        };

        Anchor downloadAnchor = new Anchor(downloadHandler, "");
        downloadAnchor.getStyle().set("display", "none");

        Button downloadButton = new Button(getTranslation("button.download"), VaadinIcon.DOWNLOAD.create());
        downloadButton.addThemeName("primary");
        downloadButton.setSizeUndefined();
        downloadButton.addClickListener(event -> {
            Path path;
            if(this.dashboardLogDirectory == null || this.dashboardLogDirectory.isEmpty()) {
                path = Path.of(System.getProperty("user.dir"), "logs");
            }
            else {
                path = Path.of(this.dashboardLogDirectory);
            }

            if(!path.toFile().exists()) {
                logger.error("Attempting to download log files for this Ikasan Dashboard but the path[{}] does not exist on the file system!"
                    , path);
                NotificationHelper.showErrorNotification(getTranslation(String.format("error.cannot-download-ikasan-dashboard-logs", path)));
            }
            else {
                downloadAnchor.getElement().callJsFunction("click");
            }
        });

        H4 header = new H4(getTranslation("label.download-aggregate-dashboard-logs"));
        Paragraph p = new Paragraph(getTranslation("label.dashboard-log-download-paragraph"));

        H5 header2 = new H5(getTranslation("header.typical-support-use-cases"));

        UnorderedList useCaseList = new UnorderedList(
            new ListItem(getTranslation("li.uc1")),
            new ListItem(getTranslation("li.uc2")),
            new ListItem(getTranslation("li.uc3"))
        );
        VerticalLayout widgetLayout = new VerticalLayout();
        widgetLayout.setWidthFull();
        widgetLayout.add(header, p, header2, useCaseList, downloadAnchor);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        layout.add(widgetLayout, downloadButton);
        layout.setHorizontalComponentAlignment(Alignment.CENTER, downloadButton);
        layout.setFlexGrow(1.0, widgetLayout);

        DashboardWidget widget = new DashboardWidget(layout);
        widget.setColspan(4);

        return widget;
    }

    /**
     * Initializes and configures a dashboard widget for downloading SOLR logs as a ZIP file.
     * This widget includes a button for initiating the download process and provides
     * contextual information and typical use cases for the download functionality.
     *
     * The method configures the button to handle the download process by streaming
     * the SOLR log directory as a ZIP file. If the directory does not exist, an error
     * notification is displayed to inform the user.
     *
     * @return a fully configured {@link DashboardWidget} for downloading SOLR logs.
     */
    private DashboardWidget initSolrLogsDownloadWidget()
    {
        DownloadHandler downloadHandler = event -> {
            // Set download configurations natively on the event context
            event.setFileName("solr-logs-" + System.currentTimeMillis() + ".zip");
            event.setContentType("application/zip");

            // Streams the response directly from your REST service to the browser client
            try {
                this.initiateSolrLogsZipOutputStream(event.getOutputStream());
            } catch (Exception e) {
                throw new RuntimeException("Error processing SOLR Logs ZIP download", e);
            }
        };

        Anchor downloadAnchor = new Anchor(downloadHandler, "");
        downloadAnchor.getStyle().set("display", "none");

        Button downloadButton = new Button(getTranslation("button.download"), VaadinIcon.DOWNLOAD.create());
        downloadButton.addThemeName("primary");
        downloadButton.setSizeUndefined();
        downloadButton.addClickListener(event -> {
            Path path;
            if(this.solrInstallDirectory == null || this.solrInstallDirectory.isEmpty()) {
                path = Path.of(System.getProperty("user.dir"), "solr", "server", "logs");
            }
            else {
                path = Path.of(this.solrInstallDirectory, "server", "logs");
            }

            if(!path.toFile().exists()) {
                logger.error("Attempting to download log files for this Ikasan SOLR but the path[{}] does not exist on the file system!"
                    , path);
                NotificationHelper.showErrorNotification(getTranslation(String.format("error.cannot-download-ikasan-solr-logs", path)));
            }
            else {
                downloadAnchor.getElement().callJsFunction("click");
            }
        });

        H4 header = new H4(getTranslation("label.download-aggregate-solr-logs", UI.getCurrent().getLocale()));

        Paragraph p = new Paragraph(getTranslation("label.solr-log-download-paragraph"));

        H5 header2 = new H5(getTranslation("header.typical-support-use-cases"));

        UnorderedList useCaseList = new UnorderedList(
            new ListItem(getTranslation("li.uc1")),
            new ListItem(getTranslation("li.uc2")),
            new ListItem(getTranslation("li.uc3"))
        );

        VerticalLayout widgetLayout = new VerticalLayout();
        widgetLayout.setWidthFull();
        widgetLayout.add(header, p, header2, useCaseList, downloadAnchor);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        layout.add(widgetLayout, downloadButton);
        layout.setHorizontalComponentAlignment(Alignment.CENTER, downloadButton);
        layout.setFlexGrow(1.0, widgetLayout);

        DashboardWidget widget = new DashboardWidget(layout);
        widget.setColspan(4);

        return widget;
    }

    /**
     * Initializes and configures a dashboard widget for downloading logs associated with specific modules or agents.
     * This widget allows users to select a module/agent, displays context-specific information
     * and typical use cases for the download functionality, and includes a button to initiate the download.
     *
     * The download process involves streaming the module logs from a server as a ZIP file.
     * If there are errors in accessing module logs (e.g., if the module or agent is not running),
     * an error notification is shown to provide feedback to the user.
     *
     * @return a fully configured {@link DashboardWidget} allowing users to download module logs.
     */
    private DashboardWidget initModuleLogsDownloadWidget()
    {
        IkasanAuthentication authentication = (IkasanAuthentication)SecurityContextHolder
            .getContext().getAuthentication();
        ComboBox<String> moduleNames = new ComboBox<>(getTranslation("label.module-agent-names"));
        moduleNames.setWidth("300px");
        moduleNames.setItems(SecurityUtils.getAccessibleModules(authentication));
        moduleNames.setErrorMessage(getTranslation("error.select-module-agent"));

        DownloadHandler downloadHandler = event -> {
            // Set download configurations natively on the event context
            event.setFileName("module-logs-" + System.currentTimeMillis() + ".zip");
            event.setContentType("application/zip");

            // Streams the response directly from your REST service to the browser client
            try {
                this.initModuleLogsZipOutputStream(event.getOutputStream(), moduleNames.getValue());
            } catch (Exception e) {
                throw new RuntimeException("Error processing Modul Logs ZIP download", e);
            }
        };

        Anchor downloadAnchor = new Anchor(downloadHandler, "");
        downloadAnchor.getStyle().set("display", "none");

        Button downloadButton = new Button(getTranslation("button.download"), VaadinIcon.DOWNLOAD.create());
        downloadButton.setSizeUndefined();
        downloadButton.addThemeName("primary");
        downloadButton.addClickListener(event -> {
            if(moduleNames.getValue() == null) {
                moduleNames.setInvalid(true);
            }
            else {
                ModuleMetaData moduleMetaData = this.moduleMetaDataService.findById(moduleNames.getValue());

                try {
                    this.logFileDownloadAndZipUtil.listLogFiles(moduleMetaData.getUrl()
                        , this.maxDownloadBytes, restUserName, restPassword);
                }
                catch (IOException e) {
                    logger.error("Attempting to download log files for module[{}]. Cannot list log files using URL[{}]. The likely cause is that the agent/module is not running." +
                            " Please make sure that the agent/module is running.", moduleMetaData.getName(), moduleMetaData.getUrl(), e);
                    NotificationHelper.showErrorNotification(getTranslation("error.cannot-list-agent-log-files"));
                    return;
                }
                downloadAnchor.getElement().callJsFunction("click");
            }
        });

        H4 header = new H4(getTranslation("label.download-aggregate-module-logs", UI.getCurrent().getLocale()));

        Paragraph p = new Paragraph(getTranslation("label.module-log-download-paragraph"));

        H5 header2 = new H5(getTranslation("header.typical-support-use-cases"));

        UnorderedList useCaseList = new UnorderedList(
            new ListItem(getTranslation("li.uc4")),
            new ListItem(getTranslation("li.uc1")),
            new ListItem(getTranslation("li.uc2")),
            new ListItem(getTranslation("li.uc3"))
        );

        VerticalLayout widgetLayout = new VerticalLayout();
        widgetLayout.setWidthFull();
        widgetLayout.add(header, p, header2, useCaseList, moduleNames, downloadAnchor);
        widgetLayout.setHorizontalComponentAlignment(Alignment.CENTER, moduleNames);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        layout.add(widgetLayout, downloadButton);
        layout.setHorizontalComponentAlignment(Alignment.CENTER, downloadButton);
        layout.setFlexGrow(1.0, widgetLayout);

        DashboardWidget widget = new DashboardWidget(layout);
        widget.setColspan(4);

        return widget;
    }

    /**
     * Initializes and configures a dashboard widget that displays information and allows the download
     * of system events from the past 24 hours as a ZIP file. The widget includes context-specific
     * details, such as a header, explanatory text, typical use cases, and a button to trigger the download.
     *
     * The download process streams the system events as a ZIP file to the user, providing options
     * to generate logs with specific content types and handling errors during the streaming process.
     *
     * @return a fully configured {@link DashboardWidget} to display and download the last 24 hours'
     * system events.
     */
    private DashboardWidget initLst24HoursSystemEventsWidget()
    {
        IkasanAuthentication authentication = (IkasanAuthentication)SecurityContextHolder
            .getContext().getAuthentication();

        DownloadHandler downloadHandler = event -> {
            // Set download configurations natively on the event context
            event.setFileName("system-events-" + System.currentTimeMillis() + ".zip");
            event.setContentType("application/zip");

            try {
                this.initSystemEventZipOutputStream(event.getOutputStream());
            } catch (Exception e) {
                throw new RuntimeException("Error processing System Events ZIP download", e);
            }
        };

        Anchor downloadAnchor = new Anchor(downloadHandler, "");
        downloadAnchor.getStyle().set("display", "none");

        Button downloadButton = new Button(getTranslation("button.download"), VaadinIcon.DOWNLOAD.create());
        downloadButton.setSizeUndefined();
        downloadButton.addThemeName("primary");
        downloadButton.addClickListener(event -> {
            downloadAnchor.getElement().callJsFunction("click");
        });

        H4 header = new H4(getTranslation("label.download-system-events-24hrs", UI.getCurrent().getLocale()));

        Paragraph p = new Paragraph(getTranslation("label.system-events-24hours-paragraph"));

        H5 header2 = new H5(getTranslation("header.typical-support-use-cases"));

        UnorderedList useCaseList = new UnorderedList(
            new ListItem(getTranslation("li.uc5")),
            new ListItem(getTranslation("li.uc6")),
            new ListItem(getTranslation("li.uc7"))
        );

        VerticalLayout widgetLayout = new VerticalLayout();
        widgetLayout.setWidthFull();
        widgetLayout.add(header, p, header2, useCaseList, downloadAnchor);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        layout.add(widgetLayout, downloadButton);
        layout.setHorizontalComponentAlignment(Alignment.CENTER, downloadButton);
        layout.setFlexGrow(1.0, widgetLayout);

        DashboardWidget widget = new DashboardWidget(layout);
        widget.setColspan(4);

        return widget;
    }

    /**
     * Initiates the process of zipping and streaming the dashboard logs directory as a ZIP file
     * to the provided output stream. If the dashboard log directory is not configured or empty,
     * the method defaults to using the "logs" directory in the user's working directory.
     *
     * @param outputStream the output stream to which the zipped logs will be written
     * @throws Exception if an error occurs during the zipping or streaming process
     */
    private void initiateDashboardLogsZipOutputStream(OutputStream outputStream) throws IOException {
        Path path;
        if(this.dashboardLogDirectory == null || this.dashboardLogDirectory.isEmpty()) {
            path = Path.of(System.getProperty("user.dir"), "logs");
        }
        else {
            path = Path.of(this.dashboardLogDirectory);
        }

        DirectoryZipUtil.zipDirectory(path.toString(), outputStream);
    }

    /**
     * Initiates the process of zipping and streaming the SOLR logs directory as a ZIP file
     * to the provided output stream. If the SOLR installation directory is not configured
     * or is empty, the method defaults to using the "logs" directory within the user's
     * working directory under "solr/server/logs".
     *
     * @param outputStream the output stream to which the zipped logs will be written
     * @throws Exception if an error occurs during the zipping or streaming process
     */
    private void initiateSolrLogsZipOutputStream(OutputStream outputStream) throws IOException {
        Path path;
        if(this.solrInstallDirectory == null || this.solrInstallDirectory.isEmpty()) {
            path = Path.of(System.getProperty("user.dir"), "solr", "server", "logs");
        }
        else {
            path = Path.of(this.solrInstallDirectory, "server", "logs");
        }

        DirectoryZipUtil.zipDirectory(path.toString(), outputStream);
    }

    /**
     * Initializes the process of zipping and streaming logs associated with a specific module
     * to the provided output stream. This method retrieves the metadata for the given module,
     * and if the metadata exists, it downloads and compresses the log files associated with the module.
     * If the metadata is not found, an error is logged to indicate the issue.
     *
     * @param outputStream the output stream to which the zipped module logs will be written
     * @param moduleName the name of the module whose logs are to be downloaded and zipped
     */
    private void initModuleLogsZipOutputStream(OutputStream outputStream, String moduleName) {
        ModuleMetaData moduleMetaData = this.moduleMetaDataService.findById(moduleName);

        if(moduleMetaData != null) {
            logFileDownloadAndZipUtil.downloadAndZipLogFiles(moduleMetaData.getUrl(), this.maxDownloadBytes
                    , outputStream, restUserName, restPassword);
        } else {
            String errorMsg = "Not able to download the zipped modules logs for [" + moduleName + "]. " +
                "The module meta data does not exist in the database!";

            logger.error(errorMsg);
        }
    }

    /**
     * Initializes the process of retrieving system events from the past 24 hours, formatting them
     * as JSON files, and compressing them into a ZIP file, which is then written to the specified
     * output stream. The system events are retrieved using a search filter based on start and end
     * times derived from the current system time.
     *
     * @param outputStream the output stream where the resulting ZIP file containing system events
     *                     will be written
     * @throws IOException if an error occurs during the zipping or streaming process
     */
    private void initSystemEventZipOutputStream(OutputStream outputStream) throws IOException {
        SystemEventSearchFilter systemEventSearchFilter = new SystemEventSearchFilterImpl();
        systemEventSearchFilter.setStartTime(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        systemEventSearchFilter.setEndTime(System.currentTimeMillis());
        AtomicInteger counter = new AtomicInteger(1);
        Map<String, String> systemEvents = this.systemEventSearchService
            .findByFilter(systemEventSearchFilter, -1, -1, null, null).getResultList()
            .stream().map(systemEvent -> ((SystemEventRecord)systemEvent).getPayload())
            .collect(Collectors.toMap(systemEvent -> "systemEvent-"+counter.incrementAndGet()+".json"
                , Function.identity()));

        DirectoryZipUtil.zipFileContents(systemEvents, outputStream);
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SYSTEM_EVENT_ADMIN, SecurityConstants.SYSTEM_EVENT_WRITE
            , SecurityConstants.SYSTEM_EVENT_READ, SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage();
            return;
        }

        if(!initialised) {
            Dashboard board = new Dashboard();
            board.setMinimumColumnWidth("100px");
            board.setDenseLayout(true);
            board.setSizeFull();
            board.setMaximumColumnCount(12);
            board.setMinimumRowHeight("100px");

            board.add(initDashboardLogsDownloadWidget());
            board.add(initSolrLogsDownloadWidget());
            board.add(initModuleLogsDownloadWidget());
            board.add(initLst24HoursSystemEventsWidget());

            this.add(board);

            this.initialised = true;
        }
    }
}
