package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.SchedulerService;

@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class SchedulerAgentDashboardView extends HorizontalLayout implements BeforeEnterObserver
{
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    private ModuleMetaDataService moduleMetadataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SchedulerService schedulerService;

    private SystemEventLogger systemEventLogger;

    private Board board;

    private boolean initialised = false;

    /**
     * Constructor
     *
     * @param moduleMetadataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerService
     */
    public SchedulerAgentDashboardView(ModuleMetaDataService moduleMetadataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                       ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                       SystemEventLogger systemEventLogger, SchedulerService schedulerService) {
        this.moduleMetadataService = moduleMetadataService;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerService = schedulerService;

        board = new Board();
        board.addClassName("styled");
        board.setSizeFull();

        this.add(board);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            board.addRow(new AgentWidget(this.moduleMetadataService, this.scheduledProcessManagementService
                , this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger
                , this.schedulerService), new SchedulerStatusWidget(this.moduleMetadataService, UI.getCurrent()));

            initialised = true;
        }
    }
}

