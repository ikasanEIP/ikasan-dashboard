package org.ikasan.dashboard.ui.visualisation.view;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.rest.client.ReplayRestServiceImpl;
import org.ikasan.rest.client.ResubmissionRestServiceImpl;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.service.ConfigurationMetaDataService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.module.client.TriggerService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;
import java.util.List;

@Route(value = "visualisation", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Visualisation")
@Component
@PermitAll
public class GraphView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(GraphView.class);

    @Autowired
    private ModuleControlService moduleControlRestService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Autowired
    private ConfigurationService configurationRestService;

    @Autowired
    private TriggerService triggerRestService;

    @Autowired
    private ConfigurationMetaDataService configurationMetadataService;

    @Autowired
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    @Autowired
    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;

    @Autowired
    private HospitalAuditService hospitalAuditService;

    @Autowired
    private ResubmissionRestServiceImpl resubmissionRestService;

    @Autowired
    private ReplayRestServiceImpl replayRestService;

    @Autowired
    private BatchInsert replayAuditService;

    @Autowired
    private MetaDataService metaDataApplicationRestService;

    @Autowired
    private BatchInsert<ModuleMetaData> moduleMetadataBatchInsert;

    @Autowired
    private DateFormatter dateFormatter;

    @Value(value = "${integrated.systems.image.path}")
    private String dynamicImagePath;

    @Value("${max.download.bytes:50000000}")
    private int maxDownloadBytes;

    private GraphVisualisation graphVisualisation;

    private boolean initialised = false;

    /**
     * Constructor
     */
    public GraphView()
    {
        this.setMargin(false);

        this.setWidth("100%");
        this.setHeight("100%");

        this.getElement().getThemeList().remove("padding");
        this.getElement().getThemeList().remove("spacing");
    }

    /**
     * Initializes the GraphView component by creating and configuring an instance of the GraphVisualisation
     * class with the necessary services and dependencies. The initialized GraphVisualisation instance is then
     * added to the current layout, and the initialised flag is set to true to indicate that the setup is complete.
     */
    private void init() {
        this.graphVisualisation = new GraphVisualisation(this.esbSearchService,
            this.moduleControlRestService, this.moduleMetadataService, this.configurationRestService,
            this.configurationMetadataService, this.businessStreamMetaDataService,
            this.hospitalAuditService, this.resubmissionRestService, this.replayRestService, this.replayAuditService,
            this.metaDataApplicationRestService, this.moduleMetadataBatchInsert, this.triggerRestService, this.dynamicImagePath,
            this.dateFormatter, this.maxDownloadBytes);

        this.add(graphVisualisation);

        this.initialised = true;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!this.initialised) {
            this.init();
            List<ModuleMetaData> moduleMetaData = this.moduleMetadataService.findAll();
            if(moduleMetaData.size() > 0) {
                this.graphVisualisation.setVisualisationName(moduleMetaData.get(0).getName());
                this.graphVisualisation.setVisualisationType(VisualisationType.MODULE.name());
            }
            this.graphVisualisation.beforeEnter(beforeEnterEvent);
        }
    }
}

