package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.visualisation.component.ModuleControlContextMenu;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ScheduledContextDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextHelper;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.dashboard.ui.visualisation.util.BusinessStreamItemTypes;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SchedulerVisualisation extends VerticalLayout implements BeforeEnterObserver, CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener {
    private Logger logger = LoggerFactory.getLogger(SchedulerVisualisation.class);

    private Registration contextInstanceStateChangeRegistration;

    private DesignerCanvas designerCanvas;

    private String dynamicImagePath;

    private ContextInstance contextInstance;

    private boolean initialised = false;

    private ScheduledContextDraw2dAdapter adapter = new ScheduledContextDraw2dAdapter();

    public SchedulerVisualisation(String dynamicImagePath) {

        this.dynamicImagePath = dynamicImagePath;
        if (this.dynamicImagePath == null) {
            throw new IllegalArgumentException("dynamicImagePath cannot be null!");
        }

        this.setMargin(false);
        this.setSpacing(false);
        this.setSizeFull();
    }

    /**
     * @param contextInstance
     */
    public void createSchedulerVisualisation(ContextInstance contextInstance) throws IOException {
        this.contextInstance = contextInstance;
        this.initialised = false;
        init();
    }

    private void init() throws IOException{
        if(!initialised) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas("canvas-viewport", this.dynamicImagePath, true);
            this.designerCanvas.setCanvasJson(adapter.adaptContext(contextInstance));
            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);

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

    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {

        if(canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            ContextInstance contextInstance = ContextHelper.getChildContextInstance(canvasItemDoubleClickEvent.getFigure().getIdentifier(),
                this.contextInstance);

            if(contextInstance.getScheduledJobs() != null) {
                try {
                    JobVisualisationDialog jobVisualisationDialog = new JobVisualisationDialog();
                    jobVisualisationDialog.createSchedulerVisualisation(contextInstance);
                    jobVisualisationDialog.open();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if(this.designerCanvas != null){
            this.redraw();
        }

        UI ui = attachEvent.getUI();
        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if(contextInstanceStateChangeEvent.getContextInstance() != null) {
                logger.info("Updating scheduler visualisation context status. Context Instance[{}], Status[{}], Status Colour[{}]",
                    contextInstanceStateChangeEvent.getContextInstance().getName(), contextInstanceStateChangeEvent.getContextInstance().getStatus().toString(),
                    StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getContextInstance().getStatus()));
                ui.access(() ->
                    this.designerCanvas.setBackgroundColor(contextInstanceStateChangeEvent.getContextInstance().getName()
                        , StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getContextInstance().getStatus())));
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.contextInstanceStateChangeRegistration.remove();
        this.contextInstanceStateChangeRegistration = null;
    }
}
