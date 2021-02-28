package org.ikasan.designer;

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import nc.unc.vaadin.flow.polymer.iron.icons.IronImageIcons;
import org.ikasan.designer.component.ColorPicker;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.pallet.DesignerPalletImageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Designer extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(Designer.class);

    private DesignerCanvas designerCanvas;
    private Accordion toolAccordion = new Accordion();
    private List<ItemPallet> itemPalettes;
    private ColorPicker paintButton = new ColorPicker();


    private boolean initialised = false;

    /**
     * Constructor
     */
    public Designer()
    {
        this.setMargin(false);
        this.setSpacing(false);

        this.setHeight("100%");
        this.setWidth("100%");


        this.itemPalettes = new ArrayList<>();
        init();
    }

    protected void init()
    {
//        this.designerCanvas = new DesignerCanvas();
//        this.designerCanvas.setSizeUndefined();
//
//        DropTarget<DesignerCanvas> dropTarget = DropTarget.create(this.designerCanvas);
//
//        dropTarget.addDropListener(event -> {
//            // move the dragged component to inside the drop target component
//            event.getDragSourceComponent().ifPresent(action -> this.addItemToCanvas((DesignerPalletItem)action));
//        });

        this.initBase();

        this.toolAccordion = new Accordion();
        this.toolAccordion.getElement().getStyle().set("font-size", "8pt");
        this.toolAccordion.setWidthFull();
        this.toolAccordion.close();

        VerticalLayout toolLayout = new VerticalLayout();
        toolLayout.setSpacing(false);
        toolLayout.setMargin(false);
        toolLayout.setWidthFull();
        toolLayout.add(toolAccordion);
        toolLayout.getElement().getThemeList().remove("padding");

        HorizontalLayout designerLayout = new HorizontalLayout();
        designerLayout.setSizeUndefined();
        designerLayout.getElement().getThemeList().remove("padding");



        Div tools = new Div();
        tools.setId("canvas-palette");
        tools.add(toolLayout);

        designerLayout.add(this.buildMenuBar(), this.initCanvasActions(), tools, this.designerCanvas);
        this.add(designerLayout);

        this.initialised = true;
    }

    protected Div buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        Text selected = new Text("");

        MenuItem file = menuBar.addItem("File");
        MenuItem edit = menuBar.addItem("Edit");
        menuBar.addItem("Help", e -> selected.setText("Sign Out"));

        SubMenu fileSubMenu = file.getSubMenu();
        MenuItem newDiagram = fileSubMenu.addItem("New");
        MenuItem open = fileSubMenu.addItem("Open");
        open.addClickShortcut(Key.KEY_O, KeyModifier.CONTROL);

        fileSubMenu.add(new Hr());
        MenuItem save = fileSubMenu.addItem("Save");
        MenuItem saveAs = fileSubMenu.addItem("Save as");

        fileSubMenu.add(new Hr());

        MenuItem publish = fileSubMenu.addItem("Publish");
        publish.setCheckable(true);
        publish.setChecked(false);
        MenuItem suppress = fileSubMenu.addItem("Suppress");
        suppress.setCheckable(true);
        suppress.setChecked(false);

        fileSubMenu.add(new Hr());

        MenuItem exportAs = fileSubMenu.addItem("Export as");

        exportAs.getSubMenu().addItem("png",
            e -> selected.setText("Edit Profile"));
        exportAs.getSubMenu().addItem("jpg",
            e -> selected.setText("Privacy Settings"));
        exportAs.getSubMenu().addItem("json",
            e -> selected.setText("Privacy Settings"));
        exportAs.getSubMenu().addItem("svg",
            e -> selected.setText("Privacy Settings"));

        fileSubMenu.add(new Hr());

        MenuItem close = fileSubMenu.addItem("Close");

        Div div = new Div();
        div.setId("canvas-menu");
        div.add(menuBar);
       return div;
    }

    protected Div initCanvasActions() {
        Div actions = new Div();
        actions.setId("canvas-actions");

        // Group canvas items
        Button groupButton = new Button();
        groupButton.addClickListener(buttonClickEvent -> {
            this.designerCanvas.group();
        });
        groupButton.getElement().appendChild(FontAwesome.Regular.OBJECT_GROUP.create().getElement());
        actions.add(groupButton);

        // Ungroup canvas items
        Button ungroupButton = new Button();
        ungroupButton.addClickListener(buttonClickEvent -> {
            this.designerCanvas.ungroup();
        });
        ungroupButton.getElement().appendChild(FontAwesome.Regular.OBJECT_UNGROUP.create().getElement());
        actions.add(ungroupButton);

        // Bring selected items to front
        Button toFrontButton = new Button();
        toFrontButton.addClickListener(buttonClickEvent -> {
            this.designerCanvas.bringToFront();
        });
        toFrontButton.getElement().appendChild(IronIcons.FLIP_TO_FRONT.create().getElement());
        actions.add(toFrontButton);

        // Send selected items to back
        Button toBackButton = new Button();
        toBackButton.addClickListener(buttonClickEvent -> {
            this.designerCanvas.sendToBack();
        });
        toBackButton.getElement().appendChild(IronIcons.FLIP_TO_BACK.create().getElement());
        actions.add(toBackButton, getDivider());

        // Undo
        Button undoButton = new Button();
        undoButton.getElement().appendChild(IronIcons.UNDO.create().getElement());
        undoButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.undo());
        actions.add(undoButton);

        // Redo
        Button redoButton = new Button();
        redoButton.getElement().appendChild(IronIcons.REDO.create().getElement());
        redoButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.redo());
        actions.add(redoButton, getDivider());

        // Zoom in
        Button zoomInButton = new Button();
        zoomInButton.getElement().appendChild(IronIcons.ZOOM_IN.create().getElement());
        zoomInButton.setId("canvas_zoom_in");
        actions.add(zoomInButton);

        // Zoom out
        Button zoomOutButton = new Button();
        zoomOutButton.getElement().appendChild(IronIcons.ZOOM_OUT.create().getElement());
        zoomOutButton.setId("canvas_zoom_out");
        actions.add(zoomOutButton, getDivider());

        // Copy
        Button copyButton = new Button();
        copyButton.getElement().appendChild(IronIcons.CONTENT_COPY.create().getElement());
        copyButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.designerCanvas.copy());
        actions.add(copyButton);

        // Paste
        Button pasteButton = new Button();
        pasteButton.getElement().appendChild(IronIcons.CONTENT_PASTE.create().getElement());
        pasteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.designerCanvas.paste());
        actions.add(pasteButton);

        // Delete
        Button deleteButton = new Button();
        deleteButton.getElement().appendChild(IronIcons.DELETE.create().getElement());
        deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.designerCanvas.delete());
        actions.add(deleteButton, getDivider());


        // Export as selected format
        Button download = new Button();
        download.getElement().appendChild(IronIcons.FILE_DOWNLOAD.create().getElement());
        actions.add(download);
        download.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.exportPng();
        });

        // Open another design
        Button open = new Button();
        open.getElement().appendChild(IronIcons.FOLDER_OPEN.create().getElement());
        actions.add(open);
        open.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.exportJson();
        });

        // Save current design
        Button save = new Button();
        save.getElement().appendChild(IronIcons.SAVE.create().getElement());
        actions.add(save, getDivider());
        save.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.exportJson();
        });

        Button eyeDropper = new Button();
        eyeDropper.getElement().appendChild(IronImageIcons.COLORIZE.create().getElement());
        actions.add(eyeDropper);
        eyeDropper.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {

        });

        paintButton = new ColorPicker();
        paintButton.addValueChangeListener((HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>)
            textFieldStringComponentValueChangeEvent -> {
                this.designerCanvas.setBackgroundColor(textFieldStringComponentValueChangeEvent.getValue());
            });
        actions.add(paintButton, getDivider());

        return actions;
    }

    protected void initBase() {
        this.designerCanvas = new DesignerCanvas();
        this.designerCanvas.setSizeUndefined();

        DropTarget<DesignerCanvas> dropTarget = DropTarget.create(this.designerCanvas);

        dropTarget.addDropListener(event -> {
            // move the dragged component to inside the drop target component
            event.getDragSourceComponent().ifPresent(action -> this.addItemToCanvas((DesignerPalletImageItem)action));
        });

        paintButton = new ColorPicker();
        paintButton.addValueChangeListener((HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>)
            textFieldStringComponentValueChangeEvent -> {
                this.designerCanvas.setBackgroundColor(textFieldStringComponentValueChangeEvent.getValue());
            });
    }

    public void addItemToCanvas(DesignerPalletImageItem item) {
        item.executeCanvasAddAction();
        designerCanvas.addPalletItem(item);
        switch(item.getDesignerPalletItemType()) {
            case ICON:
                designerCanvas.addIcon(item.getIdentifier(),
                    item.getSrc(), item.getItemHeight(),
                    item.getItemWidth());
                break;
            case RECTANGLE:
                designerCanvas.addBoundary(100, 300);
                break;
            case TRIANGLE:
                designerCanvas.addTriangleBoundary(300, 300);
                break;
            case OVAL:
                designerCanvas.addOval(300, 300);
                break;
            case CIRCLE:
                designerCanvas.addCircle();
                break;
            case LABEL:
                designerCanvas.addLabel("Click me", 100, 100);
                break;
        }
    }

    public void addLabelToItem(DesignerPalletImageItem item, String label) {
        designerCanvas.addLabelToFigure(item.getIdentifier(), label);
    }

    private Image getDivider() {
        Image divider = new Image("frontend/images/separator.png", "");
        divider.setWidth("5px");
        divider.setHeight("20px");
        divider.getStyle().set("display", "inline-block");
        divider.getStyle().set("vertical-align", "middle");

        return divider;
    }

    public void addItemPallet(ItemPallet itemPallet) {
        this.itemPalettes.add(itemPallet);
        this.toolAccordion.add(itemPallet.getSummary(), itemPallet.getPallet());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!initialised)
        {
            this.init();
            initialised = true;
        }

        this.paintButton.attachSpectrum();
//        this.initBase();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        UI ui = attachEvent.getUI();

//        broadcasterRegistration = FlowStateBroadcaster.register(flowState ->
//        {
//            ui.access(() ->
//            {
//                // do something interesting here.
//                logger.debug("Received flow state: " + flowState);
//            });
//        });

    }

    @Override
    protected void onDetach(DetachEvent detachEvent)
    {
//        broadcasterRegistration.remove();
//        broadcasterRegistration = null;
    }

    public void setLineType(String pattern) {
        designerCanvas.setLineType(pattern);
    }

    public void setRadius(double radius) {
        designerCanvas.setRadius(radius);
    }

    public void setStroke(int width) {
        this.designerCanvas.setStroke(width);
    }

    public void setReadonly(boolean readonly) {
        this.designerCanvas.setReadonly(readonly);
    }

    public void addCanvasItemRightClickEventListener(CanvasItemRightClickEventListener listener) {
        this.designerCanvas.addCanvasItemRightClickEventListener(listener);
    }

    public void addCanvasItemDoubleClickEventListener(CanvasItemDoubleClickEventListener listener) {
        this.designerCanvas.addCanvasItemDoubleClickEventListener(listener);
    }

    public void exportJson(){
        this.designerCanvas.exportJson();
    }

    public void importJson(){
        this.designerCanvas.importJson();
    }

    public void exportPng(){
        this.designerCanvas.exportPng();
    }

    public void undo(){
        this.designerCanvas.undo();
    }

    public void redo(){
        this.designerCanvas.redo();
    }
}

