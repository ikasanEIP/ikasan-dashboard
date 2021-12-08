package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.converter.StringToLongConverter;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;

public class ScheduledProcessExecutionDialog extends AbstractCloseableResizableDialog implements BeforeEnterListener {

    private TextField agentNameTf;
    private TextField agentUrlLf;
    private TextField jobNameTf;
    private TextField jobGroupTf;
    private TextArea jobDescriptionTa;
    private TextArea commandLineTf;
    private TextField resultOutputTf;
    private TextField resultErrorTf;
    private TextField pidTf;
    private TextField userTf;
    private TextField fireTimeTf;
    private TextField nextFireTimeTf;
    private TextField executionDurationTf;
    private TextField returnCodeTf;

    private Binder<ScheduledProcessEvent> formBinder;

    /**
     * Constructor
     *
     * @param scheduledProcessEvent
     * @param agent
     */
    public ScheduledProcessExecutionDialog(ScheduledProcessEvent scheduledProcessEvent, ModuleMetaData agent) {
        super.showResize(false);
        super.title.setText(getTranslation("header.scheduled-job-execution-details", UI.getCurrent().getLocale()));

        formBinder = new Binder<>();

        FormLayout formLayout = new FormLayout();
        this.agentNameTf = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentNameTf.setEnabled(false);
        formBinder.forField(this.agentNameTf)
            .bind(ScheduledProcessEvent::getAgentName, ScheduledProcessEvent::setAgentName);
        formLayout.add(agentNameTf);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        if(scheduledProcessEvent.isSuccessful()) {
            Icon check = VaadinIcon.CHECK.create();
            check.getStyle().set("color", "#66bb6a");
            check.getStyle().set("font-size", "32pt");
            check.getElement().setAttribute("title", getTranslation("tooltip.job-execution-success", UI.getCurrent().getLocale()));

            TextField status = new TextField(getTranslation("label.execution-status", UI.getCurrent().getLocale()));
            status.setValue("success");
            status.setEnabled(false);
            status.setSuffixComponent(check);
            formLayout.add(status);
        }
        else {
            Icon exclamation = VaadinIcon.EXCLAMATION.create();
            exclamation.getStyle().set("color", "#ef5350");
            exclamation.getStyle().set("font-size", "32pt");
            exclamation.getElement().setAttribute("title", getTranslation("tooltip.job-execution-fail", UI.getCurrent().getLocale()));

            TextField status = new TextField(getTranslation("label.execution-status", UI.getCurrent().getLocale()));
            status.setValue("job failed");
            status.setEnabled(false);
            status.setSuffixComponent(exclamation);
            formLayout.add(status);
        }

        this.agentUrlLf = new TextField(getTranslation("label.agent-url", UI.getCurrent().getLocale()));
        this.agentUrlLf.setPrefixComponent(link);
        this.agentUrlLf.setValue(" ");
        formLayout.add(this.agentUrlLf, 2);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setEnabled(false);
        formBinder.forField(this.jobNameTf)
            .bind(ScheduledProcessEvent::getJobName, ScheduledProcessEvent::setJobName);
        formLayout.add(jobNameTf);


        this.jobGroupTf = new TextField(getTranslation("label.job-group", UI.getCurrent().getLocale()));
        this.jobGroupTf.setEnabled(false);
        formBinder.forField(this.jobGroupTf)
            .bind(ScheduledProcessEvent::getJobGroup, ScheduledProcessEvent::setJobGroup);
        formLayout.add(jobGroupTf);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setEnabled(false);
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .bind(ScheduledProcessEvent::getJobDescription, ScheduledProcessEvent::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.commandLineTf = new TextArea(getTranslation("label.command-line", UI.getCurrent().getLocale()));
        this.commandLineTf.setEnabled(false);
        commandLineTf.getStyle().set("minHeight", "100px");
        formBinder.forField(this.commandLineTf)
            .bind(ScheduledProcessEvent::getCommandLine, ScheduledProcessEvent::setCommandLine);
        formLayout.add(commandLineTf, 2);

        this.resultOutputTf = new TextField(getTranslation("label.result-output", UI.getCurrent().getLocale()));
        this.resultOutputTf.setEnabled(false);
        formBinder.forField(this.resultOutputTf)
            .bind(ScheduledProcessEvent::getResultOutput, ScheduledProcessEvent::setResultOutput);
        formLayout.add(resultOutputTf);

        this.resultErrorTf = new TextField(getTranslation("label.error-output", UI.getCurrent().getLocale()));
        this.resultErrorTf.setEnabled(false);
        formBinder.forField(this.resultErrorTf)
            .bind(ScheduledProcessEvent::getResultError, ScheduledProcessEvent::setResultError);
        formLayout.add(resultErrorTf);

        this.pidTf = new TextField(getTranslation("label.process-id", UI.getCurrent().getLocale()));
        this.pidTf.setEnabled(false);
        formBinder.forField(this.pidTf)
            .withConverter(new StringToLongConverter(getTranslation("error.must-be-a-number", UI.getCurrent().getLocale())))
            .bind(ScheduledProcessEvent::getPid, ScheduledProcessEvent::setPid);
        formLayout.add(resultOutputTf);

        this.userTf = new TextField(getTranslation("label.user", UI.getCurrent().getLocale()));
        this.userTf.setEnabled(false);
        formBinder.forField(this.userTf)
            .bind(ScheduledProcessEvent::getUser, ScheduledProcessEvent::setUser);
        formLayout.add(resultErrorTf);

        DateFormatter dateFormatter = new DateFormatter();

        this.fireTimeTf = new TextField(getTranslation("label.job-execution-time", UI.getCurrent().getLocale()));
        this.fireTimeTf.setEnabled(false);
        this.fireTimeTf.setValue(dateFormatter.getFormattedDate(scheduledProcessEvent.getFireTime()));
        formLayout.add(fireTimeTf);

        this.nextFireTimeTf = new TextField(getTranslation("label.next-job-execution-time", UI.getCurrent().getLocale()));
        this.nextFireTimeTf.setEnabled(false);
        this.nextFireTimeTf.setValue(dateFormatter.getFormattedDate(scheduledProcessEvent.getNextFireTime()));
        formLayout.add(nextFireTimeTf);

        this.executionDurationTf = new TextField(getTranslation("label.job-duration", UI.getCurrent().getLocale()));
        this.executionDurationTf.setEnabled(false);
        if(scheduledProcessEvent.isSuccessful()) {
            this.executionDurationTf.setValue((scheduledProcessEvent.getCompletionTime() - scheduledProcessEvent.getFireTime())
                + " " + getTranslation("label.milliseconds", UI.getCurrent().getLocale()));
        }
        else{
            this.executionDurationTf.setValue(getTranslation("label.not-applicable", UI.getCurrent().getLocale()));
        }
        formLayout.add(executionDurationTf);

        this.returnCodeTf = new TextField(getTranslation("label.return-code", UI.getCurrent().getLocale()));
        this.returnCodeTf.setEnabled(false);
        formBinder.forField(this.returnCodeTf)
            .withConverter(new StringToIntegerConverter(getTranslation("error.must-be-a-number", UI.getCurrent().getLocale())))
            .bind(ScheduledProcessEvent::getReturnCode, ScheduledProcessEvent::setReturnCode);
        formLayout.add(returnCodeTf);


        this.formBinder.readBean(scheduledProcessEvent);

        this.setHeight("750px");
        this.setWidth("1200px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(formLayout);
        super.content.add(layout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.agentNameTf.getStyle().set("color", "rgba(0, 0, 0, 0.87) !important");
    }
}
