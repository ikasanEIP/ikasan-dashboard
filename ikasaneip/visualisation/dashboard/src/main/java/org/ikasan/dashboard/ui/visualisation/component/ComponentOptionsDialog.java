package org.ikasan.dashboard.ui.visualisation.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.model.flow.AbstractWiretapNode;
import org.ikasan.dashboard.ui.visualisation.model.flow.FlowItemTypes;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.dashboard.ui.visualisation.model.flow.NodeFoundStatus;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.rest.client.dto.TriggerDto;
import org.ikasan.rest.client.util.UserUtil;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.TriggerService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.trigger.TriggerRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComponentOptionsDialog extends AbstractCloseableResizableDialog {
    private static Logger logger = LoggerFactory.getLogger(ComponentOptionsDialog.class);

    protected ConfigurationService configurationRestService;

    protected TriggerService triggerRestService;

    protected Module module;

    protected String flowName;

    protected String componentName;

    protected AbstractWiretapNode abstractWiretapNode;

    private MetaDataService metaDataApplicationRestService;

    private BatchInsert<ModuleMetaData> moduleMetaDataService;

    protected boolean configuredResource;

    protected DesignerCanvas designerCanvas;

    protected ComponentOptionsDialog(Module module, String flowName, String componentName, boolean configuredResource,
                                     ConfigurationService configurationRestService,
                                     TriggerService triggerRestService, /*NetworkDiagram networkDiagram,*/
                                     AbstractWiretapNode abstractWiretapNode,
                                     MetaDataService metaDataApplicationRestService,
                                     BatchInsert<ModuleMetaData> moduleMetaDataService,
                                     DesignerCanvas designerCanvas) {
        this.module = module;
        this.flowName = flowName;
        this.componentName = componentName;
        this.configurationRestService = configurationRestService;
        this.configuredResource = configuredResource;
        this.triggerRestService = triggerRestService;
        this.abstractWiretapNode = abstractWiretapNode;
        this.metaDataApplicationRestService = metaDataApplicationRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.designerCanvas = designerCanvas;
        showResize(false);
        init();
    }

    private void init() {
        VerticalLayout verticalLayout = new VerticalLayout();

        Image mrSquidImage = new Image("/frontend/images/mr-squid-head.png", "");
        mrSquidImage.setHeight("35px");

        H3 componentOptions = new H3(
            String.format(getTranslation("label.component-options", UI.getCurrent().getLocale())));

        HorizontalLayout header = new HorizontalLayout();
        header.add(mrSquidImage, componentOptions);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, mrSquidImage, componentOptions);

        verticalLayout.add(header);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, header);

        if (this.configuredResource) {
            Button componentConfigurationButton = new Button(
                getTranslation("button.component-configuration", UI.getCurrent().getLocale()));
            componentConfigurationButton.setWidthFull();
            componentConfigurationButton.addClickListener((ComponentEventListener<ClickEvent<Button>>)
                buttonClickEvent -> openComponentConfiguration());

            verticalLayout.add(componentConfigurationButton);

            ComponentSecurityVisibility.applySecurity(componentConfigurationButton, SecurityConstants.ALL_AUTHORITY
                , SecurityConstants.PLATFORM_CONFIGURATION_ADMIN
                , SecurityConstants.PLATFORM_CONFIGURATION_READ
                , SecurityConstants.PLATFORM_CONFIGURATION_WRITE);
        }

        Button invokerConfigurationButton = new Button(
            getTranslation("button.invoker-configuration", UI.getCurrent().getLocale()));
        invokerConfigurationButton.setWidthFull();
        invokerConfigurationButton.addClickListener((ComponentEventListener<ClickEvent<Button>>)
            buttonClickEvent -> openInvokerConfiguration());

        verticalLayout.add(invokerConfigurationButton);

        ComponentSecurityVisibility.applySecurity(invokerConfigurationButton, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.PLATFORM_CONFIGURATION_ADMIN
            , SecurityConstants.PLATFORM_CONFIGURATION_READ
            , SecurityConstants.PLATFORM_CONFIGURATION_WRITE);

        Button createWiretapBeforeComponentWithTTLOneDayButton = new Button(
            getTranslation("button.wiretap-before-component-oneday", UI.getCurrent().getLocale()));
        createWiretapBeforeComponentWithTTLOneDayButton.setWidthFull();
        createWiretapBeforeComponentWithTTLOneDayButton.addClickListener(
            (ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> createWiretapWithTTLOneDay(TriggerRelationship.BEFORE.getDescription()));
        verticalLayout.add(createWiretapBeforeComponentWithTTLOneDayButton);

        ComponentSecurityVisibility.applySecurity(createWiretapBeforeComponentWithTTLOneDayButton, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.WIRETAP_WRITE
            , SecurityConstants.WIRETAP_ADMIN
            , SecurityConstants.WIRETAP_ALL_MODULES_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_ADMIN);

        Button createWiretapAfterComponentWithTTLOneDayButton = new Button(
            getTranslation("button.wiretap-after-component-oneday", UI.getCurrent().getLocale()));
        createWiretapAfterComponentWithTTLOneDayButton.setWidthFull();
        createWiretapAfterComponentWithTTLOneDayButton.addClickListener(
            (ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> createWiretapWithTTLOneDay(TriggerRelationship.AFTER.getDescription()));
        verticalLayout.add(createWiretapAfterComponentWithTTLOneDayButton);

        ComponentSecurityVisibility.applySecurity(createWiretapAfterComponentWithTTLOneDayButton, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.WIRETAP_ADMIN
            , SecurityConstants.WIRETAP_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_ADMIN);

        Button createLogBeforeComponentButton = new Button(
            getTranslation("button.log-before-component", UI.getCurrent().getLocale()));
        createLogBeforeComponentButton.setWidthFull();
        createLogBeforeComponentButton
            .addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> createLog(TriggerRelationship.BEFORE.getDescription()));
        verticalLayout.add(createLogBeforeComponentButton);

        ComponentSecurityVisibility.applySecurity(createLogBeforeComponentButton, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.WIRETAP_ADMIN
            , SecurityConstants.WIRETAP_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_ADMIN);

        Button createLogAfterComponentButton = new Button(
            getTranslation("button.log-after-component", UI.getCurrent().getLocale()));
        createLogAfterComponentButton.setWidthFull();
        createLogAfterComponentButton
            .addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> createLog(TriggerRelationship.AFTER.getDescription()));
        verticalLayout.add(createLogAfterComponentButton);

        ComponentSecurityVisibility.applySecurity(createLogAfterComponentButton, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.WIRETAP_ADMIN
            , SecurityConstants.WIRETAP_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_ADMIN);


        super.content.add(verticalLayout);
        this.setHeight("550px");
        this.setWidth("550px");
    }

    private void openComponentConfiguration() {
        ComponentConfigurationDialog componentConfigurationDialog = new ComponentConfigurationDialog(module,
            flowName, componentName, configurationRestService
        );

        this.close();
        componentConfigurationDialog.open();
    }

    private void openInvokerConfiguration() {
        InvokerConfigurationDialog componentConfigurationDialog = new InvokerConfigurationDialog(module, flowName,
            componentName, configurationRestService
        );

        this.close();
        componentConfigurationDialog.open();
    }

    private void createWiretapWithTTLOneDay(String relationship) {
        createTrigger(relationship, "wiretapJob", "720");
    }

    private void createLog(String relationship) {
        createTrigger(relationship, "loggingJob", null);
    }

    private void createTrigger(String relationship, String job, String ttl) {
        TriggerDto triggeDto = new TriggerDto(this.module.getName(), this.flowName, this.componentName, relationship,
            job, ttl, UserUtil.getUser());
        boolean success = this.triggerRestService.create(this.module.getUrl(), triggeDto);
        if (success) {
            try {
                Optional<ModuleMetaData> moduleMetaDataOptional = this.metaDataApplicationRestService.getModuleMetadata(module.getUrl(), module.getName());

                moduleMetaDataOptional.ifPresent(moduleMetaData -> {

                    moduleMetaData.getFlows().stream().filter(flow -> flowName.equals(flow.getName())).findFirst().ifPresent(flowMetaData -> {
                        logger.info(flowMetaData.toString());

                        flowMetaData.getFlowElements().stream()
                            .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals(this.componentName))
                            .findFirst().ifPresent(decorators -> {
                                this.abstractWiretapNode.setDecoratorMetaDataList(decorators.getDecorators());
                            });
                    });

                    List<ModuleMetaData> entities = new ArrayList<>();
                    entities.add(moduleMetaData);

                    this.moduleMetaDataService.insert(entities);
                });

                this.updateDiagramState(job, relationship);
                NotificationHelper
                    .showUserNotification(getTranslation("message.wiretap-save-successful", UI.getCurrent().getLocale()));
            }
            catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        } else {
            NotificationHelper.showErrorNotification(
                getTranslation("message.wiretap-save-unsuccessful", UI.getCurrent().getLocale()));
        }

        this.close();
    }

    private void updateDiagramState(String job, String relationship) throws JsonProcessingException {
        if (job.equals("wiretapJob")) {
            if (relationship.equals(TriggerRelationship.AFTER.getDescription())) {
                ImageBuilder wiretapBuilder = new ImageBuilder()
                    .withId(this.abstractWiretapNode.getId().getUuid() + "after-wiretap")
                    .withWidth(this.abstractWiretapNode.getWiretapAfterImageW())
                    .withHeight(this.abstractWiretapNode.getWiretapAfterImageH())
                    .withX(this.abstractWiretapNode.getX() + this.abstractWiretapNode.getWiretapAfterImageX())
                    .withY(this.abstractWiretapNode.getY() + this.abstractWiretapNode.getWiretapAfterImageY())
                    .withSelectable(true)
                    .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.BEFORE_WIRETAP)
                        .withIdentifier(this.abstractWiretapNode.getDecoratorMetaDataList().stream()
                            .filter(decoratorMetaData -> decoratorMetaData.getType().equals("Wiretap")
                                && decoratorMetaData.getName().startsWith("AFTER")).findFirst().get().getConfigurationId())
                        .withComponentName(this.abstractWiretapNode.getName())
                        .build())
                    .withPath(AbstractWiretapNode.WIRETAP_IMAGE);
                abstractWiretapNode.setWiretapAfterStatus(NodeFoundStatus.FOUND);

                this.designerCanvas.addImageFigureWithXYOfImageProvided(new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(wiretapBuilder.build()));
            } else if (relationship.equals(TriggerRelationship.BEFORE.getDescription())) {
                ImageBuilder wiretapBuilder = new ImageBuilder()
                    .withId(this.abstractWiretapNode.getId().getUuid() + "before-wiretap")
                    .withWidth(this.abstractWiretapNode.getWiretapBeforeImageW())
                    .withHeight(this.abstractWiretapNode.getWiretapBeforeImageH())
                    .withX(this.abstractWiretapNode.getX() + this.abstractWiretapNode.getWiretapBeforeImageX())
                    .withY(this.abstractWiretapNode.getY() + this.abstractWiretapNode.getWiretapBeforeImageY())
                    .withSelectable(true)
                    .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.BEFORE_WIRETAP)
                        .withIdentifier(this.abstractWiretapNode.getDecoratorMetaDataList().stream()
                            .filter(decoratorMetaData -> decoratorMetaData.getType().equals("Wiretap")
                                && decoratorMetaData.getName().startsWith("BEFORE")).findFirst().get().getConfigurationId())
                        .withComponentName(this.abstractWiretapNode.getName())
                        .build())
                    .withPath(AbstractWiretapNode.WIRETAP_IMAGE);
                abstractWiretapNode.setWiretapAfterStatus(NodeFoundStatus.FOUND);

                this.designerCanvas.addImageFigureWithXYOfImageProvided(new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(wiretapBuilder.build()));
            }
        } else if (job.equals("loggingJob")) {
            if (relationship.equals(TriggerRelationship.AFTER.getDescription())) {
                ImageBuilder wiretapBuilder = new ImageBuilder()
                    .withId(this.abstractWiretapNode.getId().getUuid() + "after-log-wiretap")
                    .withWidth(this.abstractWiretapNode.getLogWiretapAfterImageW())
                    .withHeight(this.abstractWiretapNode.getLogWiretapAfterImageH())
                    .withX(this.abstractWiretapNode.getX() + this.abstractWiretapNode.getLogWiretapAfterImageX())
                    .withY(this.abstractWiretapNode.getY() + this.abstractWiretapNode.getLogWiretapAfterImageY())
                    .withSelectable(true)
                    .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.BEFORE_WIRETAP)
                        .withIdentifier(this.abstractWiretapNode.getDecoratorMetaDataList().stream()
                            .filter(decoratorMetaData -> decoratorMetaData.getType().equals("LogWiretap")
                                && decoratorMetaData.getName().startsWith("AFTER")).findFirst().get().getConfigurationId())
                        .withComponentName(this.abstractWiretapNode.getName())
                        .build())
                    .withPath(AbstractWiretapNode.LOG_WIRETAP_IMAGE);
                abstractWiretapNode.setWiretapAfterStatus(NodeFoundStatus.FOUND);

                this.designerCanvas.addImageFigureWithXYOfImageProvided(new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(wiretapBuilder.build()));
            } else if (relationship.equals(TriggerRelationship.BEFORE.getDescription())) {
                ImageBuilder wiretapBuilder = new ImageBuilder()
                    .withId(this.abstractWiretapNode.getId().getUuid() + "before-log-wiretap")
                    .withWidth(this.abstractWiretapNode.getLogWiretapBeforeImageW())
                    .withHeight(this.abstractWiretapNode.getLogWiretapBeforeImageH())
                    .withX(this.abstractWiretapNode.getX() + this.abstractWiretapNode.getLogWiretapBeforeImageX())
                    .withY(this.abstractWiretapNode.getY() + this.abstractWiretapNode.getLogWiretapBeforeImageY())
                    .withSelectable(true)
                    .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.BEFORE_WIRETAP)
                        .withIdentifier(this.abstractWiretapNode.getDecoratorMetaDataList().stream()
                            .filter(decoratorMetaData -> decoratorMetaData.getType().equals("LogWiretap")
                                && decoratorMetaData.getName().startsWith("BEFORE")).findFirst().get().getConfigurationId())
                        .withComponentName(this.abstractWiretapNode.getName())
                        .build())
                    .withPath(AbstractWiretapNode.LOG_WIRETAP_IMAGE);
                abstractWiretapNode.setWiretapAfterStatus(NodeFoundStatus.FOUND);

                this.designerCanvas.addImageFigureWithXYOfImageProvided(new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(wiretapBuilder.build()));
            }
        }
    }
}
