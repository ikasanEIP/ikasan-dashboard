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
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ScheduledContextDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemDoubleClickEventListener;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemRightClickEventListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JobVisualisationDialog extends AbstractCloseableResizableDialog implements CanvasItemRightClickEventListener
    , CanvasItemDoubleClickEventListener {

    private Logger logger = LoggerFactory.getLogger(JobVisualisationDialog.class);

    private Registration schedulerJobStateChangeRegistration;

    private DesignerCanvas designerCanvas;
    private VerticalLayout layout;

    private ScheduledContextDraw2dAdapter adapter = new ScheduledContextDraw2dAdapter();

    private boolean initialised = false;

    private ContextInstance contextInstance;

    private String dynamicImagePath = ".";

    public JobVisualisationDialog() {
        this.setHeight("90%");
        this.setWidth("90%");

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
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

            this.designerCanvas = new DesignerCanvas("job-viewport", this.dynamicImagePath, true);
            this.designerCanvas.setCanvasJson(adapter.adaptJobs(contextInstance));
            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);

            this.designerCanvas.manageClickableItems();

            this.layout.add(initCanvasActions(), designerCanvas);

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

    public void exportPng(){
        this.designerCanvas.exportPng();
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        logger.info(canvasItemDoubleClickEvent.toString());
        SchedulerJobLogFileViewerDialog dialog = new SchedulerJobLogFileViewerDialog();
        dialog.open();
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {
        logger.info(canvasItemRightClickEvent.toString());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if(contextInstanceStateChangeEvent.getSchedulerJobInstance() != null) {
                logger.info("Updating scheduler visualisation job status. Scheduler Job Instance[{}], Status[{}], Status Colour[{}]",
                    contextInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier(), contextInstanceStateChangeEvent.getSchedulerJobInstance().getStatus().toString(),
                    StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getSchedulerJobInstance().getStatus()));
                ui.access(() ->
                    this.designerCanvas.setBackgroundColor(contextInstanceStateChangeEvent.getSchedulerJobInstance().getIdentifier()
                        , StatusColours.getInstanceStatusColour(contextInstanceStateChangeEvent.getSchedulerJobInstance().getStatus())));
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulerJobStateChangeRegistration.remove();
        this.schedulerJobStateChangeRegistration = null;
    }
}
