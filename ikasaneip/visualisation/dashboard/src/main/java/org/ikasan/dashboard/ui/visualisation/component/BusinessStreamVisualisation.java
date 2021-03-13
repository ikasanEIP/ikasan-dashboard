package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.cache.CacheStateBroadcaster;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.ui.general.component.SearchResultsDialog;
import org.ikasan.dashboard.ui.visualisation.adapter.service.BusinessStreamVisjsAdapter;
import org.ikasan.dashboard.ui.visualisation.component.util.SearchFoundStatus;
import org.ikasan.dashboard.ui.visualisation.model.business.stream.BusinessStream;
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
import org.ikasan.vaadin.visjs.network.Edge;
import org.ikasan.vaadin.visjs.network.NetworkDiagram;
import org.ikasan.vaadin.visjs.network.Node;
import org.ikasan.vaadin.visjs.network.NodeFoundStatus;
import org.ikasan.vaadin.visjs.network.options.Interaction;
import org.ikasan.vaadin.visjs.network.options.Options;
import org.ikasan.vaadin.visjs.network.options.edges.ArrowHead;
import org.ikasan.vaadin.visjs.network.options.edges.Arrows;
import org.ikasan.vaadin.visjs.network.options.edges.EdgeColor;
import org.ikasan.vaadin.visjs.network.options.edges.Edges;
import org.ikasan.vaadin.visjs.network.options.physics.Physics;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class BusinessStreamVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener, CanvasItemDoubleClickEventListener {
    private Logger logger = LoggerFactory.getLogger(BusinessStreamVisualisation.class);
    private NetworkDiagram networkDiagram;
    private DesignerCanvas designerCanvas;

    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService;

    private Registration flowStateBroadcasterRegistration;
    private Registration cacheStateBroadcasterRegistration;

    private ModuleControlService moduleControlRestService;
    private ConfigurationService configurationRestService;
    private TriggerService triggerRestService;
    private ModuleMetaDataService moduleMetaDataService;
    private ConfigurationMetaDataService configurationMetadataService;

    private BusinessStream businessStream;
    private ArrayList<Node> nodes = new ArrayList<>();
    private ArrayList<Flow> flows = new ArrayList<>();

    private Map<String, Flow> flowMap;
    private Map<String, SearchFoundStatus> stringSearchFoundStatusMap;

    private UI current;

    private HospitalAuditService hospitalAuditService;

    private ResubmissionService resubmissionRestService;

    private ReplayService replayRestService;

    private ModuleMetaDataService moduleMetadataService;

    private BatchInsert replayAuditService;

    private MetaDataService metaDataApplicationRestService;

    private BatchInsert<ModuleMetaData> moduleMetaDataBatchInsert;

    public BusinessStreamVisualisation(ModuleControlService moduleControlRestService
        , ConfigurationService configurationRestService, TriggerService triggerRestService
        , ModuleMetaDataService moduleMetaDataService
        , ConfigurationMetaDataService configurationMetadataService
        , SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService
        , HospitalAuditService hospitalAuditService
        , ResubmissionService resubmissionRestService, ReplayService replayRestService
        , ModuleMetaDataService moduleMetadataService, BatchInsert replayAuditService
        , MetaDataService metaDataApplicationRestService, BatchInsert<ModuleMetaData> moduleMetaDataBatchInsert) {
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

        current = UI.getCurrent();

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();
    }

    /**
     * @param businessStreamMetaData
     */
    public void createBusinessStreamGraphGraph(BusinessStreamMetaData businessStreamMetaData) throws IOException {
        BusinessStreamVisjsAdapter adapter = new BusinessStreamVisjsAdapter();

//        this.businessStream = adapter.toBusinessStreamGraph(businessStreamMetaData);
//
//        nodes = new ArrayList<>();
//        nodes.addAll(businessStream.getFlows());
//        nodes.addAll(businessStream.getDestinations());
//        nodes.addAll(businessStream.getIntegratedSystems());
//
//        flows = new ArrayList<>();
//        flows.addAll(businessStream.getFlows());

        if (this.designerCanvas != null) {
            this.remove(designerCanvas);
        }

//        this.populateFlowMap(businessStream.getFlows());

//        updateNetworkDiagram(nodes, businessStream.getEdges());

        this.designerCanvas = new DesignerCanvas("canvas-viewport");
        this.designerCanvas.setCanvasJson(businessStreamMetaData.getJson());
        this.designerCanvas.importJson();
        this.designerCanvas.manageClickableItems();
        this.designerCanvas.setReadonly(true);
        this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
        this.designerCanvas.addCanvasItemRightClickEventListener(this);

        this.populateFlowMap(businessStreamMetaData);

        this.add(designerCanvas);
    }

//    /**
//     * Method to update the network diagram with the node and edge lists.
//     *
//     * @param nodes a list containing all network nodes.
//     * @param edges a list containing all the network edges.
//     */
//    protected void updateNetworkDiagram(List<Node> nodes, List<Edge> edges) {
//        Physics physics = new Physics();
//        physics.setEnabled(false);
//
//        networkDiagram = new NetworkDiagram
//            (Options.builder()
//                .withAutoResize(false)
//                .withPhysics(physics)
//                .withInteraction(Interaction.builder().withDragNodes(false).build())
//                .withEdges(
//                    Edges.builder()
//                        .withArrows(new Arrows(new ArrowHead()))
//                        .withColor(EdgeColor.builder()
//                            .withColor("#000000")
//                            .build())
//                        .withDashes(false)
//                        .build())
//                .build());
//
//        networkDiagram.setSizeFull();
//
//        networkDiagram.setNodes(nodes);
//        networkDiagram.setEdges(edges);
//
//        networkDiagram.addDoubleClickListener(doubleClickEvent ->
//        {
//            logger.debug(doubleClickEvent.getParams().toString());
//
//            JsonArray nodesArray = doubleClickEvent.getParams().getArray("nodes");
//
//            if (nodesArray.length() > 0) {
//                String nodeId = nodesArray.get(0).asString();
//
//                logger.debug(nodeId);
//                logger.debug("Flow + " + this.flowMap.get(nodeId));
//
//                if (this.flowMap.get(nodeId) != null) {
//                    ModuleMetaData moduleMetaData = this.moduleMetaDataService
//                        .findById(nodeId.substring(0, nodeId.indexOf(".")));
//
//                    logger.debug("ModuleMetaData + " + moduleMetaData);
//
//                    FlowVisualisationDialog flowVisualisationDialog
//                        = new FlowVisualisationDialog(this.moduleControlRestService, this.configurationRestService,
//                        this.triggerRestService, this.configurationMetadataService, moduleMetaData
//                        , this.flowMap.get(nodeId), this.solrSearchService
//                        , this.stringSearchFoundStatusMap.get(nodeId), this.hospitalAuditService
//                        , this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService
//                        , this.metaDataApplicationRestService, this.moduleMetaDataBatchInsert);
//
//                    flowVisualisationDialog.open();
//                }
//            } else {
//                JsonObject coordinates = doubleClickEvent.getParams().getObject("pointer").getObject("canvas");
//
//                this.flows.forEach(flow -> {
//                    SearchFoundStatus searchFoundStatus = this.stringSearchFoundStatusMap.get(flow.getId());
//                    if (flow.wiretapClickedOn(coordinates.getNumber("x"), coordinates.getNumber("y"))) {
//                        logger.debug("wiretap clicked: " + flow.getModuleName() + " " + flow.getFlowName());
//                        SearchResultsDialog searchResultsDialog = new SearchResultsDialog(this.solrSearchService, this.hospitalAuditService,
//                            this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService);
//                        searchResultsDialog.search(searchFoundStatus.getStartTime(), searchFoundStatus.getEndTime(), searchFoundStatus.getSearchTerm(), "wiretap", false
//                            , flow.getModuleName(), flow.getFlowName());
//                        searchResultsDialog.open();
//                    }
//                    if (flow.errorClickedOn(coordinates.getNumber("x"), coordinates.getNumber("y"))) {
//                        logger.debug("error clicked: " + flow.getModuleName() + " " + flow.getFlowName());
//                        SearchResultsDialog searchResultsDialog = new SearchResultsDialog(this.solrSearchService, this.hospitalAuditService,
//                            this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService);
//                        searchResultsDialog.search(searchFoundStatus.getStartTime(), searchFoundStatus.getEndTime(), searchFoundStatus.getSearchTerm(), "error", false
//                            , flow.getModuleName(), flow.getFlowName());
//                        searchResultsDialog.open();
//                    }
//                    if (flow.exclusionClickedOn(coordinates.getNumber("x"), coordinates.getNumber("y"))) {
//                        logger.debug("exclusion clicked: " + flow.getModuleName() + " " + flow.getFlowName());
//                        SearchResultsDialog searchResultsDialog = new SearchResultsDialog(this.solrSearchService, this.hospitalAuditService,
//                            this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService);
//                        searchResultsDialog.search(searchFoundStatus.getStartTime(), searchFoundStatus.getEndTime(), searchFoundStatus.getSearchTerm(), "exclusion", false
//                            , flow.getModuleName(), flow.getFlowName());
//                        searchResultsDialog.open();
//                    }
//                    if (flow.replayClickedOn(coordinates.getNumber("x"), coordinates.getNumber("y"))) {
//                        logger.debug("replay clicked: " + flow.getModuleName() + " " + flow.getFlowName());
//                        SearchResultsDialog searchResultsDialog = new SearchResultsDialog(this.solrSearchService, this.hospitalAuditService,
//                            this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService);
//                        searchResultsDialog.search(searchFoundStatus.getStartTime(), searchFoundStatus.getEndTime(), searchFoundStatus.getSearchTerm(), "replay", false
//                            , flow.getModuleName(), flow.getFlowName());
//                        searchResultsDialog.open();
//                    }
//                });
//            }
//        });
//
//
//    }

    private void drawFlowStatus(FlowState state) {
        if (this.flowMap != null && flowMap.containsKey(state.getModuleName() + "." + state.getFlowName())) {
            Flow flow = flowMap.get(state.getModuleName() + "." + state.getFlowName());

            this.designerCanvas.addBoundary(flow.getX() - 5
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
                    wiretapMap.put(flow.getId(), results.getTotalNumberOfResults() > 0);
                }
                else if (entityType.equals("error")) {
                    errorMap.put(flow.getId(), results.getTotalNumberOfResults() > 0);
                }
                else if (entityType.equals("exclusion")) {
                    exclusionMap.put(flow.getId(), results.getTotalNumberOfResults() > 0);
                }
                else if (entityType.equals("replay")) {
                    replayMap.put(flow.getId(), results.getTotalNumberOfResults() > 0);
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

        this.flows = (ArrayList<Flow>) this.flows.stream().map(flow -> {
            SearchFoundStatus searchFoundStatus = this.stringSearchFoundStatusMap.get(flow.getId());

            int centreX = flow.getX() + (flow.getWidth() / 2);
            if(searchFoundStatus != null) {
                int numFound = 0;

                if (wiretapMap.get(flow.getId()) != null && wiretapMap.get(flow.getId())) {
                    flow.setWiretapFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setWiretapFound(true);

                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.WIRETAP.name(), flow.getId(),
                            UUID.randomUUID().toString()).toString(),"frontend/images/wiretap-service.png"
                        , flow.getX()-20, flow.getY()-20, 35, 35, false, true);

                    numFound++;
                }

                if (errorMap.get(flow.getId()) != null && errorMap.get(flow.getId())) {
                    flow.setErrorFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setErrorFound(true);
//                    4
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.ERROR.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/error-service.png"
//                        , centreX-85, flow.getY()-45, 35, 35, false, true);
//
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.WIRETAP.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/wiretap-service.png"
//                        , centreX-40, flow.getY()-45, 35, 35, false, true);
//
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.EXCLUSION.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/hospital-service.png"
//                        , centreX+5, flow.getY()-45, 35, 35, false, true);
//
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.REPLAY.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/replay-service.png"
//                        , centreX+50, flow.getY()-45, 35, 35, false, true);


////                  3
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.WIRETAP.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/wiretap-service.png"
//                        , centreX-62.5, flow.getY()-45, 35, 35, false, true);
//
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.EXCLUSION.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/hospital-service.png"
//                        , centreX-17.5, flow.getY()-45, 35, 35, false, true);
//
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.REPLAY.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/replay-service.png"
//                        , centreX+27.5, flow.getY()-45, 35, 35, false, true);

//                  2
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.WIRETAP.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/wiretap-service.png"
//                        , centreX-40, flow.getY()-45, 35, 35, false, true);
//
//                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.EXCLUSION.name(), flow.getId(),
//                            UUID.randomUUID().toString()).toString(),"frontend/images/hospital-service.png"
//                        , centreX+5, flow.getY()-45, 35, 35, false, true);

                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.EXCLUSION.name(), flow.getId(),
                            UUID.randomUUID().toString()).toString(),"frontend/images/hospital-service.png"
                        , centreX-17.5, flow.getY()-45, 35, 35, false, true);

                    numFound++;
                }

                if (exclusionMap.get(flow.getId()) != null && exclusionMap.get(flow.getId())) {
                    flow.setExclusionFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setExclusionFound(true);

                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.EXCLUSION.name(), flow.getId(),
                            UUID.randomUUID().toString()).toString(),"frontend/images/hospital-service.png"
                        , flow.getX()-20, flow.getY()-20, 35, 35, false, true);

                    numFound++;
                }

                if (replayMap.get(flow.getId()) != null && replayMap.get(flow.getId())) {
                    flow.setReplayFoundStatus(NodeFoundStatus.FOUND);
                    searchFoundStatus.setReplayFound(true);

                    this.designerCanvas.addIcon(new DesignerItemIdentifier(BusinessStreamItemTypes.REPLAY.name(), flow.getId(),
                            UUID.randomUUID().toString()).toString(),"frontend/images/replay-service.png"
                        , flow.getX()-20, flow.getY()-20, 35, 35, false, true);

                    numFound++;
                }



                this.stringSearchFoundStatusMap.put(flow.getModuleName() + flow.getFlowName()
                    , searchFoundStatus);
            }

            return flow;
        }).collect(Collectors.toList());
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

//        if (this.businessStream != null) {
//            nodes = new ArrayList<>();
//            nodes.addAll(businessStream.getFlows());
//            nodes.addAll(businessStream.getDestinations());
//            nodes.addAll(businessStream.getIntegratedSystems());
//
//            if (this.networkDiagram != null) {
//                this.remove(networkDiagram);
//            }
//
////            this.populateFlowMap(businessStream.getFlows());
//
//            updateNetworkDiagram(nodes, businessStream.getEdges());
//
////            businessStream.getBoundaries()
////                .forEach(boundary -> this.networkDiagram.drawBoundary(boundary.getX(),
////                    boundary.getY(), boundary.getW(), boundary.getH(), boundary.getLabel(), boundary.getColour()));
////
////            this.networkDiagram.scale(0.7);
//
//            for (String key : this.flowMap.keySet()) {
//                if (key.contains(".")) {
//                    ModuleMetaData module = this.moduleMetaDataService
//                        .findById(key.substring(0, key.indexOf(".")));
//
//                    if (module != null) {
//                        FlowState flowState = FlowStateCache.instance().get(module, key.substring(key.indexOf(".") + 1));
//
//                        if (flowState != null) {
//                            this.drawFlowStatus(flowState);
//                        }
//                    }
//                }
//            }
//
//            this.add(networkDiagram);
//        }
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
            Flow flow = new Flow(identifier.getName(), identifier.getName().substring(0, identifier.getName().indexOf(".")),
                identifier.getName().substring(identifier.getName().indexOf(".") + 1),
                flowJson.getNumber("x").intValue(), flowJson.getNumber("y").intValue(),
                flowJson.getNumber("width").intValue(), flowJson.getNumber("height").intValue());

            this.flowMap.put(flow.getId(), flow);
            this.flows.add(flow);
            this.stringSearchFoundStatusMap
                .put(flow.getId(), new SearchFoundStatus());
        });

    }

    private void populateFlowMap(List<Flow> flows) {
        this.flowMap = flows
            .stream()
            .collect(toMap(Flow::getId, Function.identity()));

        this.stringSearchFoundStatusMap = new HashMap<>();

        flows.forEach(flow -> this.stringSearchFoundStatusMap
            .put(flow.getId(), new SearchFoundStatus()));
    }

    public BusinessStream getBusinessStream() {
        return this.businessStream;
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {

        DesignerItemIdentifier identifier = null;

        try {
             identifier = DesignerItemIdentifier
                .getIdentifier(canvasItemDoubleClickEvent.getFigure().getIdentifier());
        }
        catch (IllegalArgumentException e){
            // we ignore any events that we cannot parse the identifier for.
            return;
        }

        if(identifier.getType().equals(BusinessStreamItemTypes.FLOW.name())) {
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
        else if(identifier.getType().equals(BusinessStreamItemTypes.ERROR.name())) {
            SearchFoundStatus searchFoundStatus = this.stringSearchFoundStatusMap.get(identifier.getName());

            Flow flow =  this.flowMap.get(identifier.getName());
            logger.debug("error clicked: " + flow.getModuleName() + " " + flow.getFlowName());
            SearchResultsDialog searchResultsDialog = new SearchResultsDialog(this.solrSearchService, this.hospitalAuditService,
                this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService);
            searchResultsDialog.search(searchFoundStatus.getStartTime(), searchFoundStatus.getEndTime(), searchFoundStatus.getSearchTerm(), "error", false
                , flow.getModuleName(), flow.getFlowName());
            searchResultsDialog.open();
        }
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {

    }
}
