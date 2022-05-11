package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;

public class ContextTemplateManagementWidget extends Div {

    private ScheduledContextService scheduledContextService;
    private FormLayout formLayout;
    private IkasanAuthentication authentication;

    private AceEditor aceEditor;

    protected SchedulerVisualisation schedulerVisualisation;

    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private TextField endWindowCronExpressionTf;

    private Tab visualisationTab;
    private Tab rawContextTab;
    private Tab instancesTab;
    private Tab jobsTab;
    private Tab statisticsTab;
    private Tabs tabs;

    /**
     * Constructor
     *
     * @param scheduledContextService
     */
    public ContextTemplateManagementWidget(ScheduledContextService scheduledContextService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService) {

        this.scheduledContextService = scheduledContextService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.init(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.setWidthFull();
    }

    private void init(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                      ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                      MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                      LogStreamingService logStreamingService) {
        this.contextNameTf = new TextField("Context Name");
        this.descriptionTa = new TextArea("Description");

        Icon startWindowCronBuilderIcon = IconDecorator.decorate(VaadinIcon.BUILDING_O.create(), "Build cron expression", "14pt", "rgba(241, 90, 35, 1.0)");
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
        this.startWindowCronExpressionTf = new TextField("Time Window Start");
        this.startWindowCronExpressionTf.setSuffixComponent(startWindowCronBuilderIcon);

        Icon endWindowCronBuilderIcon = IconDecorator.decorate(VaadinIcon.BUILDING_O.create(), "Build cron expression", "14pt", "rgba(241, 90, 35, 1.0)");
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
        this.endWindowCronExpressionTf = new TextField("Time Window Start");
        this.endWindowCronExpressionTf.setSuffixComponent(endWindowCronBuilderIcon);

        this.formLayout = new FormLayout();
        this.formLayout.setWidth("100%");

        this.formLayout.add(this.contextNameTf, this.startWindowCronExpressionTf, this.descriptionTa, this.endWindowCronExpressionTf);
        this.initialiseEditor();
        this.initialiseVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.initialiseTabs();
        HorizontalLayout tabLayout = new HorizontalLayout();
        tabLayout.add(this.tabs);
        this.add(this.formLayout, tabLayout, this.aceEditor, this.schedulerVisualisation);
    }

    private void initialiseTabs() {
        this.visualisationTab = new Tab("Visualisation");
        this.rawContextTab = new Tab("JSON");
        this.instancesTab = new Tab("Instances");
        this.jobsTab = new Tab("Jobs");
        this.statisticsTab = new Tab("Statistics");

        this.tabs = new Tabs();
        this.tabs.add(this.visualisationTab, this.rawContextTab
            , this.instancesTab, this.jobsTab, this.statisticsTab);

        tabs.addSelectedChangeListener(event -> {
            try {
                if(tabs.getSelectedTab().equals(this.instancesTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.statisticsTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.rawContextTab)) {
                    this.aceEditor.setVisible(true);
                    this.schedulerVisualisation.setVisible(false);
                }
                else if(tabs.getSelectedTab().equals(this.visualisationTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(true);
                }
                else if(tabs.getSelectedTab().equals(this.jobsTab)) {
                    this.aceEditor.setVisible(false);
                    this.schedulerVisualisation.setVisible(false);
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
    }

    protected void initialiseVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService) {
        this.schedulerVisualisation = new SchedulerVisualisation(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService);
        this.schedulerVisualisation.setWidthFull();
        this.schedulerVisualisation.setHeight("75vh");
    }
}
