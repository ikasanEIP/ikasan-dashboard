package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.scheduler.component.ContextInstanceViewMenuBar;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.designer.CanvasInitialisedListener;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.model.UserData;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;

import java.io.IOException;
import java.util.UUID;

public class ContextSchedulerInstanceVisualisation extends SchedulerInstanceVisualisation implements CanvasInitialisedListener {

    private ContextProfileService contextProfileService;

    /**
     * Constructor
     *
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param logStreamingService
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param jobUtilsService
     * @param scheduledContextService
     */
    public ContextSchedulerInstanceVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                                 ScheduledProcessManagementService scheduledProcessManagementService,
                                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                                 LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                                 JobInitiationService jobInitiationService, JobUtilsService jobUtilsService,
                                                 ScheduledContextService scheduledContextService, ContextProfileService contextProfileService) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService,
            systemEventLogger, logStreamingService, schedulerJobInstanceService,
            jobInitiationService, jobUtilsService, scheduledContextService);
        this.contextProfileService = contextProfileService;
    }

    /**
     * Initialise the internals of this class.
     *
     * @throws IOException
     */
    protected void init() throws IOException {
        if(!initialised && contextInstance != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas("canvas-viewport-"+ UUID.randomUUID().toString(),
                this.dynamicImagePath, true);

            if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
                this.designerCanvas.setCanvasJson(adapter.adaptContext(contextInstance));
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addCanvasInitialisedListener(this);

            this.designerCanvas.manageClickableItems();

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        super.doubleClickEvent(canvasItemDoubleClickEvent);
        if(canvasItemDoubleClickEvent.getFigure() != null
            && canvasItemDoubleClickEvent.getFigure().getUserData() != null
            && canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier() != null
            && canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
            String identifier = ContextHelper.getIdentifier(canvasItemDoubleClickEvent.getFigure()
                .getUserData().getIdentifier());

            this.addBoundaryToItem(identifier, false);
        }
    }

    @Override
    protected Component initCanvasActions() {
        HorizontalLayout actions = (HorizontalLayout) super.initCanvasActions();
        MenuBar contextViewsMenuBar = new ContextInstanceViewMenuBar(this.contextInstance, this.contextProfileService
            , this);
        contextViewsMenuBar.getStyle().set("right", "40px");

        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setMargin(false);
        wrapper.setSpacing(false);
        wrapper.setPadding(false);
        wrapper.add(contextViewsMenuBar);
        wrapper.setHorizontalComponentAlignment(Alignment.END, contextViewsMenuBar);

        actions.add(wrapper);

        return actions;
    }
}
