package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.quartz.CronExpression;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

public class CloneContextTemplateDialog extends AbstractCloseableResizableDialog {
    private ScheduledContextService scheduledContextService;
    private TextField contextNameTf;
    private TextArea descriptionTa;
    private TextField startWindowCronExpressionTf;

    private IntegerField contextTtlMinutes;
    private IntegerField contextTtlHours;
    private IntegerField contextTtlDays;

    private ContextTemplate contextTemplate = new ContextTemplateImpl();
    private Binder<ContextTemplate> binder = new Binder<>(ContextTemplate.class);

    private FormLayout formLayout;

    private ContextTemplateFilteringGrid contextTemplateFilteringGrid;

    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    private SchedulerJobService schedulerJobService;

    private ContextProfileService contextProfileService;

    private IkasanAuthentication authentication;

    public CloneContextTemplateDialog(ScheduledContextService scheduledContextService, ContextTemplateFilteringGrid contextTemplateFilteringGrid,
                                      ContextInstanceRegistrationService contextInstanceRegistrationService, SchedulerJobService schedulerJobService,
                                      ContextProfileService contextProfileService, ScheduledContextRecord contextToClone) {
        this.scheduledContextService = scheduledContextService;
        this.contextTemplateFilteringGrid = contextTemplateFilteringGrid;
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        this.schedulerJobService = schedulerJobService;
        this.contextProfileService = contextProfileService;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.contextTemplate = contextToClone.getContext();
        this.contextTemplate.setName(null);

        this.setHeight("440px");
        this.setWidth("90vw");

        super.showResize(false);
        super.setResizable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        super.title.setText(String.format(getTranslation("label.clone-context-template", UI.getCurrent().getLocale())));

        this.init();

        layout.add(this.formLayout);

        Button cloneButton = new Button(getTranslation("button.clone", UI.getCurrent().getLocale()));
        cloneButton.setId("cloneContextButton");
        cloneButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {
            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            this.contextTtlDays.setInvalid(this.contextTtlDays.isInvalid() || this.contextTtlDays.getValue() == null);
            this.contextTtlHours.setInvalid(contextTtlHours.isInvalid() || this.contextTtlHours.getValue() == null);
            this.contextTtlMinutes.setInvalid(contextTtlMinutes.isInvalid() || this.contextTtlMinutes.getValue() == null);

            if(this.binder.validate().isOk()) {
                try {
                    binder.writeBean(this.contextTemplate);
                    if(this.contextTtlDays.isInvalid() || this.contextTtlHours.isInvalid() || this.contextTtlMinutes.isInvalid()) {
                        NotificationHelper.showErrorNotification(getTranslation("error.error-validating-context-template-form"
                            , UI.getCurrent().getLocale()));
                        return;
                    }
                } catch (ValidationException e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.error-validating-context-template-form"
                        , UI.getCurrent().getLocale()));
                    return;
                }

                boolean inError = false;
                try {
                    ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
                    scheduledContextRecord.setContext(this.contextTemplate);
                    scheduledContextRecord.setContextName(this.contextTemplate.getName());
                    scheduledContextRecord.setModifiedBy(authentication.getName());
                    scheduledContextRecord.setTimestamp(System.currentTimeMillis());

                    SearchResults<SchedulerJobRecord> scheduledContextRecordSearchResults
                        = this.schedulerJobService.findByContext(contextToClone.getContextName(), -1, -1);

                    List<SchedulerJob> clonedJobs = new ArrayList<>();
                    scheduledContextRecordSearchResults.getResultList().forEach(context -> {
                        SchedulerJob job = context.getJob();
                        job.setContextName(this.contextTemplate.getName());
                        clonedJobs.add(job);
                    });

                    ContextProfileSearchFilter searchFilter = new SolrContextProfileSearchFilterImpl();
                    searchFilter.setContextName(contextToClone.getContextName());
                    SearchResults<ContextProfileRecord> searchResults
                        = contextProfileService.findByFilter(searchFilter, -1, -1, null, null);

                    searchResults.getResultList().forEach(contextProfileRecord -> {
                        contextProfileRecord.setContextName(contextTemplate.getName());
                    });

                    this.schedulerJobService.save(clonedJobs, authentication.getName());
                    this.scheduledContextService.save(scheduledContextRecord);

                    this.contextInstanceRegistrationService.register(this.contextTemplate.getName());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    inError = true;
                    NotificationHelper.showErrorNotification(getTranslation("error.error-creating-new-context-template"
                        , UI.getCurrent().getLocale()));
                }

                this.contextTemplateFilteringGrid.getDataProvider().refreshAll();

                if(!inError) {
                    NotificationHelper.showUserNotification(getTranslation("notification.context-template-created-successfully"
                        , UI.getCurrent().getLocale()));
                    this.close();
                }
            }
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(cloneButton, cancelButton);
        buttonLayout.getElement().getStyle().set("position", "absolute");
        buttonLayout.getElement().getStyle().set("bottom", "20px");
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, cloneButton, cancelButton);
        layout.add(buttonLayout);

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);


        super.content.add(layout);
    }

    private void init() {
        this.contextNameTf = new TextField(getTranslation("label.context-name", UI.getCurrent().getLocale()));
        this.contextNameTf.getElement().getThemeList().add("always-float-label");
        binder.forField(contextNameTf)
            .asRequired(getTranslation("error.context-name-required", UI.getCurrent().getLocale()))
            .withValidator(name -> scheduledContextService.findByName(name) == null, "A context with this name already exists. Context names must be unique!")
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

        HorizontalLayout ttlLayout = new HorizontalLayout(contextTtlDays, contextTtlHours, contextTtlMinutes);
        ttlLayout.setWidthFull();
        ttlLayout.setPadding(false);
        ttlLayout.setMargin(false);

        binder.readBean(this.contextTemplate);

        this.formLayout = new FormLayout();
        this.formLayout.setWidth("100%");
        this.formLayout.add(this.contextNameTf, this.startWindowCronExpressionTf, this.descriptionTa, ttlLayout);
    }
}
