package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.Divider;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.model.BlackoutWindowDateTimePair;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcaster;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.ContextDurationUtils;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.dashboard.model.user.IkasanPrincipal;
import org.ikasan.security.model.User;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextTemplateDialog extends AbstractCloseableResizableDialog {

    private static Logger logger = LoggerFactory.getLogger(ContextInstanceWidget.class);

    private ScheduledContextService scheduledContextService;
    private SchedulerJobService schedulerJobService;
    private SystemEventLogger systemEventLogger;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private ContextInstanceSchedulerService contextInstanceSchedulerService;
    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private IntegerField contextTtlMinutes;
    private IntegerField contextTtlHours;
    private IntegerField contextTtlDays;

    private IntegerField contextVisualisationLevelDistanceIf;
    private IntegerField contextVisualisationNodeDistanceIf;
    private IntegerField jobVisualisationHorizontalSpacingIf;
    private IntegerField jobVisualisationVerticalSpacingIf;
    private IntegerField fontSize;
    private IntegerField treeViewExpandLevel;
    private Checkbox isAbleToRunConcurrentlyCb;
    private Checkbox useDisplayNameCb;
    private DateTimePicker blackoutWindowStartTime;
    private DateTimePicker blackoutWindowEndTime;
    private ComboBox<DateTimeUtil.TimezonePair> timezoneCb;
    private ContextTemplate contextTemplate = new ContextTemplateImpl();
    private Binder<ContextTemplate> binder = new Binder<>(ContextTemplate.class);
    private FormLayout formLayout;
    private List<BlackoutWindowDateTimePair> blackoutWindowDateTimePairs;
    private Grid<BlackoutWindowDateTimePair> blackoutWindowsGrid;
    private ScheduledContextRecord scheduledContextRecord;
    private IkasanAuthentication authentication;
    private boolean editName;
    private int jobPlanIntervalMultiple;
    private UserService userService;
    private SecurityService securityService;


    /**
     * Constructor
     *
     * @param scheduledContextService
     * @param schedulerJobService
     * @param title
     * @param editName
     */
    public ContextTemplateDialog(ScheduledContextService scheduledContextService, SchedulerJobService schedulerJobService, ContextInstanceRegistrationService contextInstanceRegistrationService
        , ContextInstanceSchedulerService contextInstanceSchedulerService,  SystemEventLogger systemEventLogger, String title, boolean editName, int jobPlanIntervalMultiple
        , double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance
        , UserService userService, SecurityService securityService) {
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if(this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }
        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if(this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.userService = userService;
        if(this.userService == null) {
            throw new IllegalArgumentException("userService cannot be null!");
        }
        this.securityService = securityService;
        if(this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        if(title == null) {
            throw new IllegalArgumentException("title cannot be null!");
        }
        super.title.setText(title);

        this.editName = editName;
        this.jobPlanIntervalMultiple = jobPlanIntervalMultiple;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.contextTemplate.setVisualisationFontSize(14);
        this.contextTemplate.setJobVisualisationHorizontalSpacing((int)jobVisualisationHorizontalSpacing);
        this.contextTemplate.setJobVisualisationVerticalSpacing((int)jobVisualisationVerticalSpacing);
        this.contextTemplate.setContextVisualisationLevelDistance((int)contextVisualisationLevelDistance);
        this.contextTemplate.setContextVisualisationNodeDistance((int)contextVisualisationNodeDistance);
        this.init();
    }

    /**
     * Initialise the layout of the dialog.
     */
    private void init() {
        this.blackoutWindowDateTimePairs = new ArrayList<>();
        this.setHeight("880px");
        this.setWidth("98vw");

        super.showResize(false);
        super.setResizable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        this.initForm();

        layout.add(this.formLayout);

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("saveNewContextButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {
            if(this.validate()) {
                try {
                    binder.writeBean(this.contextTemplate);
                    this.bindFieldsToContext(this.contextTemplate);

                } catch (ValidationException e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.error-validating-context-template-form"
                        , UI.getCurrent().getLocale()));
                    return;
                }

                if(this.scheduledContextRecord != null) {
                    this.saveExisting();
                }
                else {
                    this.saveNew();
                }
            }
            else {
                NotificationHelper.showErrorNotification(getTranslation("error.validating-job-plan-form-error"
                    , UI.getCurrent().getLocale()));
            }
        });

        ComponentSecurityVisibility.applySecurity(saveButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);
        buttonLayout.getElement().getStyle().set("position", "absolute");
        buttonLayout.getElement().getStyle().set("bottom", "20px");
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, saveButton, cancelButton);
        Divider divider = new Divider();
        divider.getStyle().set("background-color", "rgba(0, 0, 0, 0.28)");
        layout.add(divider);
        layout.add(buttonLayout);

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);


        super.content.add(layout);
    }

    /**
     * Set the context template if the dialog is to manage one.
     *
     * @param contextTemplate
     */
    public void setContextTemplate(ContextTemplate contextTemplate) {
        this.contextTemplate = contextTemplate;
        this.scheduledContextRecord = this.loadContextTemplateRecord();
        this.contextTemplate = this.scheduledContextRecord.getContext();
        this.binder.readBean(contextTemplate);
        this.bindContextToFields(contextTemplate);
    }

    /**
     * Initialise the form layout.
     */
    private void initForm() {
        this.contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        this.contextNameTf.getElement().getThemeList().add("always-float-label");

        // This field is disabled thus preventing name changes. There is a lot to
        // think about relating to name changes of contexts including all retrospective artifacts
        // associated with instances of the context as well as updating all jobs and
        // synchronising those jobs with the agents. This will be a day 2 functionality if
        // it is even in fact necessary.
        this.contextNameTf.setEnabled(this.editName);
        binder.forField(contextNameTf)
            .asRequired(getTranslation("error.context-name-required", UI.getCurrent().getLocale()))
            .bind(ContextTemplate::getName, ContextTemplate::setName);
        this.descriptionTa = new TextArea(getTranslation("label.context-description", UI.getCurrent().getLocale()));
        this.descriptionTa.getElement().getThemeList().add("always-float-label");
        this.descriptionTa.setHeight("100px");
        binder.forField(descriptionTa)
            .asRequired(getTranslation("error.context-description-required", UI.getCurrent().getLocale()))
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
        this.startWindowCronExpressionTf.getElement().getThemeList().add("always-float-label");
        this.startWindowCronExpressionTf.setSuffixComponent(startWindowCronBuilderIcon);
        binder.forField(startWindowCronExpressionTf)
            .asRequired(getTranslation("error.missing-cron-expression", UI.getCurrent().getLocale()))
            .withValidator(value -> CronExpression.isValidExpression(value), getTranslation("error.invalid-cron-expression", UI.getCurrent().getLocale()))
            .bind(ContextTemplate::getTimeWindowStart, ContextTemplate::setTimeWindowStart);

        this.contextTtlDays = new IntegerField(getTranslation("label.duration-days", UI.getCurrent().getLocale()));
        this.contextTtlDays.getElement().getThemeList().add("always-float-label");
        this.contextTtlDays.setMin(0);
        this.contextTtlDays.setRequiredIndicatorVisible(true);
        this.contextTtlDays.setErrorMessage(getTranslation("error.context-ttl-days", UI.getCurrent().getLocale()));
        this.contextTtlHours = new IntegerField(getTranslation("label.duration-hours", UI.getCurrent().getLocale()));
        this.contextTtlHours.getElement().getThemeList().add("always-float-label");
        this.contextTtlHours.setRequiredIndicatorVisible(true);
        this.contextTtlHours.setMin(0);
        this.contextTtlHours.setMax(23);
        this.contextTtlHours.setErrorMessage(getTranslation("error.context-ttl-hours", UI.getCurrent().getLocale()));
        this.contextTtlMinutes = new IntegerField(getTranslation("label.duration-minutes", UI.getCurrent().getLocale()));
        this.contextTtlMinutes.getElement().getThemeList().add("always-float-label");
        this.contextTtlMinutes.setRequiredIndicatorVisible(true);
        this.contextTtlMinutes.setMin(0);
        this.contextTtlMinutes.setMax(59);
        this.contextTtlMinutes.setErrorMessage(getTranslation("error.context-ttl-minutes", UI.getCurrent().getLocale()));

        this.treeViewExpandLevel = new IntegerField(getTranslation("label.tree-view-expand-level", UI.getCurrent().getLocale()));
        this.treeViewExpandLevel.getElement().getThemeList().add("always-float-label");
        binder.forField(this.treeViewExpandLevel)
            .bind(ContextTemplate::getTreeViewExpandLevel, ContextTemplate::setTreeViewExpandLevel);

        this.contextVisualisationLevelDistanceIf = new IntegerField(getTranslation("label.context-node-vertical-distance", UI.getCurrent().getLocale()));
        this.contextVisualisationLevelDistanceIf.getElement().getThemeList().add("always-float-label");
        this.contextVisualisationLevelDistanceIf.setRequiredIndicatorVisible(true);
        this.contextVisualisationLevelDistanceIf.setErrorMessage(getTranslation("error.context-node-vertical-distance", UI.getCurrent().getLocale()));
        this.contextVisualisationLevelDistanceIf.setMin(0);
        binder.forField(this.contextVisualisationLevelDistanceIf)
            .bind(ContextTemplate::getContextVisualisationLevelDistance, ContextTemplate::setContextVisualisationLevelDistance);

        this.contextVisualisationNodeDistanceIf = new IntegerField(getTranslation("label.context-node-horizontal-distance", UI.getCurrent().getLocale()));
        this.contextVisualisationNodeDistanceIf.getElement().getThemeList().add("always-float-label");
        this.contextVisualisationNodeDistanceIf.setRequiredIndicatorVisible(true);
        this.contextVisualisationNodeDistanceIf.setErrorMessage(getTranslation("error.context-node-horizontal-distance", UI.getCurrent().getLocale()));
        this.contextVisualisationNodeDistanceIf.setMin(0);
        binder.forField(this.contextVisualisationNodeDistanceIf)
            .bind(ContextTemplate::getContextVisualisationNodeDistance, ContextTemplate::setContextVisualisationNodeDistance);

        this.jobVisualisationHorizontalSpacingIf = new IntegerField(getTranslation("label.job-visualisation-horizontal-spacing", UI.getCurrent().getLocale()));
        this.jobVisualisationHorizontalSpacingIf.getElement().getThemeList().add("always-float-label");
        this.jobVisualisationHorizontalSpacingIf.setRequiredIndicatorVisible(true);
        this.jobVisualisationHorizontalSpacingIf.setErrorMessage(getTranslation("error.job-visualisation-horizontal-spacing", UI.getCurrent().getLocale()));
        this.jobVisualisationHorizontalSpacingIf.setMin(0);
        binder.forField(this.jobVisualisationHorizontalSpacingIf)
            .bind(ContextTemplate::getJobVisualisationHorizontalSpacing, ContextTemplate::setJobVisualisationHorizontalSpacing);

        this.jobVisualisationVerticalSpacingIf = new IntegerField(getTranslation("label.job-visualisation-vertical-spacing", UI.getCurrent().getLocale()));
        this.jobVisualisationVerticalSpacingIf.getElement().getThemeList().add("always-float-label");
        this.jobVisualisationVerticalSpacingIf.setRequiredIndicatorVisible(true);
        this.jobVisualisationVerticalSpacingIf.setErrorMessage(getTranslation("error.job-visualisation-vertical-spacing", UI.getCurrent().getLocale()));
        this.jobVisualisationVerticalSpacingIf.setMin(0);
        binder.forField(this.jobVisualisationVerticalSpacingIf)
            .bind(ContextTemplate::getJobVisualisationVerticalSpacing, ContextTemplate::setJobVisualisationVerticalSpacing);

        this.fontSize = new IntegerField(getTranslation("label.visualisation-font-size", UI.getCurrent().getLocale()));
        this.fontSize.getElement().getThemeList().add("always-float-label");
        this.fontSize.setRequiredIndicatorVisible(true);
        this.fontSize.setErrorMessage(getTranslation("error.visualisation-font-size", UI.getCurrent().getLocale()));
        this.fontSize.setMin(0);
        binder.forField(this.fontSize)
            .bind(ContextTemplate::getVisualisationFontSize, ContextTemplate::setVisualisationFontSize);

        this.isAbleToRunConcurrentlyCb = new Checkbox(getTranslation("label.concurrent", UI.getCurrent().getLocale()));
        this.isAbleToRunConcurrentlyCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.isAbleToRunConcurrentlyCb)
            .bind(ContextTemplate::isAbleToRunConcurrently, ContextTemplate::setAbleToRunConcurrently);

        this.useDisplayNameCb = new Checkbox(getTranslation("label.use-display-name", UI.getCurrent().getLocale()));
        this.useDisplayNameCb.getElement().getThemeList().add("always-float-label");
        binder.forField(this.useDisplayNameCb)
            .bind(ContextTemplate::isUseDisplayName, ContextTemplate::setUseDisplayName);

        binder.readBean(this.contextTemplate);

        this.blackoutWindowStartTime = new DateTimePicker(getTranslation("label.blackout-window-start-date-time", UI.getCurrent().getLocale()));
        this.blackoutWindowStartTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowEndTime = new DateTimePicker(getTranslation("label.blackout-window-end-date-time", UI.getCurrent().getLocale()));
        this.blackoutWindowEndTime.setStep(Duration.ofMinutes(15));


        blackoutWindowsGrid = new Grid<>();
        blackoutWindowsGrid.removeAllColumns();
        blackoutWindowsGrid.setVisible(true);
        blackoutWindowsGrid.setWidthFull();
        blackoutWindowsGrid.setHeight("500px");

        blackoutWindowsGrid.addColumn(new ComponentRenderer<>(blackoutWindowDateTimePair -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(blackoutWindowDateTimePair.getBlackoutWindowStartTime());
                return horizontalLayout;
            }))
            .setHeader(getTranslation("label.blackout-window-start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTimeDate")
            .setFlexGrow(20);
        blackoutWindowsGrid.addColumn(new ComponentRenderer<>(blackoutWindowDateTimePair -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidthFull();
            horizontalLayout.add(blackoutWindowDateTimePair.getBlackoutWindowEndTime());
            return horizontalLayout;
        }))
            .setHeader(getTranslation("label.blackout-window-end-date-time", UI.getCurrent().getLocale()))
            .setKey("endTimeDate")
            .setFlexGrow(20);
        blackoutWindowsGrid.addColumn(new ComponentRenderer<>(blackoutWindowDateTimePair -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Button remove = new Button();
                remove.getElement().appendChild(VaadinIcon.MINUS.create().getElement());

                remove.addClickListener(event -> {
                    blackoutWindowDateTimePairs.remove(blackoutWindowDateTimePair);
                    blackoutWindowsGrid.getDataProvider().refreshAll();
                });

                horizontalLayout.add(remove);
                horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, remove);
                return horizontalLayout;
            }))
            .setKey("remove")
            .setFlexGrow(1);

        blackoutWindowsGrid.setItems(this.blackoutWindowDateTimePairs);
        blackoutWindowsGrid.addClassName("small-header");

        this.timezoneCb = new ComboBox<>(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        ComboBox.ItemFilter<DateTimeUtil.TimezonePair> filter = (element, filterString) ->
            element.zoneId.toLowerCase().contains(filterString.toLowerCase());
        this.timezoneCb.setRequired(true);
        this.timezoneCb.setId("timezoneCb");
        this.timezoneCb.getElement().getThemeList().add("always-float-label");
        this.timezoneCb.setItems(filter, DateTimeUtil.getAllZoneIdsAndItsOffSet());
        this.timezoneCb.setItemLabelGenerator((ItemLabelGenerator<DateTimeUtil.TimezonePair>) s
            -> String.format("%35s (UTC%s) %n", s.zoneId, s.offset).trim());
        this.timezoneCb.setClearButtonVisible(true);
        this.timezoneCb.setPlaceholder(getTranslation("label.choose-a-timezone", UI.getCurrent().getLocale()));
        this.timezoneCb.setErrorMessage(getTranslation("error.timezone-required", UI.getCurrent().getLocale()));


        Button addDateTimePairButton = new Button(getTranslation("button.add", UI.getCurrent().getLocale()), VaadinIcon.PLUS.create());
        addDateTimePairButton.setIconAfterText(true);
        addDateTimePairButton.addClickListener(event -> {
            this.blackoutWindowDateTimePairs.add(new BlackoutWindowDateTimePair());
            blackoutWindowsGrid.getDataProvider().refreshAll();
        });

        ComponentSecurityVisibility.applySecurity(addDateTimePairButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.formLayout = new FormLayout();
        this.formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 40)
        );
        this.formLayout.setWidth("100%");

        VerticalLayout cbLayout = new VerticalLayout(this.isAbleToRunConcurrentlyCb, this.useDisplayNameCb);
        cbLayout.setMargin(false);
        cbLayout.getElement().getThemeList().remove("padding");
        cbLayout.getElement().getThemeList().remove("spacing");

        this.formLayout.add(this.contextNameTf, this.startWindowCronExpressionTf, this.contextTtlDays, this.contextTtlHours
            , this.contextTtlMinutes, this.timezoneCb, this.treeViewExpandLevel, cbLayout, this.descriptionTa
            , this.contextVisualisationLevelDistanceIf, this.contextVisualisationNodeDistanceIf
            , this.jobVisualisationVerticalSpacingIf, this.jobVisualisationHorizontalSpacingIf
            , this.fontSize, this.blackoutWindowsGrid, addDateTimePairButton);

        this.formLayout.setColspan(this.contextNameTf, 12);
        this.formLayout.setColspan(this.startWindowCronExpressionTf, 6);
        this.formLayout.setColspan(this.contextTtlDays, 3);
        this.formLayout.setColspan(this.contextTtlHours, 3);
        this.formLayout.setColspan(this.contextTtlMinutes, 3);
        this.formLayout.setColspan(this.timezoneCb, 5);
        this.formLayout.setColspan(this.treeViewExpandLevel, 4);
        this.formLayout.setColspan(cbLayout, 3);
        this.formLayout.setColspan(this.descriptionTa, 12);
        this.formLayout.setColspan(this.contextVisualisationLevelDistanceIf, 5);
        this.formLayout.setColspan(this.contextVisualisationNodeDistanceIf, 5);
        this.formLayout.setColspan(this.jobVisualisationHorizontalSpacingIf, 5);
        this.formLayout.setColspan(this.jobVisualisationVerticalSpacingIf, 5);
        this.formLayout.setColspan(this.fontSize, 5 );
        this.formLayout.setColspan(blackoutWindowsGrid, 37);
        this.formLayout.setColspan(addDateTimePairButton, 3);
    }

    /**
     * Bind the adhoc fields to the context.
     *
     * @param contextTemplate
     */
    private void bindFieldsToContext(ContextTemplate contextTemplate) {
        contextTemplate.setTimezone(this.timezoneCb.getValue().zoneId);
        LocalDateTime now = LocalDateTime.now();
        ZoneId zone = ZoneId.of(contextTemplate.getTimezone());
        ZoneOffset zoneOffSet = zone.getRules().getOffset(now);

        Map<Long, Long> blackoutWindowRanges = new HashMap<>();

        this.blackoutWindowDateTimePairs.forEach(pair -> {
            blackoutWindowRanges.put(pair.getBlackoutWindowStartTime().getValue().atZone(zoneOffSet).toInstant().toEpochMilli(),
                pair.getBlackoutWindowEndTime().getValue().atZone(zoneOffSet).toInstant().toEpochMilli());
        });

        contextTemplate.setBlackoutWindowDateTimeRanges(blackoutWindowRanges);

        this.contextTemplate.setContextTtlMilliseconds(ContextDurationUtils.getMilliseconds(this.contextTtlDays.getValue()
            , this.contextTtlHours.getValue(), this.contextTtlMinutes.getValue()));
    }

    /**
     * Populate the blackout window pairs on the pairs that is the list data provider to the
     * blackout window grid.
     *
     * @param contextTemplate
     */
    private void bindContextToFields(ContextTemplate contextTemplate) {
        if(contextTemplate.getBlackoutWindowDateTimeRanges() != null) {
            contextTemplate.getBlackoutWindowDateTimeRanges().entrySet().forEach(entry -> {
                blackoutWindowDateTimePairs.add(new BlackoutWindowDateTimePair(entry.getKey(), entry.getValue()
                    , contextTemplate.getTimezone()));
            });
        }

        if(contextTemplate.getTimezone() != null) {
            this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(contextTemplate.getTimezone()));
        }
        else {
            this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(ZoneId.systemDefault().getId()));
        }

        this.contextTtlDays.setValue(ContextDurationUtils.getDays(this.contextTemplate.getContextTtlMilliseconds()));
        this.contextTtlHours.setValue(ContextDurationUtils.getHours(this.contextTemplate.getContextTtlMilliseconds()));
        this.contextTtlMinutes.setValue(ContextDurationUtils.getMinutes(this.contextTemplate.getContextTtlMilliseconds()));

        this.blackoutWindowsGrid.getDataProvider().refreshAll();
    }

    /**
     * Validate the form!
     *
     * @return
     */
    private boolean validate() {
        AtomicBoolean blackoutWindowsDefined = new AtomicBoolean(true);

        this.blackoutWindowDateTimePairs.forEach(pair -> {
            if(pair.getBlackoutWindowStartTime().getValue() == null) {
                blackoutWindowsDefined.set(false);
                pair.getBlackoutWindowStartTime().setInvalid(true);
                pair.getBlackoutWindowStartTime().setErrorMessage(getTranslation("error.blackout-window-start-date-time-missing"
                    , UI.getCurrent().getLocale()));
            }

            if(pair.getBlackoutWindowEndTime().getValue() == null) {
                blackoutWindowsDefined.set(false);
                pair.getBlackoutWindowEndTime().setInvalid(true);
                pair.getBlackoutWindowEndTime().setErrorMessage(getTranslation("error.blackout-window-end-date-time-missing"
                    , UI.getCurrent().getLocale()));
            }
        });

        if(!blackoutWindowsDefined.get()) {
            blackoutWindowsGrid.addClassName("error-header");
            this.blackoutWindowsGrid.getDataProvider().refreshAll();
        }
        else {
            blackoutWindowsGrid.removeClassName("error-header");
            this.blackoutWindowsGrid.getDataProvider().refreshAll();
        }

        AtomicBoolean blackoutWindowsValid = new AtomicBoolean(true);

        this.blackoutWindowDateTimePairs.forEach(pair -> {
            if(pair.getBlackoutWindowStartTime().getValue() != null &&
                pair.getBlackoutWindowEndTime().getValue() != null &&
                pair.getBlackoutWindowEndTime().getValue().isBefore(pair.getBlackoutWindowStartTime().getValue())) {
                pair.getBlackoutWindowStartTime().setInvalid(true);
                pair.getBlackoutWindowStartTime().setErrorMessage(getTranslation("error.end-time-before-start-time", UI.getCurrent().getLocale()));
                blackoutWindowsValid.set(false);
            }
        });

        if (!blackoutWindowsValid.get()) {
            blackoutWindowsGrid.addClassName("error-header");
            this.blackoutWindowsGrid.getDataProvider().refreshAll();
        } else {
            blackoutWindowsGrid.removeClassName("error-header");
            this.blackoutWindowsGrid.getDataProvider().refreshAll();
        }

        boolean timezoneValid = true;

        if(this.timezoneCb.getValue() == null) {
            this.timezoneCb.setInvalid(true);
            this.timezoneCb.setErrorMessage(getTranslation("error.timezone-is-required", UI.getCurrent().getLocale()));
            timezoneValid = false;
        }
        else {
            this.timezoneCb.setInvalid(false);
        }

        boolean isValid = this.binder.validate().isOk();

        this.contextTtlDays.setInvalid(this.contextTtlDays.isInvalid() || this.contextTtlDays.getValue() == null);
        this.contextTtlHours.setInvalid(this.contextTtlHours.isInvalid() || this.contextTtlHours.getValue() == null);
        this.contextTtlMinutes.setInvalid(this.contextTtlMinutes.isInvalid() || this.contextTtlMinutes.getValue() == null);
        this.contextVisualisationNodeDistanceIf.setInvalid(this.contextVisualisationNodeDistanceIf.isInvalid() || this.contextVisualisationNodeDistanceIf.getValue() == null);
        this.contextVisualisationLevelDistanceIf.setInvalid(this.contextVisualisationLevelDistanceIf.isInvalid() || this.contextVisualisationLevelDistanceIf.getValue() == null);
        this.jobVisualisationVerticalSpacingIf.setInvalid(this.jobVisualisationVerticalSpacingIf.isInvalid() || this.jobVisualisationVerticalSpacingIf.getValue() == null);
        this.jobVisualisationHorizontalSpacingIf.setInvalid(this.jobVisualisationHorizontalSpacingIf.isInvalid() || this.jobVisualisationHorizontalSpacingIf.getValue() == null);
        this.fontSize.setInvalid(this.fontSize.isInvalid() || this.fontSize.getValue() == null);


        if(isValid && blackoutWindowsDefined.get() && timezoneValid && blackoutWindowsValid.get()
            && !this.contextTtlHours.isInvalid() && !this.contextTtlHours.isInvalid() && !this.contextTtlMinutes.isInvalid()
            && !this.contextVisualisationLevelDistanceIf.isInvalid() && !this.contextVisualisationNodeDistanceIf.isInvalid()
            && !this.jobVisualisationHorizontalSpacingIf.isInvalid() && !this.jobVisualisationVerticalSpacingIf.isInvalid() && !this.fontSize.isInvalid()) {
            boolean intervalGreaterThanNextFireTime = CronUtils.isDurationGreaterThanNextFireTime(this.startWindowCronExpressionTf.getValue(), ContextDurationUtils.getMilliseconds(this.contextTtlDays.getValue()
                , this.contextTtlHours.getValue(), this.contextTtlMinutes.getValue()), this.jobPlanIntervalMultiple);

            if(!intervalGreaterThanNextFireTime) {
                this.startWindowCronExpressionTf.setInvalid(true);
                this.startWindowCronExpressionTf.setErrorMessage(getTranslation("error.cron-expression-fires-too-frequently", UI.getCurrent().getLocale()));
            }

            return intervalGreaterThanNextFireTime;
        }

        return isValid && blackoutWindowsDefined.get() && timezoneValid && blackoutWindowsValid.get()
            && !this.contextTtlHours.isInvalid() && !this.contextTtlHours.isInvalid() && !this.contextTtlMinutes.isInvalid()
            && !this.contextVisualisationLevelDistanceIf.isInvalid() && !this.contextVisualisationNodeDistanceIf.isInvalid()
            && !this.jobVisualisationHorizontalSpacingIf.isInvalid() && !this.jobVisualisationVerticalSpacingIf.isInvalid() && !this.fontSize.isInvalid();
    }

    /**
     * Save new if the is managing a new context.
     */
    private void saveNew() {
        try {
            // new context templates should be disabled
            this.contextTemplate.setDisabled(true);

            ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
            scheduledContextRecord.setContext(this.contextTemplate);
            scheduledContextRecord.setContextName(this.contextTemplate.getName());
            scheduledContextRecord.setModifiedBy(this.authentication.getName());
            scheduledContextRecord.setTimestamp(System.currentTimeMillis());
            this.scheduledContextService.save(scheduledContextRecord);

            this.applyJobPlanSecurity();

            String action = String.format("New job plan [%s] has been created.", this.contextTemplate.getName());
            this.systemEventLogger.logEvent(SystemEventConstants.NEW_JOB_PLAN_CREATED, action, authentication.getName());

            ContextTemplateSavedEventBroadcaster.broadcast(this.contextTemplate);
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.error-creating-new-context-template"
                , UI.getCurrent().getLocale()));
        }

        NotificationHelper.showUserNotification(getTranslation("notification.context-template-created-successfully"
            , UI.getCurrent().getLocale()));
        this.close();
    }


    /**
     * Applies job plan security based on the authenticated user's roles.
     * This method retrieves the authenticated user from the UserService,
     * and then iterates over the user's principals and their roles.
     * It excludes roles with names equal to "ADMIN" and "User".
     * The remaining role names are passed to the SecurityService's setJobPlanRoles method.
     */
    private void applyJobPlanSecurity() {
        User user = this.userService.loadUserByUsername(this.authentication.getName());
        List<String> roleNames = new ArrayList<>();
        user.getPrincipals().forEach(ikasanPrincipal -> {
            ikasanPrincipal.getRoles().forEach(role -> {
                if (!role.getName().equals("ADMIN")  && !role.getName().equals("User")) {
                    roleNames.add(role.getName());
                }
            });
        });

        this.securityService.setJobPlanRoles(this.contextTemplate.getName(), roleNames);
    }

    /**
     * Save existing if this is managing an existing context.
     */
    private void saveExisting() {
        try {
            ContextTemplate beforeModification = SerializationUtils.clone(this.scheduledContextRecord.getContext());

            this.scheduledContextRecord.setContextName(this.contextTemplate.getName());
            this.scheduledContextRecord.setContext(this.contextTemplate);
            this.scheduledContextRecord.setModifiedBy(this.authentication.getName());
            this.scheduledContextRecord.setTimestamp(System.currentTimeMillis());
            this.scheduledContextService.save(this.scheduledContextRecord);

            if(!this.contextTemplate.isDisabled() &&
                (!beforeModification.getTimeWindowStart().equals(this.contextTemplate.getTimeWindowStart()) ||
                    (beforeModification.getBlackoutWindowCronExpressions() != null && this.contextTemplate.getBlackoutWindowCronExpressions() != null &&
                    !beforeModification.getBlackoutWindowCronExpressions().equals(this.contextTemplate.getBlackoutWindowCronExpressions())) ||
                    (beforeModification.getBlackoutWindowCronExpressions() != null && this.contextTemplate.getBlackoutWindowCronExpressions() == null) ||
                    (beforeModification.getBlackoutWindowCronExpressions() == null && this.contextTemplate.getBlackoutWindowCronExpressions() != null) ||
                    (beforeModification.getBlackoutWindowDateTimeRanges() != null && this.contextTemplate.getBlackoutWindowDateTimeRanges() != null &&
                    !beforeModification.getBlackoutWindowDateTimeRanges().equals(this.contextTemplate.getBlackoutWindowDateTimeRanges()))) ||
                    (beforeModification.getBlackoutWindowDateTimeRanges() != null && this.contextTemplate.getBlackoutWindowDateTimeRanges() == null) ||
                    (beforeModification.getBlackoutWindowDateTimeRanges() == null && this.contextTemplate.getBlackoutWindowDateTimeRanges() != null)) {
                this.contextInstanceRegistrationService.reSchedule(this.contextTemplate.getName());
            }

            ContextTemplateSavedEventBroadcaster.broadcast(this.contextTemplate);

            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

            String action = String.format("Job plan [%s] has been modified.\nBefore\n[%s]After\n[%s]", this.contextTemplate.getName()
                , objectMapper.writeValueAsString(beforeModification), objectMapper.writeValueAsString(this.contextTemplate));
            this.systemEventLogger.logEvent(SystemEventConstants.JOB_PLAN_MODIFIED, action, authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.error-saving-context-template"
                , UI.getCurrent().getLocale()));
            return;
        }

        NotificationHelper.showUserNotification(getTranslation("notification.context-template-saved-successfully"
            , UI.getCurrent().getLocale()));
        this.close();
    }


    /**
     * Helper method to load the existing record for the context.
     *
     * @return
     */
    private ScheduledContextRecord loadContextTemplateRecord() {
        return this.scheduledContextService.findByName(this.contextTemplate.getName());
    }
}
