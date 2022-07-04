package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.shared.Registration;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerInstanceVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ContextInstanceWidget extends Div {

    private Registration contextInstanceStateChangeRegistration;

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private FormLayout formLayout;
    private IkasanAuthentication authentication;

    private AceEditor aceEditor;
    protected SchedulerInstanceVisualisation schedulerInstanceVisualisation;
    private SchedulerJobInstanceGridWidget schedulerJobInstanceGridWidget;
    private ContextTemplateStatisticsWidget contextTemplateStatisticsWidget;
    private ContextInstanceAuditWidget contextInstanceAuditWidget;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;

    private Div schedulerVisualisationDiv;

    private TextField contextInstanceId;
    private TextField contextInstanceStatus;
    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private TextField endWindowCronExpressionTf;

    private Tab visualisationTab;
    private Tab rawContextTab;
    private Tab jobsTab;
    private Tab statisticsTab;
    private Tab auditTab;
    private Tabs tabs;

    private ContextInstance contextInstance;
    private ContextTemplate contextTemplate;

    private SchedulerStatusDiv statusDiv;

    private ComboBox<String> contextViews;

    /**
     * Constructor
     *
     */
    public ContextInstanceWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                 ContextProfileService contextProfileService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.contextInstance = contextInstance;
        this.contextTemplate = contextTemplate;
        this.jobInitiationService = jobInitiationService;
        this.contextProfileService = contextProfileService;

        this.init(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.setWidthFull();
    }

    private void init(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                      ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                      MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                      LogStreamingService logStreamingService) {
        Binder<ContextInstance> binder = new Binder<>(ContextInstance.class);

        this.contextInstanceId = new TextField(getTranslation("label.context-instance-id", UI.getCurrent().getLocale()));
        binder.forField(contextInstanceId)
            .bind(ContextInstance::getId, ContextInstance::setId);
        this.contextInstanceStatus = new TextField(getTranslation("table-header.status", UI.getCurrent().getLocale()));
        this.contextInstanceStatus.setValue(this.contextInstance.getStatus().name());

        this.contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        binder.forField(contextNameTf)
            .bind(ContextInstance::getName, ContextInstance::setName);
        this.descriptionTa = new TextArea(getTranslation("table-header.description", UI.getCurrent().getLocale()));
        binder.forField(descriptionTa)
            .bind(ContextInstance::getDescription, ContextInstance::setDescription);

        this.startWindowCronExpressionTf = new TextField(getTranslation("label.time-window-start", UI.getCurrent().getLocale()));
        binder.forField(startWindowCronExpressionTf)
            .bind(ContextInstance::getTimeWindowStart, ContextInstance::setTimeWindowStart);


        this.endWindowCronExpressionTf = new TextField(getTranslation("label.time-window-end", UI.getCurrent().getLocale()));
        binder.forField(endWindowCronExpressionTf)
            .bind(ContextInstance::getTimeWindowEnd, ContextInstance::setTimeWindowEnd);

        binder.readBean(this.contextInstance);

        this.formLayout = new FormLayout();
        formLayout.getStyle().set("padding-top", "0px");
        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.setStatus(this.contextInstance.getStatus());

        formLayout.add(this.statusDiv, 4);
        formLayout.setResponsiveSteps(
            // Use four columns by default
            new FormLayout.ResponsiveStep("0", 4)
        );

        this.formLayout.setWidth("100%");

        H2 contextInstanceLabel = new H2(String.format(getTranslation("label.context-instance", UI.getCurrent().getLocale())));
        contextInstanceLabel.getStyle().set("padding-top", "5px");
        contextInstanceLabel.getStyle().set("margin", "20px");
        formLayout.add(contextInstanceLabel, 2);

        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setWidth("100%");

        Button resetButton = new Button("Reset Context");
        resetButton.setVisible(!this.contextInstance.getStatus().equals(InstanceStatus.ENDED));
        resetButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.reset-context", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.reset-context", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId());
                if (contextMachine != null) {
                    try {
                        this.saveContextInstance(contextMachine.getContext(), InstanceStatus.ENDED);
                        this.statusDiv.setStatus(InstanceStatus.ENDED);
                        ContextMachineCache.instance().remove(contextMachine);
                        contextMachine.resetContextInstance();
                        ContextMachineCache.instance().put(contextMachine);
                        this.schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(contextMachine.getContext());
                        String route = RouteConfiguration.forSessionScope()
                            .getUrl(ContextInstanceView.class, ContextMachineCache.instance()
                                .getByContextName(this.contextInstance.getName()).getContext().getId() + "_scheduledContextInstance");

                        getUI().ifPresent(ui -> ui.getPage().open(route));

                        resetButton.setVisible(false);
                    }
                    catch (Exception e) {
                        NotificationHelper.showErrorNotification("An error has occurred attempting to reset the context! Please contact Ikasan support.");
                    }
                }
            });
        });

        buttonLayout.add(resetButton);
        buttonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, resetButton);
        buttonLayout.setMargin(false);
        buttonLayout.setSpacing(false);

        formLayout.add(buttonLayout, 2);

        this.formLayout.add(this.contextInstanceId, this.contextNameTf
            , this.startWindowCronExpressionTf, this.endWindowCronExpressionTf, this.descriptionTa);

        this.initialiseEditor();
        this.initialiseVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.initialiseSchedulerJobGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseContextInstanceAuditWidget(scheduledContextInstanceService);
        this.initialiseTabs();
        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(this.tabs);
        this.getStyle().set("padding-top", "0px");
        this.add(this.formLayout, tabLayout, this.aceEditor, this.schedulerVisualisationDiv
            , this.schedulerJobInstanceGridWidget, this.contextTemplateStatisticsWidget, this.contextInstanceAuditWidget);
    }

    private void initialiseTabs() {
        this.visualisationTab = new Tab(getTranslation("tab.visualisation", UI.getCurrent().getLocale()));
        this.rawContextTab = new Tab(getTranslation("tab.json-raw-format", UI.getCurrent().getLocale()));
        this.jobsTab = new Tab(getTranslation("tab.job-instances", UI.getCurrent().getLocale()));
        this.statisticsTab = new Tab(getTranslation("tab.statistics", UI.getCurrent().getLocale()));
        this.auditTab = new Tab(getTranslation("tab.audit", UI.getCurrent().getLocale()));

        this.tabs = new Tabs();
        this.tabs.add(this.visualisationTab, this.rawContextTab
            , this.jobsTab, this.statisticsTab, this.auditTab);

        tabs.addSelectedChangeListener(event -> {
            try {
                if(tabs.getSelectedTab().equals(this.statisticsTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.schedulerJobInstanceGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(true);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.schedulerJobInstanceGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(true);
                    this.schedulerJobInstanceGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.jobsTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.schedulerJobInstanceGridWidget.setVisible(true);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.auditTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.schedulerJobInstanceGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(true);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    protected void initialiseEditor()
    {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.json);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setWidth("auto");
        aceEditor.setHeight("75vh");
        aceEditor.setReadOnly(true);
        aceEditor.setWrap(false);
        aceEditor.setVisible(false);

        ContextService contextService = new ContextService();

        try {
            aceEditor.setValue(contextService.getContextInstanceString(this.contextInstance));
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    protected void initialiseVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService) {
        this.initialiseContextViewCombo();

        this.schedulerVisualisationDiv = new Div();
        this.schedulerVisualisationDiv.setSizeFull();

        this.schedulerInstanceVisualisation = new SchedulerInstanceVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService
            , this.schedulerJobInstanceService, this.jobInitiationService);
        this.schedulerInstanceVisualisation.setWidthFull();
        this.schedulerInstanceVisualisation.setHeight("75vh");

        try {
            if(ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()) != null) {
                this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(this.contextInstance.getId()).getContext();
            }

            if(this.contextViews.getValue() != null) {
                this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance, this.contextInstance.getContextsMap().get(this.contextViews.getValue()));
            }
            else {
                this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance, this.contextInstance);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        this.schedulerVisualisationDiv.add(this.contextViews, this.schedulerInstanceVisualisation);
    }


    private void initialiseContextViewCombo() {
        this.contextViews = new ComboBox<>("Context Views");
        this.contextViews.getElement().getStyle().set("position", "absolute");
        this.contextViews.getElement().getStyle().set("right", "45px");
        this.contextViews.setWidth("550px");

        ContextProfileSearchFilter searchFilter = new SolrContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextTemplate.getName());

        SearchResults<ContextProfileRecord> contextProfileRecords = this.contextProfileService
            .findByFilter(searchFilter, -1, -1, null, null);

        List<String> items = new ArrayList<>();
        AtomicReference<String> defaultContext = new AtomicReference<>();
        if(!contextProfileRecords.getResultList().isEmpty()) {
            contextProfileRecords.getResultList().forEach(record -> {
                if(record.getOwner() != null &&
                    record.getOwner().equals(this.authentication.getName())) {
                    defaultContext.set(record.getContextProfile().getDefaultContext());
                }
                else if(record.getOwner() != null && defaultContext.get() == null && record.getOwner().equals(ContextProfileRecord.SYSTEM_OWNER)){
                    defaultContext.set(record.getContextProfile().getDefaultContext());
                }

                record.getContextProfile().getSubContexts().forEach(profile -> items.add(profile));
            });
        }

        this.contextViews.setItems(items);

        if(defaultContext.get() != null) {
            this.contextViews.setValue(defaultContext.get());
        }
        else if(items.size() > 0) {
            this.contextViews.setValue(items.get(0));
        }

        this.contextViews.addValueChangeListener(event -> {
            try {
                this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance, contextInstance.getContextsMap().get(this.contextViews.getValue()));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private void initialiseSchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService) {
        this.schedulerJobInstanceGridWidget = new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextInstance, this.schedulerJobInstanceService,
            this.jobInitiationService);
        this.schedulerJobInstanceGridWidget.setWidthFull();
        this.schedulerJobInstanceGridWidget.setHeight("75vh");
        this.schedulerJobInstanceGridWidget.setVisible(false);

    }

    private void initialiseContextTemplateStatisticsWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                  LogStreamingService logStreamingService) {
        this.contextTemplateStatisticsWidget = new ContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextTemplate);
        this.contextTemplateStatisticsWidget.setWidthFull();
        this.contextTemplateStatisticsWidget.setHeight("75vh");
        this.contextTemplateStatisticsWidget.setVisible(false);

    }

    private void initialiseContextInstanceAuditWidget(ScheduledContextInstanceService scheduledContextInstanceService) {
        ScheduledContextInstanceAuditAggregateSearchFilter contextInstanceSearchFilter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        contextInstanceSearchFilter.setContextInstanceId(this.contextInstance.getId());
        this.contextInstanceAuditWidget = new ContextInstanceAuditWidget(scheduledContextInstanceService
            , contextInstanceSearchFilter, false);
        this.contextInstanceAuditWidget.setWidthFull();
        this.contextInstanceAuditWidget.setHeight("75vh");
        this.contextInstanceAuditWidget.setVisible(false);

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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if (contextInstanceStateChangeEvent.getContextInstance() != null) {
                ui.access(() ->  {
                    if(this.contextInstance.getId().equals(contextInstanceStateChangeEvent.getContextInstance().getId())) {
                        this.contextInstance = contextInstanceStateChangeEvent.getContextInstance();
                        this.statusDiv.setStatus(contextInstanceStateChangeEvent.getNewStatus());
                    }
                    else {
                        ScheduledContextInstanceRecord record = this.scheduledContextInstanceService.findById(this.contextInstance.getId());
                        if(record != null) {
                            this.contextInstance = record.getContextInstance();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.contextInstanceStateChangeRegistration != null) {
            this.contextInstanceStateChangeRegistration.remove();
            this.contextInstanceStateChangeRegistration = null;
        }
    }
}
