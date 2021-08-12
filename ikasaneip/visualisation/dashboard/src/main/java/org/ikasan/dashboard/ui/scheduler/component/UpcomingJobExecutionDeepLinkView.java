package org.ikasan.dashboard.ui.scheduler.component;


import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Push
@HtmlImport("frontend://styles/shared-styles.html")
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@Theme(Material.class)
@Route(value = "upcomingJobsDeepLink")
@UIScope
@Component
public class UpcomingJobExecutionDeepLinkView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(UpcomingJobExecutionDeepLinkView.class);

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private DateFormatter dateFormatter;

    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    private UpcomingJobExecutionsWidget upcomingJobExecutionsWidget;

    private SystemEventLogger systemEventLogger;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param dateFormatter
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param moduleMetaDataService
     * @param systemEventLogger
     */
    public UpcomingJobExecutionDeepLinkView(ScheduledProcessManagementService scheduledProcessManagementService, DateFormatter dateFormatter
        , ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService
        , @Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetaDataService, SystemEventLogger systemEventLogger)
    {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.configurationRestService = configurationRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.dateFormatter = dateFormatter;
        this.systemEventLogger = systemEventLogger;

        init();
    }

    /**
     * Initialise the internals of the class.
     */
    private void init() {
        this.upcomingJobExecutionsWidget = new UpcomingJobExecutionsWidget(scheduledProcessManagementService
            , dateFormatter, configurationRestService, moduleControlRestService, metaDataRestService, moduleMetaDataService, true, this.systemEventLogger);

        this.add(this.upcomingJobExecutionsWidget);
        this.upcomingJobExecutionsWidget.setSizeFull();
        this.setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {

    }
}
