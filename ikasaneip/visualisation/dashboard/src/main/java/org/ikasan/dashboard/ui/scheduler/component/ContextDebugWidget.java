package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.ContextSchedulerInstanceVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerInstanceVisualisation;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.DryRunParametersImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.service.SolrSchedulerJobInstancesInitialisationParametersImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


@CssImport("./styles/dashboard-view.css")
public class ContextDebugWidget extends Div {

    Logger logger = LoggerFactory.getLogger(ContextDebugWidget.class);

    private ScheduledContextService scheduledContextService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private JobLockCacheService jobLockCacheService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private JobInitiationService jobInitiationService;
    private JobUtilsService jobUtilsService;

    protected AceEditor aceEditor;
    protected SchedulerInstanceVisualisation schedulerInstanceVisualisation;
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

    private Checkbox dryRunModeCheckBox = new Checkbox("Dry run mode");
    private boolean contextChanged = false;

    private ScheduledContextInstanceService scheduledContextInstanceService;

    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    /**
     * Constructor
     */
    public ContextDebugWidget(ScheduledContextInstanceService scheduledContextInstanceService, JobInitiationService jobInitiationService,
                              ScheduledContextService scheduledContextService, SystemEventLogger systemEventLogger,
                              InternalEventDrivenJobService internalEventDrivenJobService, String queueDir,
                              ModuleMetaDataService moduleMetaDataService, JobLockCacheService jobLockCacheService,
                              ScheduledContextInstanceService contextInstanceService, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                              SchedulerJobService schedulerJobService, LogStreamingService logStreamingService, ContextParametersInstanceService contextParametersInstanceService,
                              SchedulerJobInstanceService schedulerJobInstanceService, JobUtilsService jobUtilsService, JobLockCacheInitialisationService jobLockCacheInitialisationService) {
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("100%");

        this.scheduledContextService = scheduledContextService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.jobLockCacheService = jobLockCacheService;
        this.contextParametersInstanceService = contextParametersInstanceService;
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        this.queueDir = queueDir;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.jobInitiationService = jobInitiationService;
        this.jobUtilsService = jobUtilsService;
        this.jobLockCacheInitialisationService = jobLockCacheInitialisationService;

        this.schedulerInstanceVisualisation = new ContextSchedulerInstanceVisualisation(".", moduleMetaDataService, scheduledProcessManagementService, configurationRestService,
            moduleControlRestService,  metaDataRestService, systemEventLogger, logStreamingService, schedulerJobInstanceService, jobInitiationService, jobUtilsService,
            this.scheduledContextService, null);

        this.schedulerInstanceVisualisation.setWidthFull();
        this.schedulerInstanceVisualisation.setHeight("1000px");
        this.schedulerInstanceVisualisation.setVisible(false);

        this.contextInstanceAuditWidget = new ContextInstanceAuditWidget(contextInstanceService);
        this.contextInstanceAuditWidget.setVisible(false);

        this.initialiseEditor();

        this.objectMapper = new ObjectMapper();

        this.ui = UI.getCurrent();
        HorizontalLayout controlsLayout = new HorizontalLayout();

        this.contextInstances = new Select<>();
        this.contextInstances.setWidth("350px");
        this.contextInstances.setLabel("Context Instance");
        this.contextInstances.setVisible(false);

        if(!ContextMachineCache.instance().contextNames().isEmpty()) {
            this.contextInstances.setItems(ContextMachineCache.instance().contextNames());
            this.contextInstances.getDataProvider().refreshAll();
            this.contextInstances.setVisible(true);
        }

        this.contextInstances.addValueChangeListener(event -> {
            try {
                if(this.contextInstances.getValue() != null ) {
                    ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());

                    if (contextMachine != null) {
                        this.schedulerInstanceVisualisation.createSchedulerVisualisation(contextMachine.getContext(), contextMachine.getContext(), null);
                        controlsLayout.remove(this.dryRunModeCheckBox);
                        this.dryRunModeCheckBox = new Checkbox("Dry run mode");
                        controlsLayout.add(this.dryRunModeCheckBox);
                        controlsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.dryRunModeCheckBox);
                        this.dryRunModeCheckBox.addValueChangeListener(changeEvent -> {
                            if(this.contextInstances.getValue() != null) {
                                ContextMachine machine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());
                                if (machine != null) {
                                    if(changeEvent.getValue()) {
                                        contextMachine.setDryRunParameters(new DryRunParametersImpl());
                                    }
                                    else {
                                        contextMachine.setDryRunParameters(null);
                                    }
                                }
                            }
                            contextChanged = false;
                        });
                        this.setDryRunCheckbox(contextMachine.getContext().getName());
                    } else {
                        return;
                    }
                    if (tabs.getSelectedTab().equals(this.fullContextInstance)) {
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                    } else if (tabs.getSelectedTab().equals(this.contextStatus)) {
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                    }
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
                jobInitiationService, this.scheduledContextService, this.internalEventDrivenJobService, this.queueDir, moduleMetaDataService
                , this.jobLockCacheService, this.contextParametersInstanceService, schedulerJobInstanceService, this.jobLockCacheInitialisationService);
            contextUploadDialog.open();

            contextUploadDialog.addOpenedChangeListener(event -> {
                if(!ContextMachineCache.instance().contextNames().isEmpty()) {
                    this.contextInstances.setVisible(true);
                    if(!event.isOpened()) {
                        String currentValue = this.contextInstances.getValue();
                        this.contextInstances.setItems(ContextMachineCache.instance().contextNames());
                        this.contextInstances.getDataProvider().refreshAll();
                        if (currentValue != null) {
                            this.contextInstances.setValue(currentValue);
                            this.setDryRunCheckbox(currentValue);
                        }
                    }
                }
            });
        });

        Button resetContextButton = new Button("Reset Context");
        resetContextButton.addClickListener(buttonClickEvent -> {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());
            try {
                contextMachine.setDryRunParameters(null);
                this.saveContextInstance(contextMachine.getContext(), InstanceStatus.ENDED);
                setDryRunCheckbox(contextMachine.getContext().getName());
                ContextMachineCache.instance().remove(contextMachine);
                contextMachine.resetContextInstance(false);
                ContextMachineCache.instance().put(contextMachine);

                this.schedulerInstanceVisualisation.createSchedulerVisualisation(contextMachine.getContext(), contextMachine.getContext(), null);

                SchedulerJobInstancesInitialisationParameters schedulerJobInstancesInitialisationParameters
                    = new SolrSchedulerJobInstancesInitialisationParametersImpl(false);
                this.schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(contextMachine.getContext()
                    , schedulerJobInstancesInitialisationParameters);

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
            catch (Exception e) {
                e.printStackTrace();
            }
        });


        Button viewJobLockCacheButton = new Button("View Job Lock Cache");
        viewJobLockCacheButton.addClickListener(event -> {
            JobLockCacheViewerDialog jobLockCacheViewerDialog = new JobLockCacheViewerDialog();
            jobLockCacheViewerDialog.open();
        });

        controlsLayout.add(this.contextInstances, addContextButton, resetContextButton, viewJobLockCacheButton);
        controlsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.contextInstances);


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
                            if(ui.isAttached()) {
                                ui.access(() -> {
                                    try {
                                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContext()));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        });
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContext()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                    this.aceEditor.setVisible(true);
                    this.schedulerInstanceVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.contextStatus)) {
                    if(this.contextInstances.getValue() != null && !this.contextInstances.getValue().isEmpty()){
                        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(this.contextInstances.getValue());
                        contextMachine.addSchedulerJobStateChangeEventListener(stateChange
                            -> {
                            if(ui.isAttached()) {
                                ui.access(() -> {
                                    try {
                                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        });
                        this.aceEditor.setValue(this.objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contextMachine.getContextInstanceStatus()));
                    }
                    else {
                        this.aceEditor.setValue("");
                    }
                    this.aceEditor.setVisible(true);
                    this.schedulerInstanceVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.contextEvents)) {
                    this.aceEditor.setValue(this.events.toString());
                    this.aceEditor.setVisible(true);
                    this.schedulerInstanceVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.visualisation)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerInstanceVisualisation.setVisible(true);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.contextAudit)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerInstanceVisualisation.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(true);
                }
            }
            catch (JsonProcessingException e){
                e.printStackTrace();
            }
        });

        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(tabs);

        div.add(controlsLayout, tabLayout, this.aceEditor, this.schedulerInstanceVisualisation, this.contextInstanceAuditWidget);

        this.setSizeFull();
        this.add(div);
    }

    protected void saveContextInstance(ContextInstance contextInstance, InstanceStatus instanceStatus) {
        contextInstance.setStatus(instanceStatus);
        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(contextInstance);
        scheduledContextInstanceRecord.setTimestamp(contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
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


    private void setDryRunCheckbox(String contextName) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
        if(contextMachine.isDryRun()) {
            this.dryRunModeCheckBox.setValue(true);
        }
        else {
            this.dryRunModeCheckBox.setValue(false);
        }
    }
}
