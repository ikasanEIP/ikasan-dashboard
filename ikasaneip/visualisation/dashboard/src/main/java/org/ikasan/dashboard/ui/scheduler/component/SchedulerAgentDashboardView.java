package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.dashboard.component.*;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/hospital-events.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class SchedulerAgentDashboardView extends HorizontalLayout implements BeforeEnterObserver
{
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    private ModuleMetaDataService moduleMetadataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    private Board board;

    private boolean initialised = false;

    public SchedulerAgentDashboardView(ModuleMetaDataService moduleMetadataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                       ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService) {
        this.moduleMetadataService = moduleMetadataService;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        board = new Board();
        board.addClassName("styled");
        board.setSizeFull();

        this.add(board);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            board.addRow(new AgentWidget(this.moduleMetadataService, this.scheduledProcessManagementService
                , this.configurationRestService, this.moduleControlRestService, this.metaDataRestService)
                , new SchedulerStatusWidget(this.moduleMetadataService, UI.getCurrent()));

            initialised = true;
        }
    }
}

