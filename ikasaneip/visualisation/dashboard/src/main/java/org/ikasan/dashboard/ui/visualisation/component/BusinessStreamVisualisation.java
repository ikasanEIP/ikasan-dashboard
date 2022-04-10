package org.ikasan.dashboard.ui.visualisation.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.cache.CacheStateBroadcaster;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.SearchResultsDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
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
import org.ikasan.security.service.authentication.IkasanAuthentication;
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
import org.springframework.security.core.context.SecurityContextHolder;

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

    private BusinessStreamMetaData businessStreamMetaData;

    private boolean initialised = false;

    private DateFormatter dateFormatter;

    public BusinessStreamVisualisation(ModuleControlService moduleControlRestService
        , ConfigurationService configurationRestService, TriggerService triggerRestService
        , ModuleMetaDataService moduleMetaDataService
        , ConfigurationMetaDataService configurationMetadataService
        , SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrSearchService
        , HospitalAuditService hospitalAuditService
        , ResubmissionService resubmissionRestService, ReplayService replayRestService
        , ModuleMetaDataService moduleMetadataService, BatchInsert replayAuditService
        , MetaDataService metaDataApplicationRestService, BatchInsert<ModuleMetaData> moduleMetaDataBatchInsert
        , String dynamicImagePath, DateFormatter dateFormatter) {
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
        this.dateFormatter = dateFormatter;
        if (this.dateFormatter == null) {
            throw new IllegalArgumentException("dateFormatter cannot be null!");
        }

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();
    }

    /**
     * @param businessStreamMetaData
     */
    public void createBusinessStreamGraphGraph(BusinessStreamMetaData businessStreamMetaData) throws IOException {
        this.businessStreamMetaData = businessStreamMetaData;
        this.initialised = false;
        init();
    }

    private void init() throws IOException{
        if(!initialised) {
            if (this.designerCanvas != null) {
                this.remove(designerCanvas);
            }


            this.designerCanvas = new DesignerCanvas("canvas-viewport", this.dynamicImagePath, true);
            this.designerCanvas.setCanvasJson(businessStreamMetaData.getJson());
            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);

            this.populateFlowMap(businessStreamMetaData);

            this.designerCanvas.manageClickableItems();

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

    protected Component initCanvasActions() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);
        actions.setPadding(false);
        actions.setId("canvas-actions");

        // Zoom in
        Button zoomInButton = new Button();
        zoomInButton.getElement().appendChild(IronIcons.ZOOM_IN.create().getElement());
        zoomInButton.setId("canvas_zoom_in");
        Tooltip zoomInButtonTooltip = getTooltip(zoomInButton, getTranslation("tooltip.zoom-in", UI.getCurrent().getLocale())
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        actions.add(zoomInButton, zoomInButtonTooltip);

        // Zoom out
        Button zoomOutButton = new Button();
        zoomOutButton.getElement().appendChild(IronIcons.ZOOM_OUT.create().getElement());
        zoomOutButton.setId("canvas_zoom_out");
        Tooltip zoomOutButtonTooltip = getTooltip(zoomOutButton, getTranslation("tooltip.zoom-out", UI.getCurrent().getLocale())
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        actions.add(zoomOutButton, zoomOutButtonTooltip);

        // Export as selected format
        Button download = new Button();
        download.getElement().appendChild(IronIcons.FILE_DOWNLOAD.create().getElement());
        Tooltip downloadTooltip = getTooltip(download, getTranslation("tooltip.export-png", UI.getCurrent().getLocale())
            , TooltipPosition.BOTTOM, TooltipAlignment.BOTTOM);
        actions.add(download, downloadTooltip);
        download.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.exportPng();
        });

        return actions;
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

        HashMap<String, Long> errorMap = new HashMap<>();
        HashMap<String, Long> wiretapMap = new HashMap<>();
        HashMap<String, Long> exclusionMap = new HashMap<>();
        HashMap<String, Long> replayMap = new HashMap<>();

        this.flowMap.values().forEach(flow -> {
            entityTypes.forEach(entityType -> {
                IkasanSolrDocumentSearchResults results = this.solrSearchService.search(Set.of(flow.getModuleName()), Set.of(flow.getFlowName()), searchTerm, startTime
                    , endTime, 0, Arrays.asList(entityType), false, null, null);

                if (entityType.equals("wiretap")) {
                    wiretapMap.put(flow.getId().getName(), results.getTotalNumberOfResults());
                }
                else if (entityType.equals("error")) {
                    errorMap.put(flow.getId().getName(), results.getTotalNumberOfResults());
                }
                else if (entityType.equals("exclusion")) {
                    exclusionMap.put(flow.getId().getName(), results.getTotalNumberOfResults());
                }
                else if (entityType.equals("replay")) {
                    replayMap.put(flow.getId().getName(), results.getTotalNumberOfResults());
                }
            });
        });

        this.drawFoundStatus(errorMap, wiretapMap, exclusionMap, replayMap);
    }

    public void drawFoundStatus(HashMap<String, Long> errorMap, HashMap<String, Long> wiretapMap
        , HashMap<String, Long> exclusionMap, HashMap<String, Long> replayMap) {

        stringSearchFoundStatusMap.values().forEach(status -> {
            status.setErrorFound(false);
            status.setExclusionFound(false);
            status.setWiretapFound(false);
            status.setReplayFound(false);
        });

        this.flows.forEach(flow -> {
            flow.setWiretapFoundCount(0L);
            flow.setErrorFoundCount(0L);
            flow.setExclusionFoundCount(0L);
            flow.setReplayFoundCount(0L);
        });

        this.flows = this.flows.stream().map(flow -> {
            SearchFoundStatus searchFoundStatus = this.stringSearchFoundStatusMap.get(flow.getId().getName());

            if(searchFoundStatus != null) {
                int numFound = 0;

                if (wiretapMap.get(flow.getId().getName()) != null && wiretapMap.get(flow.getId().getName()) > 0) {
                    flow.setWiretapFoundCount(wiretapMap.get(flow.getId().getName()));
                    searchFoundStatus.setWiretapFound(true);
                    numFound++;
                }

                if (errorMap.get(flow.getId().getName()) != null && errorMap.get(flow.getId().getName()) > 0) {
                    flow.setErrorFoundCount(errorMap.get(flow.getId().getName()));
                    searchFoundStatus.setErrorFound(true);
                    numFound++;
                }

                if (exclusionMap.get(flow.getId().getName()) != null && exclusionMap.get(flow.getId().getName()) > 0) {
                    flow.setExclusionFoundCount(exclusionMap.get(flow.getId().getName()));
                    searchFoundStatus.setExclusionFound(true);
                    numFound++;
                }

                if (replayMap.get(flow.getId().getName()) != null && replayMap.get(flow.getId().getName()) > 0) {
                    flow.setReplayFoundCount(replayMap.get(flow.getId().getName()));
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
        this.designerCanvas.removeFigure(flow.getErrorCountLabelIdentifier().toString());
        this.designerCanvas.removeFigure(flow.getWiretapCountLabelIdentifier().toString());
        this.designerCanvas.removeFigure(flow.getExclusionCountLabelIdentifier().toString());
        this.designerCanvas.removeFigure(flow.getReplayCountLabelIdentifier().toString());

        if(flow.getErrorFoundCount() > 0) {
            this.designerCanvas.addLabel(String.valueOf(flow.getErrorFoundCount()), xCoordinates.get(offset), flow.getY() - 70);
            this.designerCanvas.addIcon(flow.getErrorIdentifier().toString(), "frontend/images/error-service.png"
                , xCoordinates.get(offset++), flow.getY() - 45, 35, 35, false, true);
        }

        if(flow.getWiretapFoundCount() > 0) {
            this.designerCanvas.addLabel(String.valueOf(flow.getWiretapFoundCount()), xCoordinates.get(offset) , flow.getY() - 70);
            this.designerCanvas.addIcon(flow.getWiretapIdentifier().toString(),"frontend/images/wiretap-service.png"
                , xCoordinates.get(offset++), flow.getY()-45, 35, 35, false, true);
        }

        if(flow.getExclusionFoundCount() > 0) {
            this.designerCanvas.addLabel(String.valueOf(flow.getErrorFoundCount()), xCoordinates.get(offset) , flow.getY() - 70);
            this.designerCanvas.addIcon(flow.getExclusionIdentifier().toString(),"frontend/images/hospital-service.png"
                , xCoordinates.get(offset++), flow.getY()-45, 35, 35, false, true);
        }

        if(flow.getReplayFoundCount() > 0) {
            this.designerCanvas.addLabel(String.valueOf(flow.getReplayFoundCount()), xCoordinates.get(offset) , flow.getY() - 70);
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
        try {
            this.init();
        }
        catch (IOException e) {
            logger.warn("Could not initialise business stream!", e);
        }
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
        if(this.designerCanvas != null){
            this.redraw();
        }

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
            this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService, dateFormatter);
        searchResultsDialog.search(searchFoundStatus.getStartTime(), searchFoundStatus.getEndTime(), searchFoundStatus.getSearchTerm(), identifier.getType().toLowerCase(), false
            , flow.getModuleName(), flow.getFlowName());
        searchResultsDialog.open();
    }

    private void openFlowVisualisation(DesignerItemIdentifier identifier) {
        String nodeId = identifier.getName();

        if (this.flowMap.get(nodeId) != null) {
            ModuleMetaData moduleMetaData = this.moduleMetaDataService
                .findById(nodeId.substring(0, nodeId.indexOf(".")));

            logger.debug("ModuleMetaData + " + moduleMetaData);

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            Set<String> accessibleModules = SecurityUtils.getAccessibleModules(authentication);

            if(moduleMetaData == null || moduleMetaData.getFlows().stream()
                .filter(flow -> this.flowMap.get(nodeId).getFlowName().equals(flow.getName()))
                .findFirst()
                .isEmpty()) {
                ConfirmDialog dialog = new ConfirmDialog(getTranslation("confirm.header.flow-not-found", UI.getCurrent().getLocale()),
                    getTranslation("confirm.body.flow-not-found", UI.getCurrent().getLocale()), getTranslation("button.ok", UI.getCurrent().getLocale()),
                    (ComponentEventListener<ConfirmDialog.ConfirmEvent>) confirmEvent -> {});

                dialog.open();
            }
            else if(accessibleModules.contains(moduleMetaData.getName()) || authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)) {
                FlowVisualisationDialog flowVisualisationDialog
                    = new FlowVisualisationDialog(this.moduleControlRestService, this.configurationRestService,
                    this.triggerRestService, this.configurationMetadataService, moduleMetaData
                    , this.flowMap.get(nodeId), this.solrSearchService
                    , this.stringSearchFoundStatusMap.get(nodeId), this.hospitalAuditService
                    , this.resubmissionRestService, this.replayRestService, this.moduleMetadataService, this.replayAuditService
                    , this.metaDataApplicationRestService, this.moduleMetaDataBatchInsert, this.dateFormatter);

                flowVisualisationDialog.open();
            }
            else {
                ConfirmDialog dialog = new ConfirmDialog(getTranslation("confirm.header.no-flow-access", UI.getCurrent().getLocale()),
                    getTranslation("confirm.body.no-flow-access", UI.getCurrent().getLocale()), getTranslation("button.ok", UI.getCurrent().getLocale()),
                    (ComponentEventListener<ConfirmDialog.ConfirmEvent>) confirmEvent -> {});

                dialog.open();
            }
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

    public void exportPng(){
        this.designerCanvas.exportPng();
    }

    public static Tooltip getTooltip(Component component, String message, TooltipPosition position, TooltipAlignment alignment)
    {
        Tooltip tooltip = new Tooltip();

        tooltip.getElement().getStyle().set("background-color", "#232F34");
        tooltip.getElement().getStyle().set("color", "#FFFFFF");
        tooltip.getElement().getStyle().set("border-radius", "10px");
        tooltip.getElement().getStyle().set("padding", "10px");
        tooltip.getElement().getStyle().set("font-size", "8pt");
        tooltip.getElement().getStyle().set("z-index", "100");

        tooltip.attachToComponent(component);

        tooltip.setPosition(position);
        tooltip.setAlignment(alignment);

        tooltip.add(new Paragraph(message));

        return tooltip;
    }
}
