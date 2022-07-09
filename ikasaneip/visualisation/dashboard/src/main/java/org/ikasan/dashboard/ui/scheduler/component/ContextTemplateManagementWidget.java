package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
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
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ContextTemplateManagementWidget extends Div {

    private ScheduledContextService scheduledContextService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ContextProfileService contextProfileService;
    private JobProvisionService jobProvisionService;
    private FormLayout formLayout;
    private IkasanAuthentication authentication;

    private AceEditor aceEditor;
    private SchedulerVisualisation schedulerVisualisation;
    private ContextInstanceGridWidget contextInstanceGridWidget;
    private SchedulerJobGridWidget schedulerJobGridWidget;
    private ContextTemplateStatisticsWidget contextTemplateStatisticsWidget;

    private JobInitiationService jobInitiationService;

    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private TextField endWindowCronExpressionTf;
    private ComboBox<String> contextViews;

    private Div schedulerVisualisationDiv;

    private Tab visualisationTab;
    private Tab rawContextTab;
    private Tab contextInstancesTab;
    private Tab jobTemplatesTab;
    private Tab statisticsTab;
    private Tabs tabs;

    private ContextTemplate contextTemplate;

    private UI ui;


    /**
     * Constructor
     *
     * @param scheduledContextService
     */
    public ContextTemplateManagementWidget(ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService,
                                           JobInitiationService jobInitiationService, ContextProfileService contextProfileService, JobProvisionService jobProvisionService) {

        this.scheduledContextService = scheduledContextService;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.contextTemplate = contextTemplate;
        this.jobInitiationService = jobInitiationService;
        this.contextProfileService = contextProfileService;
        this.jobProvisionService = jobProvisionService;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        ui = UI.getCurrent();

        this.init(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, jobInitiationService);
        this.setWidthFull();
    }

    private void init(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                      ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                      MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                      LogStreamingService logStreamingService, JobInitiationService jobInitiationService) {
        Binder<ContextTemplate> binder = new Binder<>(ContextTemplate.class);

        this.contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        binder.forField(contextNameTf)
            .bind(ContextTemplate::getName, ContextTemplate::setName);
        this.descriptionTa = new TextArea(getTranslation("label.context-description", UI.getCurrent().getLocale()));
        binder.forField(descriptionTa)
            .bind(ContextTemplate::getDescription, ContextTemplate::setDescription);

        Icon startWindowCronBuilderIcon = IconDecorator.decorate(VaadinIcon.BUILDING_O.create(), getTranslation("tooltip.build-cron-expression", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        startWindowCronBuilderIcon.addClickListener(event -> {
            CronBuilderDialog dialog = new CronBuilderDialog();
            dialog.init(this.startWindowCronExpressionTf.getValue());
            dialog.open();

            dialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened() && dialog.isSaveClose()) {
                    this.startWindowCronExpressionTf.setValue(dialog.getCronExpression());
                }
            });
        });

        this.startWindowCronExpressionTf = new TextField(getTranslation("label.time-window-start", UI.getCurrent().getLocale()));
        this.startWindowCronExpressionTf.setSuffixComponent(startWindowCronBuilderIcon);
        binder.forField(startWindowCronExpressionTf)
            .bind(ContextTemplate::getTimeWindowStart, ContextTemplate::setTimeWindowStart);

        Icon endWindowCronBuilderIcon = IconDecorator.decorate(VaadinIcon.BUILDING_O.create(), getTranslation("tooltip.build-cron-expression", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        endWindowCronBuilderIcon.addClickListener(event -> {
            CronBuilderDialog dialog = new CronBuilderDialog();
            dialog.init(this.endWindowCronExpressionTf.getValue());
            dialog.open();

            dialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened() && dialog.isSaveClose()) {
                    this.endWindowCronExpressionTf.setValue(dialog.getCronExpression());
                }
            });
        });
        this.endWindowCronExpressionTf = new TextField(getTranslation("label.time-window-end", UI.getCurrent().getLocale()));
        this.endWindowCronExpressionTf.setSuffixComponent(endWindowCronBuilderIcon);
        binder.forField(endWindowCronExpressionTf)
            .bind(ContextTemplate::getTimeWindowEnd, ContextTemplate::setTimeWindowEnd);

        binder.readBean(this.contextTemplate);

        this.formLayout = new FormLayout();
        this.formLayout.setWidth("100%");

        this.formLayout.add(this.contextNameTf, this.startWindowCronExpressionTf, this.descriptionTa, this.endWindowCronExpressionTf);
        this.initialiseEditor();
        this.initialiseVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.initialiseContextInstanceGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
             configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService, jobInitiationService);
        this.initialiseSchedulerJobGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseContextTemplateStatisticsWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService,  moduleControlRestService, metaDataRestService,  systemEventLogger,  schedulerJobService, logStreamingService);
        this.initialiseTabs();
        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(this.tabs);
        this.add(this.formLayout, tabLayout, this.aceEditor, this.schedulerVisualisationDiv, this.contextInstanceGridWidget, this.schedulerJobGridWidget, this.contextTemplateStatisticsWidget);
    }

    private void initialiseTabs() {
        this.visualisationTab = new Tab(getTranslation("tab.visualisation", UI.getCurrent().getLocale()));
        this.rawContextTab = new Tab(getTranslation("tab.json-raw-format", UI.getCurrent().getLocale()));
        this.contextInstancesTab = new Tab(getTranslation("tab.context-instances", UI.getCurrent().getLocale()));
        this.jobTemplatesTab = new Tab(getTranslation("tab.job-templates", UI.getCurrent().getLocale()));
        this.statisticsTab = new Tab(getTranslation("tab.statistics", UI.getCurrent().getLocale()));

        this.tabs = new Tabs();
        this.tabs.add(this.visualisationTab, this.rawContextTab
            , this.contextInstancesTab, this.jobTemplatesTab, this.statisticsTab);

        tabs.addSelectedChangeListener(event -> {
            try {
                if(tabs.getSelectedTab().equals(this.contextInstancesTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(true);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.statisticsTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(true);
                }
                else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(true);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(false);
                    this.contextTemplateStatisticsWidget.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.jobTemplatesTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisationDiv.setVisible(false);
                    this.contextInstanceGridWidget.setVisible(false);
                    this.schedulerJobGridWidget.setVisible(true);
                    this.contextTemplateStatisticsWidget.setVisible(false);
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
            aceEditor.setValue(contextService.getContextTemplateString(this.contextTemplate));
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

        this.schedulerVisualisation = new SchedulerVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService
            , this.schedulerJobInstanceService, this.jobInitiationService);
        this.schedulerVisualisation.setWidthFull();
        this.schedulerVisualisation.setHeight("75vh");

        ContextService contextService = new ContextService();
        try {
            if(this.contextViews.getValue() != null && !this.contextViews.getValue().equals(this.contextTemplate.getName())) {
                this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate, contextService.getContextTemplate(contextService.getContextTemplateString(this.contextTemplate.getContextsMap().get(this.contextViews.getValue()))), null);
            }
            else {
                this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate, contextService.getContextTemplate(contextService.getContextTemplateString(this.contextTemplate)), null);
            }

            this.schedulerVisualisationDiv.add(this.contextViews, this.schedulerVisualisation);
        }
        catch (IOException e) {
            // todo raise message
            e.printStackTrace();
        }
    }

    private void initialiseContextInstanceGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService, JobInitiationService jobInitiationService) {
        this.contextInstanceGridWidget = new ContextInstanceGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextTemplate, this.schedulerJobInstanceService,
            jobInitiationService, this.contextProfileService);
        this.contextInstanceGridWidget.setWidthFull();
        this.contextInstanceGridWidget.setHeight("75vh");
        this.contextInstanceGridWidget.setVisible(false);

    }

    private void initialiseSchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                     LogStreamingService logStreamingService) {
        this.schedulerJobGridWidget = new SchedulerJobGridWidget(scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, this.contextTemplate,
            this.jobInitiationService, this.jobProvisionService);
        this.schedulerJobGridWidget.setWidthFull();
        this.schedulerJobGridWidget.setHeight("75vh");
        this.schedulerJobGridWidget.setVisible(false);

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

    private void initialiseContextViewCombo() {
        this.contextViews = new ComboBox<>(getTranslation("label.context-views", UI.getCurrent().getLocale()));
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

        ContextService contextService = new ContextService();

        this.contextViews.addValueChangeListener(event -> {
            try {
                if (this.contextTemplate.getName().equals(event.getValue())) {
                    this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate
                        , contextService.getContextTemplate(contextService.getContextTemplateString(this.contextTemplate)), null);
                }
                else {
                    this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate
                        , contextService.getContextTemplate(contextService.getContextTemplateString(this.contextTemplate.getContextsMap().get(this.contextViews.getValue()))), null);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
