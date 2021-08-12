package org.ikasan.dashboard.ui.scheduler.component;

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
import org.aspectj.weaver.loadtime.Agent;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

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
        super.title.setText("Scheduled Job Execution Details");

        formBinder = new Binder<>();

        FormLayout formLayout = new FormLayout();
        this.agentNameTf = new TextField("Agent name");
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
            check.getElement().setAttribute("title", "Job execution successful");

            TextField status = new TextField("Execution status");
            status.setValue("success");
            status.setEnabled(false);
            status.setSuffixComponent(check);
            formLayout.add(status);
        }
        else {
            Icon exclamation = VaadinIcon.EXCLAMATION.create();
            exclamation.getStyle().set("color", "#ef5350");
            exclamation.getStyle().set("font-size", "32pt");
            exclamation.getElement().setAttribute("title", "Job execution failed");

            TextField status = new TextField("Execution Status");
            status.setValue("job failed");
            status.setEnabled(false);
            status.setSuffixComponent(exclamation);
            formLayout.add(status);
        }

        this.agentUrlLf = new TextField("Agent URL");
        this.agentUrlLf.setPrefixComponent(link);
        this.agentUrlLf.setValue(" ");
        formLayout.add(this.agentUrlLf, 2);

        this.jobNameTf = new TextField("Job name");
        this.jobNameTf.setEnabled(false);
        formBinder.forField(this.jobNameTf)
            .bind(ScheduledProcessEvent::getJobName, ScheduledProcessEvent::setJobName);
        formLayout.add(jobNameTf);


        this.jobGroupTf = new TextField("Job group");
        this.jobGroupTf.setEnabled(false);
        formBinder.forField(this.jobGroupTf)
            .bind(ScheduledProcessEvent::getJobGroup, ScheduledProcessEvent::setJobGroup);
        formLayout.add(jobGroupTf);

        this.jobDescriptionTa = new TextArea("Job description");
        this.jobDescriptionTa.setEnabled(false);
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .bind(ScheduledProcessEvent::getJobDescription, ScheduledProcessEvent::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.commandLineTf = new TextArea("Command line");
        this.commandLineTf.setEnabled(false);
        commandLineTf.getStyle().set("minHeight", "100px");
        formBinder.forField(this.commandLineTf)
            .bind(ScheduledProcessEvent::getCommandLine, ScheduledProcessEvent::setCommandLine);
        formLayout.add(commandLineTf, 2);

        this.resultOutputTf = new TextField("Result output");
        this.resultOutputTf.setEnabled(false);
        formBinder.forField(this.resultOutputTf)
            .bind(ScheduledProcessEvent::getResultOutput, ScheduledProcessEvent::setResultOutput);
        formLayout.add(resultOutputTf);

        this.resultErrorTf = new TextField("Error output");
        this.resultErrorTf.setEnabled(false);
        formBinder.forField(this.resultErrorTf)
            .bind(ScheduledProcessEvent::getResultError, ScheduledProcessEvent::setResultError);
        formLayout.add(resultErrorTf);

        this.pidTf = new TextField("Process id");
        this.pidTf.setEnabled(false);
        formBinder.forField(this.pidTf)
            .withConverter(new StringToLongConverter("Must be a number!"))
            .bind(ScheduledProcessEvent::getPid, ScheduledProcessEvent::setPid);
        formLayout.add(resultOutputTf);

        this.userTf = new TextField("User");
        this.userTf.setEnabled(false);
        formBinder.forField(this.userTf)
            .bind(ScheduledProcessEvent::getUser, ScheduledProcessEvent::setUser);
        formLayout.add(resultErrorTf);

        DateFormatter dateFormatter = new DateFormatter();

        this.fireTimeTf = new TextField("Job execution time");
        this.fireTimeTf.setEnabled(false);
        this.fireTimeTf.setValue(dateFormatter.getFormattedDate(scheduledProcessEvent.getFireTime()));
        formLayout.add(fireTimeTf);

        this.nextFireTimeTf = new TextField("Next job execution time");
        this.nextFireTimeTf.setEnabled(false);
        this.nextFireTimeTf.setValue(dateFormatter.getFormattedDate(scheduledProcessEvent.getNextFireTime()));
        formLayout.add(nextFireTimeTf);

        this.executionDurationTf = new TextField("Job duration");
        this.executionDurationTf.setEnabled(false);
        if(scheduledProcessEvent.isSuccessful()) {
            this.executionDurationTf.setValue((scheduledProcessEvent.getCompletionTime() - scheduledProcessEvent.getFireTime()) + " milliseconds");
        }
        else{
            this.executionDurationTf.setValue("Not applicable   ");
        }
        formLayout.add(executionDurationTf);

        this.returnCodeTf = new TextField("Return code");
        this.returnCodeTf.setEnabled(false);
        formBinder.forField(this.returnCodeTf)
            .withConverter(new StringToIntegerConverter("Must be a number!"))
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
