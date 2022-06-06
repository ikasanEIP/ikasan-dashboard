package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class ContextInstanceWidget extends Div {

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private FormLayout formLayout;
    private IkasanAuthentication authentication;

    private AceEditor aceEditor;
    protected SchedulerVisualisation schedulerVisualisation;
    private SchedulerJobInstanceGridWidget schedulerJobInstanceGridWidget;
    private ContextTemplateStatisticsWidget contextTemplateStatisticsWidget;
    private ContextInstanceAuditWidget contextInstanceAuditWidget;

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

    /**
     * Constructor
     *
     */
    public ContextInstanceWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.contextInstance = contextInstance;
        this.contextTemplate = contextTemplate;

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
        this.formLayout.setWidth("100%");

        this.formLayout.add(this.contextInstanceId, this.contextInstanceStatus, this.contextNameTf
            , this.startWindowCronExpressionTf, this.descriptionTa, this.endWindowCronExpressionTf);

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
        this.add(this.formLayout, tabLayout, this.aceEditor, this.schedulerVisualisation
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
                    this.schedulerVisualisation.setVisible(false);
                    this.schedulerJobInstanceGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(true);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisation.setVisible(false);
                    this.schedulerJobInstanceGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(true);
                    this.schedulerJobInstanceGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.jobsTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(false);
                    this.schedulerJobInstanceGridWidget.setVisible(true);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                    this.contextInstanceAuditWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.auditTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(false);
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
        this.schedulerVisualisation = new SchedulerVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService
            , this.schedulerJobInstanceService);
        this.schedulerVisualisation.setWidthFull();
        this.schedulerVisualisation.setHeight("75vh");

        ContextService contextService = new ContextService();
        try {
            this.schedulerVisualisation.createSchedulerVisualisation(contextService.getContextInstance(contextService.getContextInstanceString(this.contextInstance)));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initialiseSchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService) {
        this.schedulerJobInstanceGridWidget = new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextInstance, this.schedulerJobInstanceService);
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
        ContextInstanceSearchFilter contextInstanceSearchFilter = new SolrContextInstanceSearchFilterImpl();
        contextInstanceSearchFilter.setContextSearchFilter(this.contextInstance.getId());
        this.contextInstanceAuditWidget = new ContextInstanceAuditWidget(scheduledContextInstanceService
            , contextInstanceSearchFilter, false);
        this.contextInstanceAuditWidget.setWidthFull();
        this.contextInstanceAuditWidget.setHeight("75vh");
        this.contextInstanceAuditWidget.setVisible(false);

    }
}
