package org.ikasan.dashboard.ui.visualisation.view;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.rest.client.ReplayRestServiceImpl;
import org.ikasan.rest.client.ResubmissionRestServiceImpl;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.*;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.module.client.TriggerService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.solr.SolrGeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Push
@HtmlImport("frontend://styles/shared-styles.html")
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@Theme(Material.class)
@Route(value = "visualisationTab")
@UIScope
@Component
public class GraphVisualisationDeepLinkView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(GraphVisualisationDeepLinkView.class);

    private GraphVisualisation graphVisualisation;

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService;
    private ModuleControlService moduleControlRestService;
    private ModuleMetaDataService moduleMetadataService;
    private ConfigurationService configurationRestService;
    private TriggerService triggerRestService;
    private ConfigurationMetaDataService configurationMetadataService;
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;
    private HospitalAuditService hospitalAuditService;
    private ResubmissionRestServiceImpl resubmissionRestService;
    private ReplayRestServiceImpl replayRestService;
    private BatchInsert replayAuditService;
    private MetaDataService metaDataApplicationRestService;
    private BatchInsert<ModuleMetaData> moduleMetadataBatchInsert;

    @Value(value = "${integrated.systems.image.path}")
    private String dynamicImagePath;

    private boolean initialised = false;

    private String visualisationType = null;
    private String visualisationName = null;

    private DateFormatter dateFormatter;

    public GraphVisualisationDeepLinkView(SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService, ModuleControlService moduleControlRestService,
                                          ModuleMetaDataService moduleMetadataService, ConfigurationService configurationRestService, ConfigurationMetaDataService configurationMetadataService,
                                          BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
                                          SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService, HospitalAuditService hospitalAuditService,
                                          ResubmissionRestServiceImpl resubmissionRestService, ReplayRestServiceImpl replayRestService, BatchInsert replayAuditService, MetaDataService metaDataApplicationRestService,
                                          BatchInsert<ModuleMetaData> moduleMetadataBatchInsert, TriggerService triggerRestService, DateFormatter dateFormatter)
    {
        this.solrSearchService = solrSearchService;
        this.moduleControlRestService = moduleControlRestService;
        this.moduleMetadataService = moduleMetadataService;
        this.configurationRestService = configurationRestService;
        this.configurationMetadataService = configurationMetadataService;
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        this.solrGeneralService = solrGeneralService;
        this.hospitalAuditService = hospitalAuditService;
        this.resubmissionRestService = resubmissionRestService;
        this.replayRestService = replayRestService;
        this.replayAuditService = replayAuditService;
        this.metaDataApplicationRestService = metaDataApplicationRestService;
        this.moduleMetadataBatchInsert = moduleMetadataBatchInsert;
        this.triggerRestService = triggerRestService;
        this.dateFormatter = dateFormatter;
    }

    private void init() {
        this.graphVisualisation = new GraphVisualisation(solrSearchService,
            moduleControlRestService, moduleMetadataService, configurationRestService,
            configurationMetadataService, businessStreamMetaDataService, solrGeneralService,
            hospitalAuditService, resubmissionRestService, replayRestService, replayAuditService,
            metaDataApplicationRestService, moduleMetadataBatchInsert, triggerRestService, dynamicImagePath,
            this.dateFormatter);

        this.add(graphVisualisation);
        this.setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, String parameter) {
        if(parameter.contains(":")) {
            logger.info(String.format("Deep link event life identifier [%s]", parameter));
            this.visualisationType = parameter.substring(0, parameter.indexOf(":"));
            this.visualisationName = parameter.substring(parameter.indexOf(":") + 1);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            // Go to default view if there is no visualisation name
            if(this.visualisationName == null) {
                UI.getCurrent().navigate("");
            }
            this.init();
            this.graphVisualisation.setVisualisationName(this.visualisationName);
            this.graphVisualisation.setVisualisationType(this.visualisationType);
            this.graphVisualisation.beforeEnter(beforeEnterEvent);
            this.initialised = true;
        }
    }
}
