package org.ikasan.designer;

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import nc.unc.vaadin.flow.polymer.iron.icons.IronImageIcons;
import org.ikasan.designer.action.IgnoreSaveAndNavigateAction;
import org.ikasan.designer.action.IgnoreSaveAndNewAction;
import org.ikasan.designer.action.IgnoreSaveAndOpenAction;
import org.ikasan.designer.component.ColorPicker;
import org.ikasan.designer.component.SavePromptDialog;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.designer.function.ManageFunction;
import org.ikasan.designer.function.OpenFunction;
import org.ikasan.designer.function.SaveAsFunction;
import org.ikasan.designer.function.SaveFunction;
import org.ikasan.designer.pallet.DesignerPalletImageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Designer extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver
{
    Logger logger = LoggerFactory.getLogger(Designer.class);

    private DesignerCanvas designerCanvas;
    private Accordion toolAccordion = new Accordion();
    private List<ItemPallet> itemPalettes;
    private ColorPicker paintButton = new ColorPicker();
    private OpenFunction openFunction;
    private SaveFunction saveFunction;
    private SaveAsFunction saveAsFunction;
    private ManageFunction manageFunction;

    private String diagramId;
    private String diagramName;
    private String diagramDescription;

    private String dynamicImagePath;


    private boolean initialised = false;

    /**
     * Constructor
     */
    public Designer(OpenFunction openFunction, SaveFunction saveFunction
        , SaveAsFunction saveAsFunction, ManageFunction manageFunction,
                    String dynamicImagePath)
    {
        this.setMargin(false);
        this.setSpacing(false);

        this.setHeight("100%");
        this.setWidth("100%");

        this.openFunction = openFunction;
        this.saveFunction = saveFunction;
        this.saveAsFunction = saveAsFunction;
        this.manageFunction = manageFunction;

        this.dynamicImagePath = dynamicImagePath;

        this.itemPalettes = new ArrayList<>();
        init();
    }

    protected void init()
    {
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

        MenuItem file = menuBar.addItem(getTranslation("menu.file", UI.getCurrent().getLocale()));
        MenuItem edit = menuBar.addItem(getTranslation("menu.edit", UI.getCurrent().getLocale()));

        SubMenu fileSubMenu = file.getSubMenu();
        MenuItem newDiagram = fileSubMenu.addItem(getTranslation("menu.new", UI.getCurrent().getLocale()));
        newDiagram.addClickListener((ComponentEventListener<ClickEvent<MenuItem>>) menuItemClickEvent -> {
            if(!this.designerCanvas.isSaved()) {
                SavePromptDialog savePromptDialog = new SavePromptDialog(new IgnoreSaveAndNewAction(this.designerCanvas));
                savePromptDialog.open();
            }
            else {
                this.designerCanvas.clear();
            }
        });

        MenuItem open = fileSubMenu.addItem(getTranslation("menu.open", UI.getCurrent().getLocale()));
        open.addClickListener((ComponentEventListener<ClickEvent<MenuItem>>) menuItemClickEvent -> {
            if(!this.designerCanvas.isSaved()) {
                SavePromptDialog savePromptDialog = new SavePromptDialog(new IgnoreSaveAndOpenAction(this.openFunction
                    , this.designerCanvas));
                savePromptDialog.open();
            }
            else {
                this.openFunction.open(this.designerCanvas);
            }
        });

        fileSubMenu.add(new Hr());

        MenuItem save = fileSubMenu.addItem(getTranslation("menu.save", UI.getCurrent().getLocale()));
        save.addClickListener((ComponentEventListener<ClickEvent<MenuItem>>) menuItemClickEvent
            -> {
            if(this.openFunction.getId() != null) {
                this.diagramId = this.openFunction.getId();
                this.diagramName = this.openFunction.getName();
                this.diagramDescription = this.openFunction.getDescription();
                this.designerCanvas.save(this.diagramId, this.diagramName, this.diagramDescription);
            } else {
                this.designerCanvas.saveAs();
            }
        });

        MenuItem saveAs = fileSubMenu.addItem(getTranslation("menu.save-as", UI.getCurrent().getLocale()));
        saveAs.addClickListener((ComponentEventListener<ClickEvent<MenuItem>>) menuItemClickEvent -> this.designerCanvas.saveAs());

        fileSubMenu.add(new Hr());

        fileSubMenu.add(new Hr());

        MenuItem manage = fileSubMenu.addItem(getTranslation("menu.manage", UI.getCurrent().getLocale()));
        manage.addClickListener((ComponentEventListener<ClickEvent<MenuItem>>) menuItemClickEvent -> {
           this.manageFunction.open();
        });

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
        groupButton.getElement().setAttribute("title", getTranslation("tooltip.group-elements", UI.getCurrent().getLocale()));

        actions.add(groupButton);

        // Ungroup canvas items
        Button ungroupButton = new Button();
        ungroupButton.addClickListener(buttonClickEvent -> {
            this.designerCanvas.ungroup();
        });
        ungroupButton.getElement().appendChild(FontAwesome.Regular.OBJECT_UNGROUP.create().getElement());
        ungroupButton.getElement().setAttribute("title", getTranslation("tooltip.ungroup-elements", UI.getCurrent().getLocale()));

        actions.add(ungroupButton);

        // Bring selected items to front
        Button toFrontButton = new Button();
        toFrontButton.addClickListener(buttonClickEvent -> {
            this.designerCanvas.bringToFront();
        });
        toFrontButton.getElement().appendChild(IronIcons.FLIP_TO_FRONT.create().getElement());
        toFrontButton.getElement().setAttribute("title", getTranslation("tooltip.bring-to-front", UI.getCurrent().getLocale()));
        actions.add(toFrontButton);

        // Send selected items to back
        Button toBackButton = new Button();
        toBackButton.addClickListener(buttonClickEvent -> {
            this.designerCanvas.sendToBack();
        });
        toBackButton.getElement().appendChild(IronIcons.FLIP_TO_BACK.create().getElement());
        toBackButton.getElement().setAttribute("title", getTranslation("tooltip.send-to-back", UI.getCurrent().getLocale()));
        actions.add(toBackButton, getDivider());

        // Undo
        Button undoButton = new Button();
        undoButton.getElement().appendChild(IronIcons.UNDO.create().getElement());
        undoButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.undo());
        undoButton.getElement().setAttribute("title", getTranslation("tooltip.undo", UI.getCurrent().getLocale()));
        actions.add(undoButton);

        // Redo
        Button redoButton = new Button();
        redoButton.getElement().appendChild(IronIcons.REDO.create().getElement());
        redoButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.redo());
        redoButton.getElement().setAttribute("title", getTranslation("tooltip.redo", UI.getCurrent().getLocale()));
        actions.add(redoButton, getDivider());

        // Zoom in
        Button zoomInButton = new Button();
        zoomInButton.getElement().appendChild(IronIcons.ZOOM_IN.create().getElement());
        zoomInButton.setId("canvas_zoom_in");
        zoomInButton.getElement().setAttribute("title", getTranslation("tooltip.zoom-in", UI.getCurrent().getLocale()));
        actions.add(zoomInButton);

        // Zoom out
        Button zoomOutButton = new Button();
        zoomOutButton.getElement().appendChild(IronIcons.ZOOM_OUT.create().getElement());
        zoomOutButton.setId("canvas_zoom_out");
        zoomOutButton.getElement().setAttribute("title", getTranslation("tooltip.zoom-out", UI.getCurrent().getLocale()));
        actions.add(zoomOutButton, getDivider());

        // Copy
        Button copyButton = new Button();
        copyButton.getElement().appendChild(IronIcons.CONTENT_COPY.create().getElement());
        copyButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.designerCanvas.copy());
        copyButton.getElement().setAttribute("title", getTranslation("tooltip.copy", UI.getCurrent().getLocale()));
        actions.add(copyButton);

        // Paste
        Button pasteButton = new Button();
        pasteButton.getElement().appendChild(IronIcons.CONTENT_PASTE.create().getElement());
        pasteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.designerCanvas.paste());
        pasteButton.getElement().setAttribute("title", getTranslation("tooltip.paste", UI.getCurrent().getLocale()));
        actions.add(pasteButton);

        // Delete
        Button deleteButton = new Button();
        deleteButton.getElement().appendChild(IronIcons.DELETE.create().getElement());
        deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.designerCanvas.delete());
        deleteButton.getElement().setAttribute("title", getTranslation("tooltip.delete", UI.getCurrent().getLocale()));
        actions.add(deleteButton, getDivider());


        // Export as selected format
        Button download = new Button();
        download.getElement().appendChild(IronIcons.FILE_DOWNLOAD.create().getElement());
        download.getElement().setAttribute("title", getTranslation("tooltip.export-png", UI.getCurrent().getLocale()));
        actions.add(download);
        download.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.exportPng();
        });

        // Open another design
        Button open = new Button();
        open.getElement().appendChild(IronIcons.FOLDER_OPEN.create().getElement());
        open.getElement().setAttribute("title", getTranslation("tooltip.open-diagram", UI.getCurrent().getLocale()));
        actions.add(open);
        open.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            if(!this.designerCanvas.isSaved()) {
                SavePromptDialog savePromptDialog = new SavePromptDialog(new IgnoreSaveAndOpenAction(this.openFunction
                    , this.designerCanvas));
                savePromptDialog.open();
            }
            else {
                this.openFunction.open(this.designerCanvas);
            }
        });

        // Save current design
        Button save = new Button();
        save.getElement().appendChild(IronIcons.SAVE.create().getElement());
        save.getElement().setAttribute("title", getTranslation("tooltip.save-diagram", UI.getCurrent().getLocale()));
        actions.add(save, getDivider());
        save.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            if(this.openFunction.getId() != null) {
                this.diagramId = this.openFunction.getId();
                this.diagramName = this.openFunction.getName();
                this.diagramDescription = this.openFunction.getDescription();
                this.designerCanvas.save(this.diagramId, this.diagramName, this.diagramDescription);
            } else {
                this.designerCanvas.saveAs();
            }
        });

        paintButton = new ColorPicker();
        paintButton.addValueChangeListener((HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>)
            textFieldStringComponentValueChangeEvent -> {
                this.designerCanvas.setBackgroundColor(textFieldStringComponentValueChangeEvent.getValue());
            });
        paintButton.getElement().setAttribute("title", getTranslation("tooltip.choose-colour", UI.getCurrent().getLocale()));
        actions.add(paintButton, getDivider());

        return actions;
    }

    protected void initBase() {
        this.designerCanvas = new DesignerCanvas(this.saveFunction, this.saveAsFunction, "canvas-wrapper", this.dynamicImagePath, false);
        this.designerCanvas.setSizeUndefined();

        DropTarget<DesignerCanvas> dropTarget = DropTarget.create(this.designerCanvas);

        dropTarget.addDropListener(event -> {
            // move the dragged component to inside the drop target component
            event.getDragSourceComponent().ifPresent(action -> ((DesignerPalletImageItem)action).executeCanvasAddAction());
        });

        paintButton = new ColorPicker();
        paintButton.addValueChangeListener((HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>)
            textFieldStringComponentValueChangeEvent -> {
                this.designerCanvas.setBackgroundColor(textFieldStringComponentValueChangeEvent.getValue());
            });
    }

    public void addItemToCanvas(DesignerPalletImageItem item) {
        designerCanvas.addPalletItem(item);
        switch(item.getDesignerPalletItemType()) {
            case ICON:
                designerCanvas.addIcon(item.getIdentifier().toString(),
                    item.getSrc(), item.getItemHeight(),
                    item.getItemWidth(), false);
                break;
            case RECTANGLE:
                designerCanvas.addBoundary(item.getItemHeight(), item.getItemWidth());
                break;
            case TRIANGLE:
                designerCanvas.addTriangleBoundary(item.getItemHeight(), item.getItemWidth());
                break;
            case OVAL:
                designerCanvas.addOval(item.getItemHeight(),item.getItemWidth());
                break;
            case CIRCLE:
                designerCanvas.addCircle(item.getItemHeight());
                break;
            case LABEL:
                designerCanvas.addLabel(getTranslation("label.double-click-to-edit", UI.getCurrent().getLocale()));
                break;
        }
    }

    public void addLabelToItem(DesignerPalletImageItem item, String label) {
        designerCanvas.addLabelToFigure(item.getIdentifier().toString(), label);
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
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent beforeLeaveEvent) {
        if(!this.designerCanvas.isSaved()) {
            BeforeLeaveEvent.ContinueNavigationAction action = beforeLeaveEvent.postpone();

            SavePromptDialog savePromptDialog = new SavePromptDialog(new IgnoreSaveAndNavigateAction(action));
            savePromptDialog.open();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
    }

    public void setFont(String font) {
        this.designerCanvas.setFont(font);
    }

    public void setLineTargetDecorator(String decorator) {
        this.designerCanvas.setLineTargetDecorator(decorator);
    }

    public void setLineSourceDecorator(String decorator) {
        this.designerCanvas.setLineSourceDecorator(decorator);
    }

    public void setFontSize(String fontSize) {
        this.designerCanvas.setFontSize(fontSize);
    }

    public void setLineType(String pattern) {
        if(pattern.equals("EMPTY")){
            pattern = "";
        }
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

