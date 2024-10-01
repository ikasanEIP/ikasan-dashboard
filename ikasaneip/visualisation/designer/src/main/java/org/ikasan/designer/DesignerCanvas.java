package org.ikasan.designer;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.designer.event.*;
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


@Tag("div")
@StyleSheet("/frontend/org/ikasan/draw2d/designer.css")
@StyleSheet("/frontend/org/ikasan/draw2d/spinner.css")
public class DesignerCanvas extends VerticalLayout implements HasSize, BeforeEnterObserver {

    Logger logger = LoggerFactory.getLogger(DesignerCanvas.class);

    private String canvasJson;
    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, DesignerPalletImageItem> designerPalletItemMap = new HashMap<>();
    private List<CanvasItemRightClickEventListener> canvasItemRightClickEventListeners = new ArrayList<>();
    private List<CanvasItemDoubleClickEventListener> canvasItemDoubleClickEventListeners = new ArrayList<>();
    private List<CanvasItemSingleClickEventListener> canvasItemSingleClickEventListeners = new ArrayList<>();
    private List<ConnectorEventListener> connectorEventListeners = new ArrayList<>();
    private List<CanvasInitialisedListener> canvasInitialisedListeners = new ArrayList<>();
    private List<CanvasUpdatedListener> canvasUpdatedListeners = new ArrayList<>();
    private List<FigureUndoDeleteEventListener> figureUndoDeleteEventListeners = new ArrayList<>();
    private List<FigureDeleteEventListener> figureDeleteEventListeners = new ArrayList<>();
    private List<JobMouseOverListener> jobMouseOverListeners = new ArrayList<>();
    private SaveFunction saveFunction;
    private SaveAsFunction saveAsFunction;

    private boolean saved = true;

    private String name;
    private String dynamicImagePath;

    private List<Image> dynamicImages;

    private DesignerDynamicImageManager designerDynamicImageManager;

    private boolean readonly;

    private boolean connectorInitialised = false;

    private UI ui;

    private boolean toBack;



    /**
     * Constructs a DesignerCanvas object with the specified parameters.
     *
     * @param name              the name of the DesignerCanvas object
     * @param dynamicImagePath  the path to the dynamic images
     * @param readonly          whether the DesignerCanvas is read-only or not
     * @param ui                the UI object to associate with the DesignerCanvas
     * @param toBack            whether to send the DesignerCanvas to the back or not
     */
    public DesignerCanvas(String name, String dynamicImagePath, boolean readonly, UI ui, boolean toBack) {
        super();
        this.name = name;
        this.dynamicImagePath = dynamicImagePath;
        this.readonly = readonly;
        this.toBack = toBack;

        this.getElement().getThemeList().remove("padding");
        this.getElement().getThemeList().remove("spacing");
        this.getElement().getStyle().set("border", "1px solid #E0E0E0");
        this.getElement().getStyle().set("padding", "0px");
        this.getElement().getStyle().set("margin", "0px");
        this.getElement().getStyle().set("position", "relative");
        this.getElement().getStyle().set("top", "0px");
        this.getElement().getStyle().set("right", "0px");
        this.getElement().getStyle().set("left", "0px");
        this.getElement().getStyle().set("bottom", "0px");
        this.getElement().getStyle().set("overflow", "scroll");
        this.getElement().getStyle().set("height", "100%");
        this.getElement().getStyle().set("width", "100%");
        this.getElement().getStyle().set("background-color", "#FFFFFF");

        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/jquery.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/jquery-ui.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/draw2d.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/spinner.umd.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/designer-connector-flow.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/mousetrap.min.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/view.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/RotateRectangleFeedbackSelectionPolicy.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/RotateHandle.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/Triangle.js");
        ui.getPage().addJavaScript("/frontend/org/ikasan/draw2d/NoDecorator.js");

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


    /**
     * Constructs a new DesignerCanvas object.
     *
     * @param saveFunction the function to be called when the canvas is saved
     * @param saveAsFunction the function to be called when the canvas is saved as a new file
     * @param name the name of the canvas
     * @param dynamicImagePath the path to the dynamic image file
     * @param readonly whether the canvas is read-only or not
     * @param ui the user interface object associated with the canvas
     * @param toBack whether the canvas should be sent to the back or not
     */
    public DesignerCanvas(SaveFunction saveFunction, SaveAsFunction saveAsFunction, String name
        , String dynamicImagePath, boolean readonly, UI ui, boolean toBack) {
        this(name, dynamicImagePath, readonly, ui, toBack);
        this.saveFunction = saveFunction;
        this.saveAsFunction = saveAsFunction;
    }

    /**
     * This method works in combination with designer-connector-flow.js to set up the
     * interaction between the Vaadin framework and draw2d javascript.
     */
    private void initConnector() {
        getUI()
            .orElseThrow(() -> new IllegalStateException(
                "Connector can only be initialized for an attached Designer"))
            .getPage()
            .executeJs("window.Vaadin.Flow.designerConnector.initLazy($0, $1, $2)",
                getElement(), this.name, this.readonly);

        this.setReadonly(this.readonly);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        initConnector();

        if(this.canvasJson != null && ! connectorInitialised) {
            this.importJson(this.toBack);
        }

        if(!connectorInitialised) {
            connectorInitialised = true;
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }

    /**
     * Add an image icon to the draw2d canvas.
     *
     * @param identifier - the identifier assigned to the icon.
     * @param image the image to render.
     * @param h the height of the image to render
     * @param w the width of the image to render
     * @param isClickable show hand icon when hovered over
     */
    public void addIcon(String identifier, String image, double h, double w, boolean isClickable) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addIconNoCoordinates"
                , identifier, image, h, w, isClickable));
        this.saved = false;
    }

    /**
     * Add an image icon to the draw2d canvas.
     *
     * @param identifier - the identifier assigned to the icon.
     * @param image the image to render.
     * @param x the x location to render the icon on the canvas
     * @param y the y location to render the icon on the canvas
     * @param h the height of the image to render
     * @param w the width of the image to render
     * @param showPorts show the connection ports on the icon
     * @param isClickable show hand icon when hovered over
     */
    public void addIcon(String identifier, String image, double x, double y, double h, double w, boolean showPorts, boolean isClickable) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addIcon", identifier, image, x, y, h, w, showPorts, isClickable));
        this.saved = false;
    }

    /**
     * Add things like tooltips and pointer to icons.
     */
    public void manageClickableItems() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.manageClickableItems"));
        this.saved = false;
    }


    /**
     * Add a boundary to the most recent x and y location.
     *
     * @param h the height of the boundary
     * @param w the width of the boundary
     */
    public void addBoundary(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addBoundarySimple", h, w));
        this.saved = false;
    }

    /**
     * Add a boundary at a x and y location.
     *
     * @param identifier the identifier to assign to the boundary.
     * @param shapeIdentifier the shape identifier to bring to the front
     * @param x the x location to render the boundary
     * @param y the y location to render the boundary
     * @param h the height of the boundary
     * @param w the width of the boundary
     * @param colour the colour to render the boundary
     */
    public void addBoundary(String identifier, String shapeIdentifier, double x, double y, double h, double w, String colour) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addBoundaryToShape", identifier, shapeIdentifier, x, y, h, w, colour));
        this.saved = false;
    }

    /**
     * Add a styled boundary at the most recent x and y location.
     *
     * @param identifier the identifier to assign to the boundary.
     * @param w the width of the boundary
     * @param h he height of the boundary
     * @param dashArray the format of the line of the boundary
     * @param colour the colour of the boundary to render
     * @param stroke the line stroke of the boundary
     */
    public void addBoundaryStyled(String identifier, double w, double h, String dashArray, String colour, int stroke) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addBoundaryStyled"
                , identifier, w, h, dashArray, colour, stroke));
        this.saved = false;
    }

    /**
     * Remove a figure from the canvas based on its identifier.
     *
     * @param identifier the identifier of the figure to remove.
     */
    public void removeFigure(String identifier) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.removeFigure", identifier));
        this.saved = false;
    }

    /**
     * Add a triangle boundary to the canvas at the most recent x and y location.
     *
     * @param h the height of the triangle
     * @param w the width of the triangle
     */
    public void addTriangleBoundary(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addTriangle", h, w));
        this.saved = false;
    }

    /**
     * Add an oval to the canvas at the most recent x and y location.
     *
     * @param h the height of the oval
     * @param w the width of the oval
     */
    public void addOval(double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addOval", h, w));
        this.saved = false;
    }

    /**
     * Add a circle to the canvas at the most recent x and y location.
     *
     * @param diameter the diameter of the circle
     */
    public void addCircle(double diameter) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addCircle", diameter));
        this.saved = false;
    }

    /**
     * Add a label to the canvas at the most recent x and y location.
     *
     * @param label the string in the label
     */
    public void addLabel(String label) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabel", label));
        this.saved = false;
    }

    /**
     * Add an image to the canvas at the most recent x and y location.
     *
     * @param image the path to the image.
     */
    public void addImageFigure(String image) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addImageFigure", image));
        this.saved = false;
    }


    /**
     * Adds a connection to the UI.
     *
     * @param connection the connection string to be added
     */
    public void addConnection(String connection) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addConnection", connection));
        this.saved = false;
    }

    /**
     * Add an image to a figure.
     *
     * @param figureIdentifier the identifier of the figure to add the image to.
     * @param iconIdentifier the identifier of the added image
     * @param image the path to the image
     * @param h the height of the image
     * @param w the width of the image
     */
    public void addImageToFigure(String figureIdentifier, String iconIdentifier, String image, double h, double w) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addImageToFigure"
                , figureIdentifier, iconIdentifier, image, h, w));
        this.saved = false;
    }

    /**
     * Add a triangle to a figure.
     *
     * @param figureIdentifier the identifier of the figure to add the image to
     * @param h the height of the triangle
     * @param w the width of the triangle
     * @param backgroundColour the background colour of the image
     */
    public void addTriangleToFigure(String figureIdentifier, double h, double w, String backgroundColour) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addTriangleToFigure"
                , figureIdentifier, h, w, backgroundColour));
        this.saved = false;
    }

    /**
     * Add a boundary to a figure.
     *
     * @param figureIdentifier the identifier of the figure to add the boundary to
     * @param itemIdentifier the identifier of the added boundary
     * @param h the height of the boundary
     * @param w the width of the boundary
     * @param lineFormat the format of the boundary line
     * @param backgroundColor the background colour of the boundary fill
     * @param scrollToFigure the location of the boundary to scroll to
     */
    public void addBoundaryToFigure(String figureIdentifier, String itemIdentifier, double h, double w
        , String lineFormat, String backgroundColor, boolean scrollToFigure) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addBoundaryToFigure"
                , figureIdentifier, itemIdentifier, h, w, lineFormat, backgroundColor, scrollToFigure));
        this.saved = false;
    }

    /**
     * Add a label to a given figure.
     *
     * @param figureIdentifier the figure identifier to add the label to
     * @param label the string that appears in the label
     */
    public void addLabelToFigure(String figureIdentifier, String label) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabelToFigure", figureIdentifier, label));
        this.saved = false;
    }

    /**
     * Add a label to a given figure.
     *
     * @param figureIdentifier the figure identifier to add the label to
     * @param label the string that appears in the label
     */
    public void addLabelToFigure(String figureIdentifier, String label, String fontSize) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabelToFigureWithFontSize"
                , figureIdentifier, label, fontSize));
        this.saved = false;
    }

    /**
     * Add a label to a give x y location
     * @param label the string that appears in the label
     * @param x the x coordinate of the label
     * @param y the y coordinate of the label
     */
    public void addLabel(String label, double x, double y) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.addLabelWithCoordinates", label, x, y));
        this.saved = false;
    }

    /**
     * Populates the context menu based on the selected element in the canvas.
     *
     * If a single connection is selected, triggers a {@link CanvasItemRightClickEvent} for the corresponding designer pallet item.
     *
     * If multiple figures are selected, triggers a {@link CanvasItemRightClickEvent} for each figure that contains the click coordinates.
     *
     * @see CanvasItemRightClickEvent
     */
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
                logger.error("An error has occurred populating context menu", e);
            }
        });
    }

    /**
     * Set the font on the canvas.
     *
     * @param font
     */
    public void setFont(String font) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setFont", font));
        this.saved = false;
    }

    /**
     * Start the spinner on the canvas.
     */
    public void startSpinner() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.startSpinner"));
    }

    /**
     * Stop the spinner on the canvas.
     */
    public void stopSpinner() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.stopSpinner"));
    }

    /**
     * Deselect all selected figures on the canvas.
     */
    public void deselectAllFigures() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.deselectAllFigures"));
    }

    /**
     * Set the target decorator on a line.
     *
     * @param decorator
     */
    public void setLineTargetDecorator(String decorator) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setTargetDecorator", decorator));
        this.saved = false;
    }

    /**
     * Set the source decorator on a line.
     *
     * @param decorator
     */
    public void setLineSourceDecorator(String decorator) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setSourceDecorator", decorator));
        this.saved = false;
    }

    /**
     * Set the font size on the canvas.
     *
     * @param fontSize
     */
    public void setFontSize(String fontSize) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setFontSize", fontSize));
        this.saved = false;
    }

    /**
     * Bring selected items to front.
     */
    public void bringToFront() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.bringToFront"));
        this.saved = false;
    }

    /**
     * Send selected items to the back.
     */
    public void sendToBack() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.sendToBack"));
        this.saved = false;
    }

    /**
     * Group selected items.
     */
    public void group() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.group"));
        this.saved = false;
    }

    /**
     * Ungroup selected items.
     */
    public void ungroup() {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.ungroup"));
        this.saved = false;
    }

    /**
     * Set background colour of selected item.
     *
     * @param color
     */
    public void setBackgroundColor(String color) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setBackgroundColor", color));
        this.saved = false;
    }

    /**
     * Set background colour of figure.
     *
     * @param identifier the identifier of the figure
     * @param color the colour to set
     */
    public void setBackgroundColor(String identifier, String color) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setBackgroundColorOnFigure", identifier, color));
        this.saved = false;
    }

    /**
     * Set the line type of the selected items.
     * @param pattern
     */
    public void setLineType(String pattern) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setLineType", pattern));
        this.saved = false;
    }

    /**
     * Set the radius of the selected items.
     *
     * @param radius
     */
    public void setRadius(double radius) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setRadius", radius));
        this.saved = false;
    }

    /**
     * Set the line stroke of the selected items.
     *
     * @param width
     */
    public void setStroke(int width) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setStroke", width));
        this.saved = false;
    }

    /**
     * Set the canvas to read only.
     * @param readonly
     */
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.setReadOnly", readonly));
    }

    /**
     * Rotate the selected items.
     *
     * @param angle
     */
    public void rotateSelected(int angle) {
        runBeforeClientResponse(
            ui -> getElement().callJsFunction("$connector.rotate", angle));
        this.saved = false;
    }

    /**
     * Runs the given command before the client receives a response from the server.
     *
     * @param command the command to run before the client response.
     *                Accepts a {@link SerializableConsumer} with a {@link UI} parameter.
     *                Use this parameter to access the current UI and perform any necessary operations.
     * @throws NullPointerException if the command is null
     */
    public void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode()
            .runWhenAttached(ui -> ui.beforeClientResponse(this, context -> command.accept(ui)));
    }

    /**
     * Adds a pallet item to the pallet.
     *
     * @param designerPalletImageItem the pallet item to add
     */
    public void addPalletItem(DesignerPalletImageItem designerPalletImageItem) {
        this.designerPalletItemMap.put(designerPalletImageItem.getIdentifier().toString(), designerPalletImageItem);
    }

    /**
     * Adds a CanvasItemRightClickEventListener to the list of listeners.
     * The listener will be notified when a right-click event occurs on a canvas item.
     *
     * @param listener the CanvasItemRightClickEventListener to be added
     */
    public void addCanvasItemRightClickEventListener(CanvasItemRightClickEventListener listener) {
        this.canvasItemRightClickEventListeners.add(listener);
    }

    /**
     * Adds a CanvasItemDoubleClickEventListener to the list of event listeners.
     *
     * @param listener the CanvasItemDoubleClickEventListener to be added
     */
    public void addCanvasItemDoubleClickEventListener(CanvasItemDoubleClickEventListener listener) {
        this.canvasItemDoubleClickEventListeners.add(listener);
    }

    /**
     * Adds a {@link CanvasItemSingleClickEventListener} to the list of listeners.
     *
     * @param listener the listener to be added
     */
    public void addCanvasItemSingleClickEventListener(CanvasItemSingleClickEventListener listener) {
        this.canvasItemSingleClickEventListeners.add(listener);
    }

    /**
     * Adds a ConnectorEventListener to the list of listeners.
     *
     * @param listener the ConnectorEventListener to be added
     */
    public void addConnectorEventListener(ConnectorEventListener listener) {
        this.connectorEventListeners.add(listener);
    }

    /**
     * Adds a FigureDeleteEventListener to the list of listeners.
     *
     * @param listener the FigureDeleteEventListener to be added
     */
    public void addFigureDeleteEventListeners(FigureDeleteEventListener listener) {
        this.figureDeleteEventListeners.add(listener);
    }

    public void addJobMouseOverEventListener(JobMouseOverListener jobMouseOverListener) {
        this.jobMouseOverListeners.add(jobMouseOverListener);
    }

    /**
     * Adds an instance of FigureUndoDeleteEventListener to the list of event listeners.
     *
     * @param listener the FigureUndoDeleteEventListener to add
     */
    public void addFigureUndoDeleteEventListeners(FigureUndoDeleteEventListener listener) {
        this.figureUndoDeleteEventListeners.add(listener);
    }

    /**
     * Adds a CanvasUpdatedListener to the list of listeners.
     *
     * @param listener The listener to be added.
     */
    public void addCanvasUpdatedListener(CanvasUpdatedListener listener) {
        this.canvasUpdatedListeners.add(listener);
    }

    @ClientCallable
    private void showJobDetails(String figure){
        try {
            Figure figureObj = mapper.readValue(figure, Figure.class);
            this.jobMouseOverListeners.forEach(jobMouseOverListener -> jobMouseOverListener.onJobMouseOverEvent(new JobMouseOverEvent(figureObj)));
        }
        catch (JsonProcessingException e) {
            logger.error("An error has occurred processing figure json associated with a job mouse over event - [%s]!".formatted(e.getMessage()), e);
        }
    }

    @ClientCallable
    private void connectorEvent(String event){
        try {
            ConnectorEvent connectorEvent = mapper.readValue(event, ConnectorEvent.class);
            logger.debug("Event received: " + connectorEvent.getCanvasJson());

            this.connectorEventListeners.forEach(listener
                -> listener.connectorEvent(connectorEvent));
        }
        catch (Exception e) {
           logger.error(e.getMessage(), e);
        }
    }

    @ClientCallable
    private void canvasUpdatedEvent(String event){
        try {
            logger.debug("Event received: " + event);
            CanvasUpdatedEvent canvasUpdatedEvent = mapper.readValue(event, CanvasUpdatedEvent.class);

            this.canvasUpdatedListeners.forEach(listener
                -> listener.canvasUpdated(canvasUpdatedEvent));
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
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
            logger.error(e.getMessage(), e);
        }
    }

    @ClientCallable
    private void clickEvent(String figure){
        try {
            Figure figureObj = mapper.readValue(figure, Figure.class);

            this.canvasItemSingleClickEventListeners.forEach(listener
                -> listener.singleClickEvent(new CanvasItemSingleClickEvent(this.designerPalletItemMap.get(figureObj.getIdentifier())
                , figureObj.getX(), figureObj.getY(), figureObj)));
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @ClientCallable
    private void rightClickEvent(String figure){
        try {
            Figure figureObj = mapper.readValue(figure, Figure.class);

            CanvasItemRightClickEvent event = new CanvasItemRightClickEvent(this.designerPalletItemMap.get(figureObj.getIdentifier()),
                figureObj.getX(), figureObj.getY(), figureObj);

            this.canvasItemRightClickEventListeners.forEach(listener -> listener.rightClickEvent(event));

        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @ClientCallable
    private void figureDeleted(String figure){
        try {
            Figure figureObj = mapper.readValue(figure, Figure.class);

            FigureDeleteEvent figureDeleteEvent = new FigureDeleteEvent(figureObj);
            this.figureDeleteEventListeners.forEach(listener -> listener.figureDeleted(figureDeleteEvent));
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @ClientCallable
    private void undoFigureDeleted(String figure){
        try {
            Figure figureObj = mapper.readValue(figure, Figure.class);

            FigureUndoDeleteEvent figureUndoDeleteEvent = new FigureUndoDeleteEvent(figureObj);
            this.figureUndoDeleteEventListeners.forEach(listener -> listener.undoFigureDeleted(figureUndoDeleteEvent));

        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @ClientCallable
    private void canvasInitialised(){
        this.canvasInitialisedListeners.forEach(listener -> listener.canvasInitialised());
    }

    public void addCanvasInitialisedListener(CanvasInitialisedListener listener) {
        this.canvasInitialisedListeners.add(listener);
    }

    public void exportJson(){
        getElement().callJsFunction("$connector.exportJson").then(String.class, canvasJson -> {
            this.canvasJson = canvasJson;
            logger.debug(canvasJson);
        });
    }

    public void save(String id, String name, String description){
        getElement().callJsFunction("$connector.exportJson").then(String.class, canvasJson -> {
            this.canvasJson = canvasJson;
            logger.debug(canvasJson);

            if(this.saveFunction != null) {
                this.saveFunction.save(id, name, description, canvasJson);
            }

            this.saved = true;
        });
    }

    public void saveAs(){
        getElement().callJsFunction("$connector.exportJson").then(String.class, result -> {
            this.canvasJson = result;
            logger.debug(result);

            if(this.saveAsFunction != null) {
                this.saveAsFunction.saveAs(result);
            }

            this.saved = true;
        });
    }

    public void importJson() {
        this.clear();
        this.importJson(this.toBack);
    }

    public void importJson(boolean toBack){
        if(this.canvasJson != null) {
            this.saved = true;
            this.clear();
            getElement().callJsFunction("$connector.importJson", this.canvasJson, toBack);
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

    public void zoomIn(){
        getElement().callJsFunction("$connector.zoomIn");
        this.saved = true;
    }

    public void zoomOut(){
        getElement().callJsFunction("$connector.zoomOut");
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
