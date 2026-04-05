package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.f0rce.ace.util.AceCursorPosition;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.model.JsonValidationError;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.broadcast.ContextTemplateSavedEventBroadcaster;
import org.ikasan.job.orchestration.context.validation.ContextError;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JobPlanEditorWidget extends VerticalLayout {
    private Logger logger = LoggerFactory.getLogger(JobPlanEditorWidget.class);

    private ScheduledContextService scheduledContextService;
    private SchedulerJobService<SchedulerJobRecord> schedulerJobService;
    private ContextTemplate contextTemplate;
    private AceEditor aceEditor;
    private ContextService contextService;
    private SystemEventLogger systemEventLogger;

    private Tabs tabs;
    private Tab errorTab;
    private Tab warningTab;

    private Grid<ContextError> warningGrid;
    private Grid<JsonValidationError> errorGrid;

    private VerticalLayout warningWidget;
    private VerticalLayout errorWidget;

    private SplitLayout editorSplitLayout;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private IkasanAuthentication ikasanAuthentication;

    public JobPlanEditorWidget(ContextTemplate contextTemplate, ScheduledContextService scheduledContextService,
                               SchedulerJobService<SchedulerJobRecord> schedulerJobService, SystemEventLogger systemEventLogger) {
        this.contextTemplate = contextTemplate;
        if(this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }

        this.ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.contextService =  new ContextService();

        this.setMargin(false);
        this.setPadding(false);
        this.initialiseEditor();
        this.initialiseTabs();
        this.initialiseSplitLayout();
        this.setSizeFull();

        HorizontalLayout buttons = this.createButtonLayout();
        this.add(buttons);

        this.setHorizontalComponentAlignment(Alignment.CENTER, buttons);
    }

    /**
     * Initialise the JSON editor.
     */
    protected void initialiseEditor()
    {
        aceEditor = new AceEditor();

        aceEditor.setTheme(AceTheme.dracula);
        aceEditor.setMode(AceMode.json);
        aceEditor.setFontSize(11);
        aceEditor.setTabSize(4);
        aceEditor.setSizeFull();
        aceEditor.setReadOnly(false);
        aceEditor.setWrap(false);
        aceEditor.setVisible(true);

        this.updateRawContextTemplate(this.contextTemplate);
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();

        Button validateButton = new Button(getTranslation("button.validate", UI.getCurrent().getLocale()));
        ComponentSecurityVisibility.applySecurity(validateButton, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_DEV_ADMIN,
            SecurityConstants.SCHEDULER_DEV_WRITE);
        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        ComponentSecurityVisibility.applySecurity(saveButton, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_DEV_ADMIN,
            SecurityConstants.SCHEDULER_DEV_WRITE);

        horizontalLayout.add(validateButton, saveButton);

        validateButton.addClickListener(event -> this.validate());

        saveButton.addClickListener(buttonClickEvent -> {
            ContextTemplate contextTemplate = this.validate();

            if(contextTemplate == null) {
                NotificationHelper.showUserNotification(getTranslation("notification.cannot-save-job-plan-template-json", UI.getCurrent().getLocale()));
            }
            else  {
                ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                dialog.open(getTranslation("progress-dialog.saving-job-plan", UI.getCurrent().getLocale()),
                    getTranslation("progress-dialog.saving-job-plan-body", UI.getCurrent().getLocale()));

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("ContextInstanceTreeViewWidget"));
                executor.execute(() -> {
                    boolean error = false;
                    try {
                        ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(this.contextTemplate.getName());

                        ContextTemplate beforeUpdate = scheduledContextRecord.getContext();

                        scheduledContextRecord.setContext(contextTemplate);

                        SearchResults<SchedulerJobRecord> searchResults = (SearchResults<SchedulerJobRecord>) this.schedulerJobService
                            .findByContext(contextTemplate.getName(), -1, -1);

                        List<SchedulerJob> jobs = searchResults.getResultList().stream()
                            .map(record -> record.getJob())
                            .collect(Collectors.toList());

                        // Helper method to populate the contextNames collection on each of the scheduler jobs.
                        ContextHelper.populateChildContextNamesOnSchedulerJobs(contextTemplate,
                            jobs);

                        this.schedulerJobService.save(jobs, ikasanAuthentication.getName());
                        this.scheduledContextService.save(scheduledContextRecord);

                        this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_SAVED, String.format("Job Plan Saved. Parent Job Plan [%s]. Name of Saved Job Plan [%s].\nBefore\n[%s]\nAfter\n[%s]"
                            , this.contextTemplate.getName(), this.contextTemplate.getName(), this.objectMapper.writeValueAsString(beforeUpdate), this.objectMapper.writeValueAsString(contextTemplate)), this.ikasanAuthentication.getName());


                        ContextTemplateSavedEventBroadcaster.broadcast(contextTemplate);
                    } catch (JsonProcessingException e) {
                        logger.error(String.format("An error has occurred saving job plan[%s]!", contextTemplate.getName()), e);
                        error = true;
                    } finally {
                        boolean finalError = error;
                        current.access(() -> {
                            dialog.close();
                            if (finalError) {
                                NotificationHelper.showUserNotification(getTranslation("notification.job-plan-save-error"
                                    , UI.getCurrent().getLocale()));
                            } else {
                                NotificationHelper.showUserNotification(getTranslation("notification.job-plan-successfully-saved", UI.getCurrent().getLocale()));
                            }
                        });
                    }
                });
            }
        });

        return horizontalLayout;
    }

    private ContextTemplate validate() {
        ContextTemplate contextTemplate;
        this.errorGrid.setItems(List.of());
        this.warningGrid.setItems(List.of());

        try {
            contextService.isValidJSON(this.aceEditor.getValue());
            contextTemplate = this.contextService.getContextTemplate(this.aceEditor.getValue());
        }
        catch (JsonProcessingException e) {
            JsonValidationError jsonValidationError = new JsonValidationError(e.getMessage(), e.getLocation().getLineNr()-1,
                e.getLocation().getColumnNr());

            this.errorGrid.setItems(List.of(jsonValidationError));
            this.tabs.setSelectedTab(errorTab);
            this.errorWidget.setVisible(true);
            this.warningWidget.setVisible(false);
            this.editorSplitLayout.setSplitterPosition(75);

            NotificationHelper.showUserNotification(getTranslation("notification.job-plan-json-not-valid", UI.getCurrent().getLocale()));
            return null;
        }

        List<JsonValidationError> jsonValidationErrors = new ArrayList<>();
        List<ContextError> jsonValidationWarnings = new ArrayList<>();

        try {
            SearchResults<SchedulerJobRecord> schedulerJobRecords = (SearchResults<SchedulerJobRecord>)this.schedulerJobService
                .findByContext(this.contextTemplate.getName(), -1, -1);

            List<SchedulerJob> jobs = schedulerJobRecords.getResultList().stream()
                .map(schedulerJobRecord -> schedulerJobRecord.getJob())
                .collect(Collectors.toList());

            ContextTemplateValidator contextTemplateValidator = new ContextTemplateValidator();
            contextTemplateValidator.validateJobs(contextTemplate, jobs);
        }
        catch (InvalidContextTemplateException e) {
            AceCursorPosition position = this.aceEditor.getCursorPosition();
            e.getContextErrors().forEach(contextError -> {
                int lineNumber = 0;
                for(String line: this.aceEditor.getValue().split("\n")) {
                    if(line.contains("\"jobName\" : \"" + contextError.getJobName() + "\"")) {
                        break;
                    }
                    lineNumber++;
                }

                JsonValidationError error = new JsonValidationError(contextError.getErrorMessage(), lineNumber,
                    0);
                jsonValidationErrors.add(error);
            });

            this.aceEditor.setCursorPosition(position.getRow(), position.getColumn());
        }
        catch (Exception e) {
            NotificationHelper.showUserNotification(getTranslation("error.job-plan-validation-failure", UI.getCurrent().getLocale()));
        }


        try {
            ContextTemplateValidator contextTemplateValidator = new ContextTemplateValidator();
            contextTemplateValidator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            jsonValidationWarnings.addAll(e.getContextErrors());
        }
        catch (Exception e) {
            NotificationHelper.showUserNotification(getTranslation("error.job-plan-validation-failure", UI.getCurrent().getLocale()));
        }

        if(!contextTemplate.getName().equals(this.contextTemplate.getName())) {
            NotificationHelper.showUserNotification(getTranslation("notification.job-plan-json-name-cannot-change", UI.getCurrent().getLocale()));

            this.aceEditor.findAndSelect("\"name\" : \""+this.contextTemplate.getName()+"\"");
            JsonValidationError jsonValidationError = new JsonValidationError(getTranslation("notification.job-plan-json-name-cannot-change", UI.getCurrent().getLocale()),
                this.aceEditor.getCursorPosition().getRow(), this.aceEditor.getCursorPosition().getColumn());

            jsonValidationErrors.add(jsonValidationError);
        }

        if(!jsonValidationErrors.isEmpty()) {
            this.errorGrid.setItems(jsonValidationErrors);
            this.tabs.setSelectedTab(this.errorTab);
            this.errorWidget.setVisible(true);
            this.warningWidget.setVisible(false);
            this.editorSplitLayout.setSplitterPosition(75);

            NotificationHelper.showUserNotification(getTranslation("notification.job-plan-json-not-valid", UI.getCurrent().getLocale()));
        }

        if(!jsonValidationWarnings.isEmpty()) {
            this.warningGrid.setItems(jsonValidationWarnings);
            if(jsonValidationErrors.isEmpty()) {
                this.tabs.setSelectedTab(this.warningTab);
                this.errorWidget.setVisible(false);
                this.warningWidget.setVisible(true);
                this.editorSplitLayout.setSplitterPosition(75);

                NotificationHelper.showUserNotification(getTranslation("notification.job-plan-json-contains-warnings", UI.getCurrent().getLocale()));
            }
        }

        if(jsonValidationErrors.isEmpty() && jsonValidationWarnings.isEmpty()) {
            NotificationHelper.showUserNotification(getTranslation("notification.job-plan-is-valid", UI.getCurrent().getLocale()));
        }

        if(!jsonValidationErrors.isEmpty()) {
            return null;
        }
        else {
            return contextTemplate;
        }

    }

    private void initialiseTabs() {
        this.errorTab = new Tab(getTranslation("tab.errors", UI.getCurrent().getLocale()));
        this.warningTab = new Tab(getTranslation("tab.warnings", UI.getCurrent().getLocale()));

        this.tabs = new Tabs();
        this.tabs.add(this.errorTab, this.warningTab);

        tabs.addSelectedChangeListener(event -> {
            if (tabs.getSelectedTab().equals(this.errorTab)) {
                this.errorWidget.setVisible(true);
                this.warningWidget.setVisible(false);
            }
            else if (tabs.getSelectedTab().equals(this.warningTab)) {
                this.errorWidget.setVisible(false);
                this.warningWidget.setVisible(true);
            }
        });
    }

    private void initialiseErrorWidget() {
        this.errorWidget = new VerticalLayout();
        this.errorWidget.setSizeFull();
        this.errorWidget.setMargin(false);
        this.errorWidget.setPadding(false);

        this.errorGrid = new Grid<>();
        errorGrid.addColumn(JsonValidationError::getErrorMessage)
            .setHeader(getTranslation("label.error-message", UI.getCurrent().getLocale()))
            .setKey("errorMessage")
            .setFlexGrow(20);
        errorGrid.addColumn(new ComponentRenderer<>(
                jsonValidationError -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    Button link = new Button(getTranslation("button.go-to-error", UI.getCurrent().getLocale()));
                    link.addClickListener(event ->
                       aceEditor.setCursorPosition(jsonValidationError.getLineNumber()
                           , jsonValidationError.getColumnNumber()));

                    verticalLayout.add(link);

                    return verticalLayout;
                }))
            .setKey("link")
            .setFlexGrow(1);

        this.errorWidget.add(errorGrid);
    }

    private void initialiseWarningWidget() {
        this.warningWidget = new VerticalLayout();
        this.warningWidget.setSizeFull();
        this.warningWidget.setMargin(false);
        this.warningWidget.setPadding(false);

        this.warningGrid = new Grid<>();
        warningGrid.addColumn(ContextError::getErrorMessage)
            .setHeader(getTranslation("label.warning-message", UI.getCurrent().getLocale()))
            .setKey("warningMessage")
            .setFlexGrow(20);
        warningGrid.addColumn(new ComponentRenderer<>(
                contextError -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    Button link = new Button(getTranslation("button.go-to-job-plan", UI.getCurrent().getLocale()));
                    link.addClickListener(event ->
                        aceEditor.findAndSelect(contextError.getContextName()));

                    verticalLayout.add(link);

                    return verticalLayout;
                }))
            .setKey("link")
            .setFlexGrow(1);

        this.warningWidget.add(this.warningGrid);
        this.warningWidget.setVisible(false);
    }

    private void initialiseSplitLayout() {
        this.editorSplitLayout = new SplitLayout();
        this.editorSplitLayout.setHeight("100%");
        this.editorSplitLayout.setWidthFull();
        this.editorSplitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        this.editorSplitLayout.getElement().getStyle().set("margin-bottom", "5px");
        this.editorSplitLayout.setSplitterPosition(97);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(false);
        verticalLayout.setSpacing(false);
        verticalLayout.add(this.aceEditor);
        verticalLayout.expand(this.aceEditor);

        Button upButton = new Button();
        Button downButton = new Button();
        Button middleButton = new Button();

        upButton.getElement().appendChild(VaadinIcon.ARROW_UP.create().getElement());
        upButton.addClickListener(event -> {
            this.editorSplitLayout.setSplitterPosition(25);
        });

        middleButton.getElement().appendChild(VaadinIcon.LINE_H.create().getElement());
        middleButton.addClickListener(event -> {
            this.editorSplitLayout.setSplitterPosition(50);
        });

        downButton.getElement().appendChild(VaadinIcon.ARROW_DOWN.create().getElement());
        downButton.addClickListener(event -> {
            this.editorSplitLayout.setSplitterPosition(97);
        });

        HorizontalLayout splitLayoutManagerButtonLayout = new HorizontalLayout();
        splitLayoutManagerButtonLayout.getStyle().set("position", "absolute");
        splitLayoutManagerButtonLayout.getStyle().set("right", "10px");

        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.add(splitLayoutManagerButtonLayout);

        splitLayoutManagerButtonLayout.add(upButton, middleButton, downButton);

        VerticalLayout notificationTabsLayout = new VerticalLayout();
        notificationTabsLayout.setSpacing(false);
        notificationTabsLayout.setMargin(false);
        notificationTabsLayout.setPadding(false);
        notificationTabsLayout.add(wrapper, this.tabs);

        this.initialiseErrorWidget();
        this.initialiseWarningWidget();

        this.editorSplitLayout.addToPrimary(verticalLayout);
        this.editorSplitLayout.addToSecondary(notificationTabsLayout
            , errorWidget, warningWidget);

        this.add(this.editorSplitLayout);
    }

    public void updateRawContextTemplate(ContextTemplate contextTemplate) {
        ContextService contextService = new ContextService();

        try {
            this.contextTemplate = contextTemplate;
            aceEditor.setValue(contextService.getContextTemplateString(this.contextTemplate));
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
