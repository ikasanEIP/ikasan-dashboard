package org.ikasan.designer;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.function.OpenFunction;
import org.ikasan.designer.function.SaveAsFunction;
import org.ikasan.designer.function.SaveFunction;
import org.ikasan.designer.json.DesignerDynamicImageManager;
import org.ikasan.designer.model.Container;
import org.ikasan.designer.model.Figure;
import org.ikasan.designer.pallet.DesignerPalletImageItem;
import org.ikasan.designer.util.DynamicImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Wraps a visjs network diagram. See http://visjs.org/network_examples.html
 */
@SuppressWarnings("serial")
@Tag("div")
@StyleSheet("./org/ikasan/draw2d/designer.css")
public class DesignerCanvas extends VerticalLayout implements HasSize, BeforeEnterObserver {

    Logger logger = LoggerFactory.getLogger(DesignerCanvas.class);

    private String canvasJson;

    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, DesignerPalletImageItem> designerPalletItemMap = new HashMap<>();
    private List<CanvasItemRightClickEventListener> canvasItemRightClickEventListeners
        = new ArrayList<>();
    private List<CanvasItemDoubleClickEventListener> canvasItemDoubleClickEventListeners
        = new ArrayList<>();

    private SaveFunction saveFunction;
    private SaveAsFunction saveAsFunction;

    private boolean saved = true;

    private String name;
    private String dynamicImagePath;

    private List<Image> dynamicImages;

    private DesignerDynamicImageManager designerDynamicImageManager;

    private boolean readonly;

    public DesignerCanvas(String name, String dynamicImagePath, boolean readonly) {
        super();
        this.name = name;
        this.dynamicImagePath = dynamicImagePath;
        this.readonly = readonly;

        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/jquery.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/jquery-ui.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/draw2d.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/designer-connector-flow.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/mousetrap.min.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/view.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/RotateRectangleFeedbackSelectionPolicy.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/RotateHandle.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/Triangle.js");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/draw2d/NoDecorator.js");

        // Dont transfer empty options.
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        // Dont transfer getter and setter
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withGetterVisibility(Visibility.NONE).withSetterVisibility(Visibility.NONE)
            .withIsGetterVisibility(Visibility.NONE).withFieldVisibility(Visibility.ANY));
        // remains utf8 escaped chars
        mapper.configure(Feature.ESCAPE_NON_ASCII, true);

        this.setId(name);

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setTarget(this);

        getElement().addEventListener("vaadin-context-menu-before-open", e -> {
            contextMenu.setVisible(false);
            populateContextMenu();
        });

        try {
            this.dynamicImages = DynamicImageHelper.loadDynamicImages(this.dynamicImagePath);
            this.designerDynamicImageManager = new DesignerDynamicImageManager(this.dynamicImages);

            this.dynamicImages.forEach(image -> {
                this.add(image);
                image.setVisible(false);
            });
        }
        catch (IOException e) {
            logger.warn("Could not load dynamic images", e);
        }
    }

    public DesignerCanvas(SaveFunction saveFunction, SaveAsFunction saveAsFunction, String name, String dynamicImagePath, boolean readonly) {
        this(name, dynamicImagePath, readonly);
        this.saveFunction = saveFunction;
        this.saveAsFunction = saveAsFunction;
    }

    private void initConnector() {
        getUI()
            .orElseThrow(() -> new IllegalStateException(
                "Connector can only be initialized for an attached Designer"))
            .getPage()
            .executeJs("window.Vaadin.Flow.designerConnector.initLazy($0, $1)",
                getElement(), this.name, this.readonly);

        this.setReadonly(this.readonly);
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

    public void addIcon(String identifier, String image, double h, double w, boolean isClickable) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addIconNoCoordinates", identifier, image, h, w, isClickable));
        this.saved = false;
    }

    public void addIcon(String identifier, String image, double x, double y, double h, double w, boolean showPorts, boolean isClickable) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addIcon", identifier, image, x, y, h, w, showPorts, isClickable));
        this.saved = false;
    }

    public void manageClickableItems() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.manageClickableItems"));
        this.saved = false;
    }


    public void addBoundary(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addBoundarySimple", h, w));
        this.saved = false;
    }

    public void addBoundary(String identifier, String shapeIdentifier, double x, double y, double h, double w, String colour) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addBoundaryToShape", identifier, shapeIdentifier, x, y, h, w, colour));
        this.saved = false;
    }

    public void removeFigure(String identifier) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.removeFigure", identifier));
        this.saved = false;
    }

    public void addTriangleBoundary(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addTriangle", h, w));
        this.saved = false;
    }

    public void addOval(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addOval", h, w));
        this.saved = false;
    }

    public void addCircle(double diameter) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addCircle", diameter));
        this.saved = false;
    }

    public void addLabel(String label) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabel", label));
        this.saved = false;
    }

    public void addLabelToFigure(String figureIdentifier, String label) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabelToFigure", figureIdentifier, label));
        this.saved = false;
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

    public void setFont(String font) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setFont", font));
        this.saved = false;
    }

    public void setLineTargetDecorator(String decorator) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setTargetDecorator", decorator));
        this.saved = false;
    }

    public void setLineSourceDecorator(String decorator) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setSourceDecorator", decorator));
        this.saved = false;
    }

    public void setFontSize(String fontSize) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setFontSize", fontSize));
        this.saved = false;
    }

    public void bringToFront() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.bringToFront"));
        this.saved = false;
    }

    public void sendToBack() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.sendToBack"));
        this.saved = false;
    }

    public void group() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.group"));
        this.saved = false;
    }

    public void ungroup() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.ungroup"));
        this.saved = false;
    }

    public void setBackgroundColor(String color) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setBackgroundColor", color));
        this.saved = false;
    }

    public void setLineType(String pattern) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setLineType", pattern));
        this.saved = false;
    }

    public void setRadius(double radius) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setRadius", radius));
        this.saved = false;
    }

    public void setStroke(int width) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setStroke", width));
        this.saved = false;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setReadOnly", readonly));
    }

    public void rotateSelected(int angle) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.rotate", angle));
        this.saved = false;
    }

    public void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode()
            .runWhenAttached(ui -> ui.beforeClientResponse(this, context -> command.accept(ui)));
    }

    public void addPalletItem(DesignerPalletImageItem designerPalletImageItem) {
        this.designerPalletItemMap.put(designerPalletImageItem.getIdentifier().toString(), designerPalletImageItem);
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
        getElement().callJsFunction("$connector.exportJson").then(String.class, canvasJson -> {
            this.canvasJson = canvasJson;
            logger.info(canvasJson);
        });
    }

    public void save(String id, String name, String description){
        getElement().callJsFunction("$connector.exportJson").then(String.class, canvasJson -> {
            this.canvasJson = canvasJson;
            logger.info(canvasJson);

            if(this.saveFunction != null) {
                this.saveFunction.save(id, name, description, canvasJson);
            }

            this.saved = true;
        });
    }

    public void saveAs(){
        getElement().callJsFunction("$connector.exportJson").then(String.class, result -> {
            this.canvasJson = result;
            logger.info(result);

            if(this.saveAsFunction != null) {
                this.saveAsFunction.saveAs(result);
            }

            this.saved = true;
        });
    }

    public void importJson(){
        if(this.canvasJson != null) {
            this.saved = true;
            getElement().callJsFunction("$connector.importJson", this.canvasJson);
        }
    }

    public void exportPng(){
        getElement().callJsFunction("$connector.exportPng");
        this.saved = false;
    }

    public void undo(){
        getElement().callJsFunction("$connector.undo");
        this.saved = false;
    }

    public void redo(){
        getElement().callJsFunction("$connector.redo");
        this.saved = false;
    }

    public void copy(){
        getElement().callJsFunction("$connector.copy");
        this.saved = false;
    }

    public void paste(){
        getElement().callJsFunction("$connector.paste");
        this.saved = false;
    }

    public void delete(){
        getElement().callJsFunction("$connector.delete");
        this.saved = false;
    }

    public void clear(){
        getElement().callJsFunction("$connector.clear");
        this.saved = true;
    }

    public void setCanvasJson(String canvasJson) throws IOException {
        if(this.designerDynamicImageManager != null) {
            this.canvasJson = this.designerDynamicImageManager.parse(canvasJson);
        }
        else {
            this.canvasJson = canvasJson;
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.saved = true;
    }
}
