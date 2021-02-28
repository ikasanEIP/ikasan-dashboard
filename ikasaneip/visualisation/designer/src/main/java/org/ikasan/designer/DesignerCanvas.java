package org.ikasan.designer;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableConsumer;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.model.Container;
import org.ikasan.designer.model.Figure;
import org.ikasan.designer.pallet.DesignerPalletImageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Wraps a visjs network diagram. See http://visjs.org/network_examples.html
 */
@SuppressWarnings("serial")
@Tag("div")
@StyleSheet("./org/ikasan/draw2d/designer.css")
public class DesignerCanvas extends VerticalLayout implements HasSize {

    Logger logger = LoggerFactory.getLogger(DesignerCanvas.class);

    private String canvasJson;

    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, DesignerPalletImageItem> designerPalletItemMap = new HashMap<>();
    private List<CanvasItemRightClickEventListener> canvasItemRightClickEventListeners
        = new ArrayList<>();
    private List<CanvasItemDoubleClickEventListener> canvasItemDoubleClickEventListeners
        = new ArrayList<>();

    ScheduledExecutorService executorService
        = Executors.newSingleThreadScheduledExecutor();

    public DesignerCanvas() {
        super();
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/jquery.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/jquery-ui.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/draw2d.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/designer-connector-flow.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/mousetrap.min.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/view.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/RotateRectangleFeedbackSelectionPolicy.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/RotateHandle.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/Triangle.js");

        // Dont transfer empty options.
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        // Dont transfer getter and setter
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withGetterVisibility(Visibility.NONE).withSetterVisibility(Visibility.NONE)
            .withIsGetterVisibility(Visibility.NONE).withFieldVisibility(Visibility.ANY));
        // remains utf8 escaped chars
        mapper.configure(Feature.ESCAPE_NON_ASCII, true);

        this.setId("canvas-wrapper");

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setTarget(this);

        getElement().addEventListener("vaadin-context-menu-before-open", e -> {
            contextMenu.setVisible(false);
            populateContextMenu();
        });
    }

    private void initConnector() {
        getUI()
            .orElseThrow(() -> new IllegalStateException(
                "Connector can only be initialized for an attached Designer"))
            .getPage()
            .executeJs("window.Vaadin.Flow.designerConnector.initLazy($0)",
                getElement());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initConnector();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }

    public void addIcon(String identifier, String image, double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addIcon", identifier, image, h, w));
    }

    public void addBoundary(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addBoundary", h, w));
    }

    public void addTriangleBoundary(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addTriangle", h, w));
    }

    public void addOval(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addOval", h, w));
    }

    public void addCircle() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addCircle"));
    }

    public void addLabel(String label, int x, int y) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabel", label, x, y));
    }

    public void addLabelToFigure(String figureIdentifier, String label) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabelToFigure", figureIdentifier, label));
    }

    public void populateContextMenu() {

        getElement().callJsFunction("$connector.getSelected").then(String.class, result -> {

            try {
                Container container = mapper.readValue(result, Container.class);

                if(container.getFigures().size() == 1 && container.getFigures().get(0).getType().equals("draw2d.Connection")) {
                    CanvasItemRightClickEvent event = new CanvasItemRightClickEvent(this.designerPalletItemMap.get(container.getFigures().get(0).getIdentifier()),
                        container.getWindowx(), container.getWindowy(), container.getFigures().get(0));

                    this.canvasItemRightClickEventListeners.forEach(listener -> listener.rightClickEvent(event));
                    return;
                }

                for (Figure figure : container.getFigures()) {
                    if (container.getX() > figure.getX() && container.getX() < figure.getX() + figure.getWidth()
                        && container.getY() > figure.getY() && container.getY() < figure.getY() + figure.getHeight()) {

                        CanvasItemRightClickEvent event = new CanvasItemRightClickEvent(this.designerPalletItemMap.get(figure.getIdentifier()),
                            container.getWindowx(), container.getWindowy(), figure);

                        this.canvasItemRightClickEventListeners.forEach(listener -> listener.rightClickEvent(event));
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void bringToFront() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.bringToFront"));
    }

    public void sendToBack() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.sendToBack"));
    }

    public void group() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.group"));
    }

    public void ungroup() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.ungroup"));
    }

    public void setBackgroundColor(String color) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setBackgroundColor", color));
    }

    public void setLineType(String pattern) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setLineType", pattern));
    }

    public void setRadius(double radius) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setRadius", radius));
    }

    public void setStroke(int width) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setStroke", width));
    }

    public void setReadonly(boolean readonly) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setReadOnly", readonly));
        this.exportJson();
    }

    public void rotateSelected(int angle) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.rotate", angle));
    }

    public void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode()
            .runWhenAttached(ui -> ui.beforeClientResponse(this, context -> command.accept(ui)));
    }

    public void addPalletItem(DesignerPalletImageItem designerPalletImageItem) {
        this.designerPalletItemMap.put(designerPalletImageItem.getIdentifier(), designerPalletImageItem);
    }

    public void addCanvasItemRightClickEventListener(CanvasItemRightClickEventListener listener) {
        this.canvasItemRightClickEventListeners.add(listener);
    }

    public void addCanvasItemDoubleClickEventListener(CanvasItemDoubleClickEventListener listener) {
        this.canvasItemDoubleClickEventListeners.add(listener);
    }

    @ClientCallable
    private void doubleClickEvent(String figure){
        try {
            Figure figureObj = mapper.readValue(figure, Figure.class);

            this.canvasItemDoubleClickEventListeners.forEach(listener
                -> listener.doubleClickEvent(new CanvasItemDoubleClickEvent(this.designerPalletItemMap.get(figureObj.getIdentifier())
                    , figureObj.getX(), figureObj.getY(), figureObj)));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ClientCallable
    private void rightClickEvent(String figure, int pageX, int pageY){
        try {
            Figure figureObj = mapper.readValue(figure, Figure.class);

            CanvasItemRightClickEvent event = new CanvasItemRightClickEvent(this.designerPalletItemMap.get(figureObj.getIdentifier()),
                pageX, pageY, figureObj);

            this.canvasItemRightClickEventListeners.forEach(listener -> listener.rightClickEvent(event));

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportJson(){
        getElement().callJsFunction("$connector.exportJson").then(String.class, result -> {
            this.canvasJson = result;
            logger.info(result);
        });
    }

    public void importJson(){
        if(this.canvasJson != null) {
            getElement().callJsFunction("$connector.importJson", this.canvasJson);
        }
    }

    public void exportPng(){
        getElement().callJsFunction("$connector.exportPng");
    }

    public void undo(){
            getElement().callJsFunction("$connector.undo");
    }

    public void redo(){
        getElement().callJsFunction("$connector.redo");
    }

    public void copy(){
        getElement().callJsFunction("$connector.copy");
    }

    public void paste(){
        getElement().callJsFunction("$connector.paste");
    }

    public void delete(){
        getElement().callJsFunction("$connector.delete");
    }
}
