package org.ikasan.dashboard.ui.visualisation.view;


import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.rest.client.ReplayRestServiceImpl;
import org.ikasan.rest.client.ResubmissionRestServiceImpl;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;


@Route(value = "visualisationTab")
@UIScope
@Component
@PermitAll
@PreserveOnRefresh
public class GraphVisualisationDeepLinkView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(GraphVisualisationDeepLinkView.class);

    private GraphVisualisation graphVisualisation;

    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;
    private ModuleControlService moduleControlRestService;
    private ModuleMetaDataService moduleMetadataService;
    private ConfigurationService configurationRestService;
    private TriggerService triggerRestService;
    private ConfigurationMetaDataService configurationMetadataService;
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private HospitalAuditService hospitalAuditService;
    private ResubmissionRestServiceImpl resubmissionRestService;
    private ReplayRestServiceImpl replayRestService;
    private BatchInsert replayAuditService;
    private MetaDataService metaDataApplicationRestService;
    private BatchInsert<ModuleMetaData> moduleMetadataBatchInsert;

    @Value(value = "${integrated.systems.image.path}")
    private String dynamicImagePath;

    @Value("${max.download.bytes:50000000}")
    private int maxDownloadBytes;

    private boolean initialised = false;

    private String visualisationType = null;
    private String visualisationName = null;

    private DateFormatter dateFormatter;

    public GraphVisualisationDeepLinkView(@Qualifier("esbSearchService") ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService, ModuleControlService moduleControlRestService,
                                          @Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService, ConfigurationService configurationRestService,
                                          @Qualifier("configurationMetadataService") ConfigurationMetaDataService configurationMetadataService,
                                          BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
                                          @Qualifier("hospitalAuditService") HospitalAuditService hospitalAuditService,
                                          ResubmissionRestServiceImpl resubmissionRestService,
                                          ReplayRestServiceImpl replayRestService,
                                          @Qualifier("replayEventBatchInsert") BatchInsert replayAuditService,
                                          MetaDataService metaDataApplicationRestService,
                                          @Qualifier("moduleMetadataBatchInsert") BatchInsert<ModuleMetaData> moduleMetadataBatchInsert,
                                          TriggerService triggerRestService,
                                          DateFormatter dateFormatter)
    {
        this.esbSearchService = esbSearchService;
        this.moduleControlRestService = moduleControlRestService;
        this.moduleMetadataService = moduleMetadataService;
        this.configurationRestService = configurationRestService;
        this.configurationMetadataService = configurationMetadataService;
        this.businessStreamMetaDataService = businessStreamMetaDataService;
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
        this.graphVisualisation = new GraphVisualisation(esbSearchService,
            moduleControlRestService, moduleMetadataService, configurationRestService,
            configurationMetadataService, businessStreamMetaDataService,
            hospitalAuditService, resubmissionRestService, replayRestService, replayAuditService,
            metaDataApplicationRestService, moduleMetadataBatchInsert, triggerRestService, dynamicImagePath,
            this.dateFormatter, this.maxDownloadBytes);

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
                DashboardContextNavigator.navigateToLandingPage();
            }
            this.init();
            this.graphVisualisation.setVisualisationName(this.visualisationName);
            this.graphVisualisation.setVisualisationType(this.visualisationType);
            this.graphVisualisation.beforeEnter(beforeEnterEvent);
            this.initialised = true;
        }
    }
}
