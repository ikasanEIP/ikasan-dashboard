package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.Divider;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.model.BlackoutWindowDateTimePair;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateSavedEventBroadcaster;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.quartz.CronExpression;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextTemplateDialog extends AbstractCloseableResizableDialog {
    private ScheduledContextService scheduledContextService;
    private SchedulerJobService schedulerJobService;
    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;
    private TextField endWindowCronExpressionTf;
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

    /**
     * Constructor
     *
     * @param scheduledContextService
     * @param schedulerJobService
     * @param title
     * @param editName
     */
    public ContextTemplateDialog(ScheduledContextService scheduledContextService, SchedulerJobService schedulerJobService, String title,
                                 boolean editName) {
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        if(title == null) {
            throw new IllegalArgumentException("title cannot be null!");
        }
        super.title.setText(title);

        this.editName = editName;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.init();
    }

    /**
     * Initialise the layout of the dialog.
     */
    private void init() {
        this.blackoutWindowDateTimePairs = new ArrayList<>();
        this.setHeight("740px");
        this.setWidth("90vw");

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
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);
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
        this.endWindowCronExpressionTf.getElement().getThemeList().add("always-float-label");
        this.endWindowCronExpressionTf.setSuffixComponent(endWindowCronBuilderIcon);
        binder.forField(endWindowCronExpressionTf)
            .asRequired(getTranslation("error.missing-cron-expression", UI.getCurrent().getLocale()))
            .withValidator(value -> CronExpression.isValidExpression(value), getTranslation("error.invalid-cron-expression", UI.getCurrent().getLocale()))
            .bind(ContextTemplate::getTimeWindowEnd, ContextTemplate::setTimeWindowEnd);

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


        Button addDateTimePairButton = new Button("Add", VaadinIcon.PLUS.create());
        addDateTimePairButton.setIconAfterText(true);
        addDateTimePairButton.addClickListener(event -> {
            this.blackoutWindowDateTimePairs.add(new BlackoutWindowDateTimePair());
            blackoutWindowsGrid.getDataProvider().refreshAll();
        });

        this.formLayout = new FormLayout();
        this.formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("500px", 20)
        );
        this.formLayout.setWidth("100%");
        this.formLayout.add(this.contextNameTf, this.startWindowCronExpressionTf, this.endWindowCronExpressionTf, this.timezoneCb, this.descriptionTa,
            blackoutWindowsGrid, addDateTimePairButton);
        this.formLayout.setColspan(this.contextNameTf, 8);
        this.formLayout.setColspan(this.startWindowCronExpressionTf, 4);
        this.formLayout.setColspan(this.endWindowCronExpressionTf, 4);
        this.formLayout.setColspan(this.timezoneCb, 4);
        this.formLayout.setColspan(this.descriptionTa, 8);
        this.formLayout.setColspan(blackoutWindowsGrid, 11);
        this.formLayout.setColspan(addDateTimePairButton, 1);
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

        return isValid && blackoutWindowsDefined.get() && timezoneValid && blackoutWindowsValid.get();
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
     * Save existing if this is managing an existing context.
     */
    private void saveExisting() {
        try {
            this.scheduledContextRecord.setContextName(this.contextTemplate.getName());
            this.scheduledContextRecord.setContext(this.contextTemplate);
            this.scheduledContextRecord.setModifiedBy(this.authentication.getName());
            this.scheduledContextRecord.setTimestamp(System.currentTimeMillis());
            this.scheduledContextService.save(this.scheduledContextRecord);
            ContextTemplateSavedEventBroadcaster.broadcast(this.contextTemplate);
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.error-saving-context-template"
                , UI.getCurrent().getLocale()));
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
