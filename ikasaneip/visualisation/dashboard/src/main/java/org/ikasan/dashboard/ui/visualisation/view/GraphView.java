package org.ikasan.dashboard.ui.visualisation.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Route(value = "visualisation", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Visualisation")
@Component
public class GraphView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(GraphView.class);

    @Resource
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService;

    @Resource
    private ModuleControlService moduleControlRestService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Autowired
    private ConfigurationService configurationRestService;

    @Autowired
    private TriggerService triggerRestService;

    @Resource
    private ConfigurationMetaDataService configurationMetadataService;

    @Resource
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    @Resource
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    @Resource
    private HospitalAuditService hospitalAuditService;

    @Resource
    private ResubmissionRestServiceImpl resubmissionRestService;

    @Resource
    private ReplayRestServiceImpl replayRestService;

    @Resource
    private BatchInsert replayAuditService;

    @Resource
    private MetaDataService metaDataApplicationRestService;

    @Resource
    private BatchInsert<ModuleMetaData> moduleMetadataBatchInsert;

    @Value(value = "${integrated.systems.image.path}")
    private String dynamicImagePath;

    private Registration broadcasterRegistration;

    private GraphVisualisation graphVisualisation;

    private boolean initialised = false;

    /**
     * Constructor
     */
    public GraphView()
    {
        this.setMargin(false);

        this.setWidth("100%");
        this.setHeight("88vh");
    }

    private void init() {
        this.graphVisualisation = new GraphVisualisation(this.solrSearchService,
            this.moduleControlRestService, this.moduleMetadataService, this.configurationRestService,
            this.configurationMetadataService, this.businessStreamMetaDataService, this.solrGeneralService,
            this.hospitalAuditService, this.resubmissionRestService, this.replayRestService, this.replayAuditService,
            this.metaDataApplicationRestService, this.moduleMetadataBatchInsert, this.triggerRestService, this.dynamicImagePath);

        this.add(graphVisualisation);

        this.initialised = true;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!this.initialised) {
            this.init();
            this.graphVisualisation.beforeEnter(beforeEnterEvent);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        UI ui = attachEvent.getUI();

        broadcasterRegistration = FlowStateBroadcaster.register(flowState ->
        {
            ui.access(() ->
            {
                // do something interesting here.
                logger.debug("Received flow state: " + flowState);
            });
        });

    }

    @Override
    protected void onDetach(DetachEvent detachEvent)
    {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }
}

