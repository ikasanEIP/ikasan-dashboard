package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

public class ContextDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(ContextDialog.class);

    // Fields to capture schedule job properties.
    private TextField contextNameTf;
    private TextArea contextDescriptionTa;
    private TextField timeWindowStartExpressionTf;
    private TextField timeWindowEndExpressionTf;
    private ComboBox<DateTimeUtil.TimezonePair> timezoneCb;

    private Button saveButton;
    private Button cancelButton;


    private ContextTemplateImpl contextTemplate;

    private SolrQuartzScheduleDrivenJobImpl quartzScheduleDrivenJob;

    private Binder<ContextTemplateImpl> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;

    private ScheduledContextService scheduledContextService;

    /**
     * Constructor
     *
     * @param systemEventLogger
     */
    public ContextDialog(SystemEventLogger systemEventLogger, ScheduledContextService scheduledContextService) {
        super.showResize(false);
        // todo translation
        super.title.setText("Context");
        this.systemEventLogger = systemEventLogger;
        this.scheduledContextService = scheduledContextService;
        this.contextTemplate = new ContextTemplateImpl();

        this.quartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();


        this.formBinder
            = new Binder<>(ContextTemplateImpl.class);

        this.setHeight("600px");
        this.setWidth("1200px");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("scheduledJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            if(!this.performFormValidation(this.contextTemplate)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            try {
                createOrUpdateContext(this.contextTemplate);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                return;
            }

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if (this.editMode == EditMode.NEW) {
                String action = String.format("New quartz scheduled job created [%s].", this.quartzScheduleDrivenJob);
                this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
            }
            else if (this.editMode == EditMode.EDIT) {
                String action = String.format("Quartz scheduled job edited. \nBefore [%s]\nAfter [%s].", this.quartzScheduleDrivenJob,
                    this.quartzScheduleDrivenJob);
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_EDIT, action, authentication.getName());
            }

            this.close();
        });

        cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(true);
        buttonLayout.setSpacing(true);
        buttonLayout.add(saveButton, cancelButton);
        buttonLayout.getStyle().set("padding-bottom", "20px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.createConfigurationForm(), buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        layout.getStyle().set("padding-bottom", "20px");
        super.content.add(layout);
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();

        // Fields to capture context.
        H3 scheduleDetailsLabel = new H3(getTranslation("header.context", UI.getCurrent().getLocale()));
        formLayout.add(scheduleDetailsLabel, 2);

        this.contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        this.contextNameTf.setId("jobNameTf");
        this.contextNameTf.setRequired(true);
        this.contextNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.contextNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-context-name", UI.getCurrent().getLocale()))
            .bind(ContextTemplateImpl::getName, ContextTemplateImpl::setName);
        formLayout.add(contextNameTf, 2);


        this.contextDescriptionTa = new TextArea(getTranslation("label.context-description", UI.getCurrent().getLocale()));
        this.contextDescriptionTa.setRequired(true);
        this.contextDescriptionTa.setId("contextDescriptionTa");
        contextDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.contextDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-context-description", UI.getCurrent().getLocale()))
            .bind(ContextTemplateImpl::getDescription, ContextTemplateImpl::setDescription);
        formLayout.add(contextDescriptionTa, 2);


        Icon timeWindowStartBuilderIcon = IconDecorator.decorate(VaadinIcon.BUILDING_O.create(), "Build cron expression", "14pt", "rgba(241, 90, 35, 1.0)");
        timeWindowStartBuilderIcon.addClickListener(event -> {
            CronBuilderDialog dialog = new CronBuilderDialog();
            dialog.init(this.timeWindowStartExpressionTf.getValue());
            dialog.open();

            dialog.addOpenedChangeListener(openedChangeEvent -> {
               if(!openedChangeEvent.isOpened() && dialog.isSaveClose()) {
                   this.timeWindowStartExpressionTf.setValue(dialog.getCronExpression());
               }
            });
        });

        this.timeWindowStartExpressionTf = new TextField(getTranslation("label.context-time-window-start-cron-expression", UI.getCurrent().getLocale()));
        this.timeWindowStartExpressionTf.setRequired(true);
        this.timeWindowStartExpressionTf.setId("timeWindowStartExpressionTf");
        this.timeWindowStartExpressionTf.setSuffixComponent(timeWindowStartBuilderIcon);
        formBinder.forField(this.timeWindowStartExpressionTf)
            .withValidator(value -> !value.isEmpty(), getTranslation("error.missing-cron-expression", UI.getCurrent().getLocale()))
            .withValidator(value -> CronExpression.isValidExpression(value), getTranslation("error.invalid-cron-expression", UI.getCurrent().getLocale()))
            .bind(ContextTemplateImpl::getTimeWindowStart, ContextTemplateImpl::setTimeWindowStart);
        formLayout.add(timeWindowStartExpressionTf);

        Icon timeWindowEndBuilderIcon = IconDecorator.decorate(VaadinIcon.BUILDING_O.create(), "Build cron expression", "14pt", "rgba(241, 90, 35, 1.0)");
        timeWindowEndBuilderIcon.addClickListener(event -> {
            CronBuilderDialog dialog = new CronBuilderDialog();
            dialog.init(this.timeWindowEndExpressionTf.getValue());
            dialog.open();

            dialog.addOpenedChangeListener(openedChangeEvent -> {
                if(!openedChangeEvent.isOpened() && dialog.isSaveClose()) {
                    this.timeWindowEndExpressionTf.setValue(dialog.getCronExpression());
                }
            });
        });

        this.timeWindowEndExpressionTf = new TextField(getTranslation("label.context-time-window-end-cron-expression", UI.getCurrent().getLocale()));
        this.timeWindowEndExpressionTf.setRequired(true);
        this.timeWindowEndExpressionTf.setId("timeWindowEndExpressionTf");
        this.timeWindowEndExpressionTf.setSuffixComponent(timeWindowEndBuilderIcon);
        formBinder.forField(this.timeWindowEndExpressionTf)
            .withValidator(value -> !value.isEmpty(), getTranslation("error.missing-cron-expression", UI.getCurrent().getLocale()))
            .withValidator(value -> CronExpression.isValidExpression(value), getTranslation("error.invalid-cron-expression", UI.getCurrent().getLocale()))
            .bind(ContextTemplateImpl::getTimeWindowStart, ContextTemplateImpl::setTimeWindowStart);
        formLayout.add(timeWindowEndExpressionTf);


        this.timezoneCb = new ComboBox<>(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        ComboBox.ItemFilter<DateTimeUtil.TimezonePair> filter = (element, filterString) ->
            element.zoneId.toLowerCase().contains(filterString.toLowerCase());
        this.timezoneCb.setId("timezoneCb");
        this.timezoneCb.setItems(filter, DateTimeUtil.getAllZoneIdsAndItsOffSet());
        this.timezoneCb.setItemLabelGenerator((ItemLabelGenerator<DateTimeUtil.TimezonePair>) s -> String.format("%35s (UTC%s) %n", s.zoneId, s.offset).trim());
        this.timezoneCb.setClearButtonVisible(true);
        this.timezoneCb.setPlaceholder(getTranslation("label.choose-a-timezone", UI.getCurrent().getLocale()));
        this.timezoneCb.setErrorMessage(getTranslation("error.timezone-required", UI.getCurrent().getLocale()));
        formLayout.add(timezoneCb);

        return formLayout;
    }

    /**
     * Perform validation of the form.
     *
     * @param contextTemplate
     * @return
     */
    private boolean performFormValidation(ContextTemplateImpl contextTemplate) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);

            if(this.timezoneCb.getValue() != null) {
                contextTemplate.setTimezone(this.timezoneCb.getValue().zoneId);
            }

            formBinder.writeBean(contextTemplate);

            if(!isValid.get()){
                return false;
            }
        }
        catch (ValidationException e) {
            return false;
        }

        return true;
    }
    

    /**
     *
     *
     * @param contextTemplate
     */
    public void createOrUpdateContext(ContextTemplateImpl contextTemplate) {
        ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
        scheduledContextRecord.setTimestamp(System.currentTimeMillis());
        scheduledContextRecord.setContextName(contextTemplate.getName());
        scheduledContextRecord.setContext(contextTemplate);

        this.scheduledContextService.save(scheduledContextRecord);
    }


    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.timezoneCb.setEnabled(enabled);

        this.contextNameTf.setEnabled(this.editMode == EditMode.NEW);
        this.contextDescriptionTa.setEnabled(enabled);
        this.timeWindowStartExpressionTf.setEnabled(enabled);
        this.timezoneCb.setEnabled(enabled);

        this.saveButton.setVisible(enabled);
        this.cancelButton.setVisible(enabled);
    }
}
