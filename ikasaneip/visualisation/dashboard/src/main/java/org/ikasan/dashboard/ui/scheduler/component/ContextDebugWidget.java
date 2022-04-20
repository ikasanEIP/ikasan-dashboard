package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


@CssImport("./styles/dashboard-view.css")
public class ContextDebugWidget extends Div {

    Logger logger = LoggerFactory.getLogger(ContextDebugWidget.class);

    private ScheduledContextService scheduledContextService;
    private JobLockCacheService jobLockCacheService;

    protected AceEditor aceEditor;
    protected SchedulerVisualisation schedulerVisualisation;
    private ContextInstanceAuditWidget contextInstanceAuditWidget;

    private Tab fullContextInstance;
    private Tab contextStatus;
    private Tab contextEvents;
    private Tab visualisation;
    private Tab contextAudit;
    private Tabs tabs;

    private Select<String> contextInstances;
    private ObjectMapper objectMapper;

    private StringBuffer events = new StringBuffer();

    private InternalEventDrivenJobService internalEventDrivenJobService;
    private String queueDir;

    private UI ui;

    /**
     * Constructor
     */
    public ContextDebugWidget(ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService,
                              ScheduledContextService scheduledContextService, SystemEventLogger systemEventLogger,
                              InternalEventDrivenJobService internalEventDrivenJobService, String queueDir,
                              ModuleMetaDataService moduleMetaDataService, JobLockCacheService jobLockCacheService,
                              ScheduledContextInstanceService contextInstanceService, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                              SchedulerJobService schedulerJobService, LogStreamingService logStreamingService) {
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("100%");

        this.scheduledContextService = scheduledContextService;
        this.jobLockCacheService = jobLockCacheService;
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        this.queueDir = queueDir;

        this.schedulerVisualisation = new SchedulerVisualisation(".", moduleMetaDataService, scheduledProcessManagementService, configurationRestService,
            moduleControlRestService,  metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);

        this.schedulerVisualisation.setWidthFull();
        this.schedulerVisualisation.setHeight("1000px");
        this.schedulerVisualisation.setVisible(false);

        this.contextInstanceAuditWidget = new ContextInstanceAuditWidget(contextInstanceService);
        this.contextInstanceAuditWidget.setVisible(false);

        this.initialiseEditor();

        this.objectMapper = new ObjectMapper();

        this.ui = UI.getCurrent();

        this.contextInstances = new Select<>();
        this.contextInstances.setLabel("Context Instance");
        this.contextInstances.addValueChangeListener(listener -> {
            try {
                ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());

                if(contextMachine != null) {
                    this.schedulerVisualisation.createSchedulerVisualisation(contextMachine.getContext());
                }
                else {
                    return;
                }
                if(tabs.getSelectedTab().equals(this.fullContextInstance)) {
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                }
                else if(tabs.getSelectedTab().equals(this.contextStatus)) {
                    this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                }
            }
            catch (JsonProcessingException e){
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button addContextButton = new Button("Add Context");
        addContextButton.addClickListener(buttonClickEvent -> {
            ContextUploadDialog contextUploadDialog = new ContextUploadDialog(scheduledContextInstanceService,
                schedulerService, this.scheduledContextService, this.internalEventDrivenJobService, this.queueDir, moduleMetaDataService, this.jobLockCacheService);
            contextUploadDialog.open();

            contextUploadDialog.addOpenedChangeListener(event -> {
                if(!event.isOpened()){
                    this.contextInstances.setItems(ContextMachineCache.instance().contextNames());
                    this.contextInstances.getDataProvider().refreshAll();
                }
            });
        });

//        Button newContextButton = new Button("New Context");
//        newContextButton.addClickListener(buttonClickEvent -> {
//            ContextDialog contextUploadDialog = new ContextDialog(systemEventLogger,
//                this.scheduledContextService);
//            contextUploadDialog.open();
//
//            contextUploadDialog.addOpenedChangeListener(event -> {
//                if(!event.isOpened()){
//                    this.contextInstances.removeAll();
//                    this.contextInstances.setItems(ContextMachineCache.instance().contextNames());
//                }
//            });
//        });

        Button resetContextButton = new Button("Reset Context");
        resetContextButton.addClickListener(buttonClickEvent -> {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());
            try {
                contextMachine.resetContextInstance();
                this.schedulerVisualisation.createSchedulerVisualisation(contextMachine.getContext());
                if(tabs.getSelectedTab().equals(this.fullContextInstance)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                    this.aceEditor.setVisible(true);
                }
                else if(tabs.getSelectedTab().equals(this.contextStatus)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                }
                else if(tabs.getSelectedTab().equals(this.contextEvents)) {
                    this.aceEditor.setValue(this.events.toString());
                }
                else if(tabs.getSelectedTab().equals(this.visualisation)) {
                   // do something
                }
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });


        HorizontalLayout controlsLayout = new HorizontalLayout();
        controlsLayout.add(this.contextInstances, addContextButton, resetContextButton);

        this.fullContextInstance = new Tab("Full Context Instance");
        this.contextStatus = new Tab("Context Instance Status");
        this.contextEvents = new Tab("Context Instance Events");
        this.visualisation = new Tab("Context Instance Visualisation");
        this.contextAudit = new Tab("Context Instance Audit");
        this.tabs = new Tabs(fullContextInstance, contextStatus, contextEvents, visualisation, contextAudit);

        tabs.addSelectedChangeListener(event -> {
            try {
                if(tabs.getSelectedTab().equals(this.fullContextInstance)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());
                        contextMachine.addSchedulerJobStateChangeEventListener(stateChange
                            -> {
                            ui.access(() -> {
                            try {
                                 this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContext()));
                            }
                            catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }});
                        });
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.contextStatus)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());
                        contextMachine.addSchedulerJobStateChangeEventListener(stateChange
                            -> {
                            ui.access(() -> {
                            try {
                                 this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()));
                            }
                            catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }});
                        });
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.contextEvents)) {
                    this.aceEditor.setValue(this.events.toString());
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.visualisation)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(true);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.contextAudit)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(true);
                }
            }
            catch (JsonProcessingException e){
                e.printStackTrace();
            }
        });

        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(tabs);

        div.add(controlsLayout, tabLayout, this.aceEditor, this.schedulerVisualisation, this.contextInstanceAuditWidget);

        this.setSizeFull();
        this.add(div);
    }

    protected void initialiseEditor()
    {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.json);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setWidth("auto");
        aceEditor.setHeight("80vh");
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
    }

    public void updateContextDropdownContents() {
        this.contextInstances.setItems(ContextMachineCache.instance().contextNames());
    }
}
