package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.cache.CacheStateBroadcaster;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.ui.general.component.SearchResultsDialog;
import org.ikasan.dashboard.ui.visualisation.adapter.service.BusinessStreamVisjsAdapter;
import org.ikasan.dashboard.ui.visualisation.component.util.SearchFoundStatus;
import org.ikasan.dashboard.ui.visualisation.model.business.stream.Flow;
import org.ikasan.dashboard.ui.visualisation.util.BusinessStreamItemTypes;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.json.DesignerJsonHelper;
import org.ikasan.designer.pallet.DesignerItemIdentifier;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.ConfigurationMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.*;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.solr.SolrGeneralService;
import org.ikasan.vaadin.visjs.network.NodeFoundStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BusinessStreamVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener, CanvasItemDoubleClickEventListener {
    private Logger logger = LoggerFactory.getLogger(BusinessStreamVisualisation.class);
    private DesignerCanvas designerCanvas;

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService;

    private Registration flowStateBroadcasterRegistration;
    private Registration cacheStateBroadcasterRegistration;

    private ModuleControlService moduleControlRestService;
    private ConfigurationService configurationRestService;
    private TriggerService triggerRestService;
    private ModuleMetaDataService moduleMetaDataService;
    private ConfigurationMetaDataService configurationMetadataService;

    private List<Flow> flows = new ArrayList<>();

    private Map<String, Flow> flowMap;
    private Map<String, SearchFoundStatus> stringSearchFoundStatusMap;

    private HospitalAuditService hospitalAuditService;

    private ResubmissionService resubmissionRestService;

    private ReplayService replayRestService;

    private ModuleMetaDataService moduleMetadataService;

    private BatchInsert replayAuditService;

    private MetaDataService metaDataApplicationRestService;

    private BatchInsert<ModuleMetaData> moduleMetaDataBatchInsert;

    private String dynamicImagePath;

    public BusinessStreamVisualisation(ModuleControlService moduleControlRestService
        , ConfigurationService configurationRestService, TriggerService triggerRestService
        , ModuleMetaDataService moduleMetaDataService
        , ConfigurationMetaDataService configurationMetadataService
        , SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService
        , HospitalAuditService hospitalAuditService
        , ResubmissionService resubmissionRestService, ReplayService replayRestService
        , ModuleMetaDataService moduleMetadataService, BatchInsert replayAuditService
        , MetaDataService metaDataApplicationRestService, BatchInsert<ModuleMetaData> moduleMetaDataBatchInsert
        , String dynamicImagePath) {
        this.moduleControlRestService = moduleControlRestService;
        if (this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }
        this.configurationRestService = configurationRestService;
        if (this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }
        this.triggerRestService = triggerRestService;
        if (this.triggerRestService == null) {
            throw new IllegalArgumentException("triggerRestService cannot be null!");
        }
        this.moduleMetaDataService = moduleMetaDataService;
        if (this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.configurationMetadataService = configurationMetadataService;
        if (this.configurationMetadataService == null) {
            throw new IllegalArgumentException("configurationMetadataService cannot be null!");
        }
        this.solrSearchService = solrSearchService;
        if (this.solrSearchService == null) {
            throw new IllegalArgumentException("solrSearchService cannot be null!");
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
        this.moduleMetadataService = moduleMetadataService;
        if (this.moduleMetadataService == null) {
            throw new IllegalArgumentException("moduleMetadataService cannot be null!");
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

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();
    }

    /**
     * @param businessStreamMetaData
     */
    public void createBusinessStreamGraphGraph(BusinessStreamMetaData businessStreamMetaData) throws IOException {
        BusinessStreamVisjsAdapter adapter = new BusinessStreamVisjsAdapter();

        if (this.designerCanvas != null) {
            this.remove(designerCanvas);
        }


        this.designerCanvas = new DesignerCanvas("canvas-viewport", this.dynamicImagePath, true);
        this.designerCanvas.setCanvasJson(businessStreamMetaData.getJson());
        this.designerCanvas.importJson();
//        this.designerCanvas.setReadonly(true);
        this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
        this.designerCanvas.addCanvasItemRightClickEventListener(this);

        this.populateFlowMap(businessStreamMetaData);

        this.designerCanvas.manageClickableItems();

        this.add(designerCanvas);
    }

    private void drawFlowStatus(FlowState state) {
        if (this.flowMap != null && flowMap.containsKey(state.getModuleName() + "." + state.getFlowName())) {
            Flow flow = flowMap.get(state.getModuleName() + "." + state.getFlowName());

            this.designerCanvas.removeFigure(flow.getStatusIdentifier());

            this.designerCanvas.addBoundary(flow.getStatusIdentifier(), flow.getId().toString(), flow.getX() - 5
                , flow.getY() - 5, flow.getHeight() + 10, flow.getWidth() + 10, state.getState().getStateColour());
        }
    }

    public void search(List<String> entityTypes, String searchTerm, long startTime, long endTime) {
        this.stringSearchFoundStatusMap.values().forEach(searchFoundStatus -> {
            searchFoundStatus.setSearchTerm(searchTerm);
            searchFoundStatus.setStartTime(startTime);
            searchFoundStatus.setEndTime(endTime);
        });

        HashMap<String, Boolean> errorMap = new HashMap<>();
        HashMap<String, Boolean> wiretapMap = new HashMap<>();
        HashMap<String, Boolean> exclusionMap = new HashMap<>();
        HashMap<String, Boolean> replayMap = new HashMap<>();

        this.flowMap.values().forEach(flow -> {
            entityTypes.forEach(entityType -> {
                IkasanSolrDocumentSearchResults results = this.solrSearchService.search(Set.of(flow.getModuleName()), Set.of(flow.getFlowName()), searchTerm, startTime
                    , endTime, 0, Arrays.asList(entityType), false, null, null);

                if (entityType.equals("wiretap")) {
                    wiretapMap.put(flow.getId().getName(), results.getTotalNumberOfResults() > 0);
                }
                else if (entityType.equals("error")) {
                    errorMap.put(flow.getId().getName(), results.getTotalNumberOfResults() > 0);
                }
                else if (entityType.equals("exclusion")) {
                    exclusionMap.put(flow.getId().getName(), results.getTotalNumberOfResults() > 0);
                }
                else if (entityType.equals("replay")) {
                    replayMap.put(flow.getId().getName(), results.getTotalNumberOfResults() > 0);
                }
            });
        });

        this.drawFoundStatus(errorMap, wiretapMap, exclusionMap, replayMap);
    }

    public void drawFoundStatus(HashMap<String, Boolean> errorMap, HashMap<String, Boolean> wiretapMap
        , HashMap<String, Boolean> exclusionMap, HashMap<String, Boolean> replayMap) {

        stringSearchFoundStatusMap.values().forEach(status -> {
            status.setErrorFound(false);
            status.setExclusionFound(false);
            status.setWiretapFound(false);
            status.setReplayFound(false);
        });

        this.flows.forEach(flow -> {
            flow.setWiretapFoundStatus(NodeFoundStatus.NOT_FOUND);
            flow.setErrorFoundStatus(NodeFoundStatus.NOT_FOUND);
            flow.setExclusionFoundStatus(NodeFoundStatus.NOT_FOUND);
            flow.setReplayFoundStatus(NodeFoundStatus.NOT_FOUND);
        });

        this.flows = this.flows.stream().map(flow -> {
            SearchFoundStatus searchFoundStatus = this.stringSearchFoundStatusMap.get(flow.getId().getName());

            if(searchFoundStatus != null) {
                int numFound = 0;

                if (wiretapMap.get(flow.getId().getName()) != null && wiretapMap.get(flow.getId().getName())) {
                    flow.setWiretapFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setWiretapFound(true);
                    numFound++;
                }

                if (errorMap.get(flow.getId().getName()) != null && errorMap.get(flow.getId().getName())) {
                    flow.setErrorFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setErrorFound(true);
                    numFound++;
                }

                if (exclusionMap.get(flow.getId().getName()) != null && exclusionMap.get(flow.getId().getName())) {
                    flow.setExclusionFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setExclusionFound(true);
                    numFound++;
                }

                if (replayMap.get(flow.getId().getName()) != null && replayMap.get(flow.getId().getName())) {
                    flow.setReplayFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setReplayFound(true);
                    numFound++;
                }

                this.addSearchFoundIconToFlow(flow, this.getSearchIconCoordinates(flow, numFound));

                this.stringSearchFoundStatusMap.put(flow.getModuleName() + flow.getFlowName()
                    , searchFoundStatus);
            }

            return flow;
        }).collect(Collectors.toList());
    }

    private void addSearchFoundIconToFlow(Flow flow, List<Double> xCoordinates) {
        int offset = 0;

        this.designerCanvas.removeFigure(flow.getErrorIdentifier().toString());
        this.designerCanvas.removeFigure(flow.getWiretapIdentifier().toString());
        this.designerCanvas.removeFigure(flow.getExclusionIdentifier().toString());
        this.designerCanvas.removeFigure(flow.getReplayIdentifier().toString());

        if(flow.getErrorFoundStatus().equals(NodeFoundStatus.FOUND)) {
            this.designerCanvas.addIcon(flow.getErrorIdentifier().toString(), "frontend/images/error-service.png"
                , xCoordinates.get(offset++), flow.getY() - 45, 35, 35, false, true);
        }

        if(flow.getWiretapFoundStatus().equals(NodeFoundStatus.FOUND)) {
            this.designerCanvas.addIcon(flow.getWiretapIdentifier().toString(),"frontend/images/wiretap-service.png"
                , xCoordinates.get(offset++), flow.getY()-45, 35, 35, false, true);
        }

        if(flow.getExclusionFoundStatus().equals(NodeFoundStatus.FOUND)) {
            this.designerCanvas.addIcon(flow.getExclusionIdentifier().toString(),"frontend/images/hospital-service.png"
                , xCoordinates.get(offset++), flow.getY()-45, 35, 35, false, true);
        }

        if(flow.getReplayFoundStatus().equals(NodeFoundStatus.FOUND)) {
            this.designerCanvas.addIcon(flow.getReplayIdentifier().toString(),"frontend/images/replay-service.png"
                , xCoordinates.get(offset++), flow.getY()-45, 35, 35, false, true);
        }
    }

    private List<Double> getSearchIconCoordinates(Flow flow, int numFound) {
        int centreX = flow.getX() + (flow.getWidth() / 2);

        ArrayList<Double> xCoordinates = new ArrayList();
        if(numFound == 1){
            xCoordinates.add(centreX-17.5);
            xCoordinates.add(0d);
            xCoordinates.add(0d);
            xCoordinates.add(0d);
        }
        else if(numFound == 2){
            xCoordinates.add(centreX-40d);
            xCoordinates.add(centreX+5d);
            xCoordinates.add(0d);
            xCoordinates.add(0d);
        }
        else if(numFound == 3){
            xCoordinates.add(centreX-62.5);
            xCoordinates.add(centreX-17.5);
            xCoordinates.add(centreX+27.5);
            xCoordinates.add(0d);
        }
        else if(numFound == 4){
            xCoordinates.add(centreX-84d);
            xCoordinates.add(centreX-40d);
            xCoordinates.add(centreX+5d);
            xCoordinates.add(centreX+50d);
        }

        return xCoordinates;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.redraw();
    }

    public void redraw() {
        for (String key : this.flowMap.keySet()) {
            if (key.contains(".")) {
                ModuleMetaData module = this.moduleMetaDataService
                    .findById(key.substring(0, key.indexOf(".")));

                if (module != null) {
                    FlowState flowState = FlowStateCache.instance().get(module, key.substring(key.indexOf(".") + 1));

                    if (flowState != null) {
                        this.drawFlowStatus(flowState);
                    }
                }
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.redraw();
        UI ui = attachEvent.getUI();
        flowStateBroadcasterRegistration = FlowStateBroadcaster.register(flowState ->
        {
            logger.debug("Received flow state: " + flowState);
            this.drawFlowStatus(ui, flowState);
        });

        cacheStateBroadcasterRegistration = CacheStateBroadcaster.register(flowState ->
        {
            logger.debug("Received flow state: " + flowState);
            this.drawFlowStatus(ui, flowState);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.flowStateBroadcasterRegistration.remove();
        this.flowStateBroadcasterRegistration = null;
        this.cacheStateBroadcasterRegistration.remove();
        this.cacheStateBroadcasterRegistration = null;
    }

    protected void drawFlowStatus(UI ui, FlowState flowState) {
        ui.access(() ->
        {
            if (this.flowMap != null && this.flowMap.containsKey(flowState.getModuleName() + "." + flowState.getFlowName())) {
                this.drawFlowStatus(flowState);
            }
        });
    }

    private void populateFlowMap(BusinessStreamMetaData businessStreamMetaData) {
        List<JSONObject> flows = DesignerJsonHelper.getIdentifierTypeItems(BusinessStreamItemTypes.FLOW.name(),
            businessStreamMetaData.getJson());

        this.flowMap = new HashMap<>();
        this.stringSearchFoundStatusMap = new HashMap<>();

        flows.forEach(flowJson -> {
            DesignerItemIdentifier identifier
                = DesignerItemIdentifier.getIdentifier(flowJson.getString("id"));
            Flow flow = new Flow(identifier.toString(), identifier.getName().substring(0, identifier.getName().indexOf(".")),
                identifier.getName().substring(identifier.getName().indexOf(".") + 1),
                flowJson.getNumber("x").intValue(), flowJson.getNumber("y").intValue(),
                flowJson.getNumber("width").intValue(), flowJson.getNumber("height").intValue());

            this.flowMap.put(flow.getId().getName(), flow);
            this.flows.add(flow);
            this.stringSearchFoundStatusMap
                .put(flow.getId().getName(), new SearchFoundStatus());
        });

    }

    public List<Flow> getFlows() {
        return this.flows;
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {

        DesignerItemIdentifier identifier;

        try {
             identifier = DesignerItemIdentifier
                .getIdentifier(canvasItemDoubleClickEvent.getFigure().getIdentifier());
        }
        catch (IllegalArgumentException e){
            // we ignore any events that we cannot parse the identifier for.
            return;
        }

        if(identifier.getType().equals(BusinessStreamItemTypes.FLOW.name())) {
            this.openFlowVisualisation(identifier);
        }
        else if(identifier.getType().equals(BusinessStreamItemTypes.ERROR.name()) ||
            identifier.getType().equals(BusinessStreamItemTypes.EXCLUSION.name()) ||
            identifier.getType().equals(BusinessStreamItemTypes.WIRETAP.name()) ||
            identifier.getType().equals(BusinessStreamItemTypes.REPLAY.name())) {
            this.openSearchResultsDialog(identifier);
        }
    }

    private void openSearchResultsDialog(DesignerItemIdentifier identifier) {
        SearchFoundStatus searchFoundStatus = this.stringSearchFoundStatusMap.get(identifier.getName());

        Flow flow =  this.flowMap.get(identifier.getName());
        logger.debug("error clicked: " + flow.getModuleName() + " " + flow.getFlowName());
        SearchResultsDialog searchResultsDialog = new SearchResultsDialog(this.solrSearchService, this.hospitalAuditService,
            this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService);
        searchResultsDialog.search(searchFoundStatus.getStartTime(), searchFoundStatus.getEndTime(), searchFoundStatus.getSearchTerm(), identifier.getType().toLowerCase(), false
            , flow.getModuleName(), flow.getFlowName());
        searchResultsDialog.open();
    }

    private void openFlowVisualisation(DesignerItemIdentifier identifier) {
        String nodeId = identifier.getName();

        logger.debug(nodeId);
        logger.debug("Flow + " + this.flowMap.get(nodeId));

        if (this.flowMap.get(nodeId) != null) {
            ModuleMetaData moduleMetaData = this.moduleMetaDataService
                .findById(nodeId.substring(0, nodeId.indexOf(".")));

            logger.debug("ModuleMetaData + " + moduleMetaData);

            FlowVisualisationDialog flowVisualisationDialog
                = new FlowVisualisationDialog(this.moduleControlRestService, this.configurationRestService,
                this.triggerRestService, this.configurationMetadataService, moduleMetaData
                , this.flowMap.get(nodeId), this.solrSearchService
                , this.stringSearchFoundStatusMap.get(nodeId), this.hospitalAuditService
                , this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService
                , this.metaDataApplicationRestService, this.moduleMetaDataBatchInsert);

            flowVisualisationDialog.open();
        }
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {
        if(canvasItemRightClickEvent.getFigure().getType().equals(BusinessStreamItemTypes.FLOW.name())) {
            ModuleControlContextMenu moduleControlContextMenu = new ModuleControlContextMenu(canvasItemRightClickEvent.getClickLocationX(),
                canvasItemRightClickEvent.getClickLocationY());

            moduleControlContextMenu.open();

        }
    }
}
