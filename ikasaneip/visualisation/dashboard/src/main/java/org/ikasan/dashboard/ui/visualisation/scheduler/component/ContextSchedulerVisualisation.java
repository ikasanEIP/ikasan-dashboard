package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.ikasan.dashboard.ui.scheduler.listener.NewContextListener;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonToContextTemplateAdapter;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.ConnectorEvent;
import org.ikasan.designer.event.ConnectorEventListener;
import org.ikasan.designer.event.FigureMovedEvent;
import org.ikasan.designer.event.FigureMovedEventListener;
import org.ikasan.designer.model.UserData;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class ContextSchedulerVisualisation extends SchedulerVisualisation implements FigureMovedEventListener
    , ConnectorEventListener, NewContextListener {

    private Logger logger = LoggerFactory.getLogger(ContextSchedulerVisualisation.class);

    /**
     * Constructs a ContextSchedulerVisualisation object with the provided parameters.
     *
     * @param dynamicImagePath the dynamic image path
     * @param moduleMetaDataService service for module metadata
     * @param scheduledProcessManagementService service for scheduled process management
     * @param configurationRestService configuration REST service
     * @param moduleControlRestService module control REST service
     * @param metaDataRestService metadata REST service
     * @param systemEventLogger logger for system events
     * @param schedulerJobService job scheduler service
     * @param logStreamingService streaming service for log data
     * @param jobInitiationService service for job initiation
     * @param contextProfileService context profile service
     * @param userService user service
     * @param securityService security service
     * @param jobProvisionService job provision service
     * @param scheduledContextService service for scheduled contexts
     * @param schedulerJobExecutionEnvironmentLabel map of scheduler job execution environment labels
     * @param jobVisualisationVerticalSpacing vertical spacing for job visualisation
     * @param jobVisualisationHorizontalSpacing horizontal spacing for job visualisation
     * @param contextVisualisationLevelDistance distance between context visualisation levels
     * @param contextVisualisationNodeDistance distance between context visualisation nodes
     */
    public ContextSchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService
        , ScheduledProcessManagementService scheduledProcessManagementService, ConfigurationService configurationRestService
        , ModuleControlService moduleControlRestService, MetaDataService metaDataRestService, SystemEventLogger systemEventLogger
        , SchedulerJobService schedulerJobService, LogStreamingService logStreamingService, JobInitiationService jobInitiationService
        , ContextProfileService contextProfileService, UserService userService, SecurityService securityService
        , JobProvisionService jobProvisionService, ScheduledContextService scheduledContextService
        , Map<String, String> schedulerJobExecutionEnvironmentLabel, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing
        , double contextVisualisationLevelDistance, double contextVisualisationNodeDistance, boolean showPrettyFormattedDiagram) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService
            , moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService
            , jobInitiationService, contextProfileService, userService, securityService, jobProvisionService, scheduledContextService
            , schedulerJobExecutionEnvironmentLabel, jobVisualisationVerticalSpacing
            , jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance
            , showPrettyFormattedDiagram);
    }

    /**
     * Initializes the DesignerCanvas with the specified UI object.
     *
     * @param ui the UI object used for initialization
     * @throws IOException if an I/O error occurs
     */
    protected void init(UI ui) throws IOException {
        if(!initialised && contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas(this, null, "canvas-viewport-"+ UUID.randomUUID().toString()
                , this.dynamicImagePath, !this.edit, ui, true);
            this.designerCanvas.addCanvasInitialisedListener(this);

            this.designerCanvas.setCanvasJson(adapter.adaptContext(contextTemplate));

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addConnectorEventListener(this);
            this.designerCanvas.addCanvasUpdatedListener(this);
            this.designerCanvas.addFigureDeleteEventListeners(this);
            this.designerCanvas.addFigureUndoDeleteEventListeners(this);
            this.designerCanvas.addFigureMovedEventListeners(this);

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

    @Override
    public void save(String id, String name, String description, String payload) {
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        super.parentContextTemplate = adapter.adaptParentContextTemplate(super.parentContextTemplate, payload);

        super._save();
    }

    @Override
    public void newContext(ContextTemplate context) {
        if(ContextHelper.getChildContextTemplate(context.getName(), this.parentContextTemplate) != null) {
            ConfirmDialog errorDialog = new ConfirmDialog();
            errorDialog.setHeader(getTranslation("error-dialog-header.cannot-add-context", UI.getCurrent().getLocale()));
            errorDialog.setWidth("500px");

            StringBuffer message = new StringBuffer();
            message.append("<p style=\"color:red\">" + getTranslation("error-dialog-body.cannot-add-context", UI.getCurrent().getLocale()) +
                "</p>");
            errorDialog.setText(new Html("<div>"+message.toString()+"</div>"));
            errorDialog.setConfirmText(getTranslation("button.ok"));
            errorDialog.setCancelText(getTranslation("button.cancel"));
            errorDialog.open();
            return;
        }

        this.contextTemplate = ContextHelper.getChildContextTemplate(this.contextTemplate.getName(), this.parentContextTemplate);
        this.contextTemplate.getContexts().add(context);
        this.contextTemplate.getContextsMap().put(context.getName(), context);

        this.designerCanvas.addImageFigure(adapter.adaptChildContext(context));
        this.designerCanvas.addLabelToFigure(context.getName(), context.getName());
        this.designerCanvas.manageClickableItems();

        if(this.edit) {
            this.designerCanvas.save("", "", "");
            this.systemEventLogger.logEvent(SystemEventConstants.CHILD_JOB_PLAN_ADDED_TO_JOB_PLAN, String.format("Child job plan [%s], has been added to job plan [%s]"
                , context.getName(), parentContextTemplate.getName()), this.authentication.getName());
        }
    }

    @Override
    public void connectorEvent(ConnectorEvent connectorEvent) {
        logger.debug("Connector event - " + connectorEvent.getEventType());
        if(connectorEvent.getEventType().equals("CONNECTOR_ADDED")) {
            if(connectorEvent.getSourceUserData() != null && connectorEvent.getSourceUserData().getItemType() != null
                && connectorEvent.getSourceUserData().getItemType().equals(UserData.CONTEXT)
                && connectorEvent.getTargetUserData() != null && connectorEvent.getTargetUserData().getItemType() != null
                && connectorEvent.getTargetUserData().getItemType().equals(UserData.CONTEXT)) {
                ContextTemplate childContextTemplate = ContextHelper.getChildContextTemplate(connectorEvent.getTargetUserData().getContextName()
                    , this.parentContextTemplate);
                ContextHelper.removeChildContextTemplate(connectorEvent.getTargetUserData().getContextName(), this.parentContextTemplate);

                ContextTemplate contextTemplate = ContextHelper.getChildContextTemplate(connectorEvent.getSourceUserData().getContextName(), this.parentContextTemplate);

                contextTemplate.getContexts().add(childContextTemplate);
                contextTemplate.getContextsMap().put(childContextTemplate.getName(),childContextTemplate);

                if(this.edit) {
                    this.designerCanvas.save("", "", "");
                }
            }
        }
    }

    @Override
    public void figureMoved(FigureMovedEvent figureMovedEvent) {
        logger.info("Figure moved - " + figureMovedEvent.getFigure());
        if(this.edit) {
            this.designerCanvas.save("", "", "");
        }
    }
}
