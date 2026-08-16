package org.ikasan.dashboard.ui.visualisation.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.search.listener.SearchListener;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamStatusPanel;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamVisualisation;
import org.ikasan.dashboard.ui.visualisation.event.GraphViewChangeEvent;
import org.ikasan.dashboard.ui.visualisation.event.GraphViewChangeListener;
import org.ikasan.dashboard.ui.visualisation.model.business.stream.Flow;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ConfigurationMetaDataService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.*;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GraphViewBusinessStreamVisualisation extends VerticalLayout implements SearchListener
{
    Logger logger = LoggerFactory.getLogger(GraphViewBusinessStreamVisualisation.class);

    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;

    private ModuleControlService moduleControlRestService;

    private ModuleMetaDataService moduleMetadataService;

    private ConfigurationService configurationRestService;

    private TriggerService triggerRestService;

    private ConfigurationMetaDataService configurationMetadataService;

    private BusinessStreamVisualisation businessStreamVisualisation;

    private VerticalLayout headerLayout;

    private H2 businessStreamLabel;
    private Paragraph businessStreamDescription;

    private HospitalAuditService hospitalAuditService;

    private ResubmissionService resubmissionRestService;

    private ReplayService replayRestService;

    private BatchInsert replayAuditService;

    private MetaDataService metaDataApplicationRestService;

    private BatchInsert<ModuleMetaData> moduleMetaDataBatchInsert;

    private BusinessStreamStatusPanel businessStreamStatusPanel;

    private List<GraphViewChangeListener> graphViewChangeListeners;

    private String dynamicImagePath;

    private DateFormatter dateFormatter;

    private int maxDownloadBytes;

    /**
     * Constructs a GraphViewBusinessStreamVisualisation object to provide the visualization and
     * management of business stream data.
     *
     * @param esbSearchService the service used for search communication with Solr. Must not be null.
     * @param moduleControlRestService the service for handling module control actions. Must not be null.
     * @param moduleMetadataService the service for managing module metadata. Must not be null.
     * @param configurationRestService the service for configuration management. Must not be null.
     * @param triggerRestService the service for managing triggers within the application. Must not be null.
     * @param configurationMetadataService the service for accessing configuration metadata. Must not be null.
     * @param hospitalAuditService the service for handling hospital audit data. Must not be null.
     * @param resubmissionRestService the service to handle resubmission operations. Must not be null.
     * @param replayRestService the service to manage replay operations. Must not be null.
     * @param replayAuditService the batch insert service for managing replay audits. Must not be null.
     * @param metaDataApplicationRestService the service for metadata operations within the application. Must not be null.
     * @param moduleMetaDataBatchInsert the batch insert service for module metadata. Must not be null.
     * @param dynamicImagePath the path for storing dynamic images. Must not be null.
     * @param dateFormatter the formatter for date operations. Must not be null.
     * @param maxDownloadBytes the maximum allowed bytes for download operations.
     * @throws IllegalArgumentException if any of the parameters are null.
     */
    public GraphViewBusinessStreamVisualisation(ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService
        , ModuleControlService moduleControlRestService, ModuleMetaDataService moduleMetadataService, ConfigurationService configurationRestService
        , TriggerService triggerRestService, ConfigurationMetaDataService configurationMetadataService, HospitalAuditService hospitalAuditService
        , ResubmissionService resubmissionRestService, ReplayService replayRestService, BatchInsert replayAuditService, MetaDataService metaDataApplicationRestService
        , BatchInsert<ModuleMetaData> moduleMetaDataBatchInsert, String dynamicImagePath, DateFormatter dateFormatter, int maxDownloadBytes)
    {
        this.setMargin(false);
        this.setSizeFull();

        this.esbSearchService = esbSearchService;
        if(this.esbSearchService == null){
            throw new IllegalArgumentException("solrSearchService cannot be null!");
        }
        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService == null){
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if(this.moduleMetadataService == null){
            throw new IllegalArgumentException("moduleMetadataService cannot be null!");
        }
        this.configurationRestService = configurationRestService;
        if(this.configurationRestService == null){
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.triggerRestService = triggerRestService;
        if(this.triggerRestService == null){
            throw new IllegalArgumentException("triggerRestService cannot be null!");
        }
        this.configurationMetadataService = configurationMetadataService;
        if(this.configurationMetadataService == null){
            throw new IllegalArgumentException("configurationMetadataService cannot be null!");
        }
        this.hospitalAuditService = hospitalAuditService;
        if (this.hospitalAuditService == null) {
            throw new IllegalArgumentException("hospitalAuditService cannot be null!");
        }
        this.resubmissionRestService = resubmissionRestService;
        if (this.resubmissionRestService == null) {
            throw new IllegalArgumentException("resubmissionRestService cannot be null!");
        }
        this.replayRestService = replayRestService;
        if (this.replayRestService == null) {
            throw new IllegalArgumentException("replayRestService cannot be null!");
        }
        this.replayAuditService = replayAuditService;
        if (this.replayAuditService == null) {
            throw new IllegalArgumentException("replayAuditService cannot be null!");
        }
        this.metaDataApplicationRestService = metaDataApplicationRestService;
        if (this.metaDataApplicationRestService == null) {
            throw new IllegalArgumentException("metaDataApplicationRestService cannot be null!");
        }
        this.moduleMetaDataBatchInsert = moduleMetaDataBatchInsert;
        if (this.moduleMetaDataBatchInsert == null) {
            throw new IllegalArgumentException("moduleMetaDataBatchInsert cannot be null!");
        }
        this.dynamicImagePath = dynamicImagePath;
        if (this.dynamicImagePath == null) {
            throw new IllegalArgumentException("dynamicImagePath cannot be null!");
        }
        this.dateFormatter = dateFormatter;
        if (this.dateFormatter == null) {
            throw new IllegalArgumentException("dateFormatter cannot be null!");
        }

        this.maxDownloadBytes = maxDownloadBytes;

        this.graphViewChangeListeners = new ArrayList<>();

        init();
    }

    /**
     * Initializes the component by removing specific CSS theme styles.
     *
     * This method modifies the theme configuration of the component by removing
     * the "padding" and "spacing" themes from the theme list. This adjustment
     * ensures that the component does not use these default themes, allowing for
     * custom styling or layout adjustments.
     */
    private void init() {
        this.getThemeList().remove("padding");
        this.getThemeList().remove("spacing");
    }

    /**
     * Method to perform the search.
     *
     * @param searchTerm the search term
     * @param startDate the start date/time of the search
     * @param endDate the end date/time of the search
     */
    public void search(String searchTerm, List<String> entityTypes, boolean negateQuery, long startDate, long endDate) {
        this.businessStreamVisualisation.search(entityTypes, searchTerm, startDate, endDate);
    }


    protected void createBusinessStreamGraph(String name, BusinessStreamMetaData businessStreamMetaData) throws IOException {

        if (this.businessStreamVisualisation != null) {
            this.removeAll();
        }

        this.businessStreamLabel = new H2();
        this.businessStreamLabel.getStyle().set("padding", "0px");
        this.businessStreamLabel.getStyle().set("margin", "0px");

        this.businessStreamLabel.setText(name);
        this.businessStreamDescription = new Paragraph();
        this.businessStreamDescription.getStyle().set("padding", "0px");
        this.businessStreamDescription.getStyle().set("margin-top", "20px");
        this.businessStreamDescription.setText(businessStreamMetaData.getDescription());

        this.headerLayout = new VerticalLayout();
        this.headerLayout.setWidth("100%");

        this.headerLayout.add(this.businessStreamLabel, this.businessStreamDescription);

        this.add(this.headerLayout);

        this.businessStreamVisualisation = new BusinessStreamVisualisation(this.moduleControlRestService,
            this.configurationRestService, this.triggerRestService, this.moduleMetadataService
            , this.configurationMetadataService, this.esbSearchService, this.hospitalAuditService,
            this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService,
            this.metaDataApplicationRestService, this.moduleMetaDataBatchInsert, this.dynamicImagePath, this.dateFormatter,
            this.maxDownloadBytes);

        this.businessStreamVisualisation.createBusinessStreamGraphGraph(businessStreamMetaData);

        this.add(this.businessStreamVisualisation);

        this.fireModuleFlowChangeEvent();
    }

    protected void fireModuleFlowChangeEvent() {
        GraphViewChangeEvent graphViewChangeEvent = new GraphViewChangeEvent();

        for (GraphViewChangeListener graphViewChangeListener : this.graphViewChangeListeners) {
            graphViewChangeListener.onChange(graphViewChangeEvent);
        }
    }

    public List<Flow> getFlows() {
        if(this.businessStreamVisualisation != null) {
            return this.businessStreamVisualisation.getFlows();
        }

        return null;
    }
}

