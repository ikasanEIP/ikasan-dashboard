package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.scheduled.event.model.UpcomingScheduledProcess;
import org.ikasan.spec.metadata.ModuleMetaData;

public class UpcomingJobExecutionDialog extends AbstractCloseableResizableDialog implements BeforeEnterListener {

    private TextField agentNameTf;
    private TextField agentUrlLf;
    private TextField jobNameTf;
    private TextField jobGroupTf;
    private TextArea jobDescriptionTa;
    private TextArea commandLineTf;
    private TextField fireTimeTf;

    private Binder<UpcomingScheduledProcess> formBinder;

    /**
     * Constructor
     *
     * @param upcomingScheduledProcess
     * @param agent
     */
    public UpcomingJobExecutionDialog(UpcomingScheduledProcess upcomingScheduledProcess, ModuleMetaData agent) {
        super.showResize(false);
        super.title.setText(getTranslation("header.scheduled-job-execution-details", UI.getCurrent().getLocale()));



        formBinder = new Binder<>();

        FormLayout formLayout = new FormLayout();
        this.agentNameTf = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentNameTf.setEnabled(false);
        formBinder.forField(this.agentNameTf)
            .bind(UpcomingScheduledProcess::getAgentName, UpcomingScheduledProcess::setAgentName);
        formLayout.add(agentNameTf);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        this.agentUrlLf = new TextField(getTranslation("label.agent-url", UI.getCurrent().getLocale()));
        this.agentUrlLf.setPrefixComponent(link);
        this.agentUrlLf.setValue(" ");
        formLayout.add(this.agentUrlLf);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setEnabled(false);
        formBinder.forField(this.jobNameTf)
            .bind(UpcomingScheduledProcess::getJobName, UpcomingScheduledProcess::setJobName);
        formLayout.add(jobNameTf);


        this.jobGroupTf = new TextField(getTranslation("label.job-group", UI.getCurrent().getLocale()));
        this.jobGroupTf.setEnabled(false);
        formBinder.forField(this.jobGroupTf)
            .bind(UpcomingScheduledProcess::getJobGroup, UpcomingScheduledProcess::setJobGroup);
        formLayout.add(jobGroupTf);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setEnabled(false);
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .bind(UpcomingScheduledProcess::getJobDescription, UpcomingScheduledProcess::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.commandLineTf = new TextArea(getTranslation("label.command-line", UI.getCurrent().getLocale()));
        this.commandLineTf.setEnabled(false);
        commandLineTf.getStyle().set("minHeight", "100px");
//        upcomingScheduledProcess.getProcessExecutionBrokerConfigurationMetaData().getParameters().stream()
//            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("commandLine"))
//            .findFirst().ifPresent(configurationParameterMetaData -> commandLineTf.setValue((String)configurationParameterMetaData.getValue()));
//        formLayout.add(commandLineTf, 2);

        DateFormatter dateFormatter = new DateFormatter();

        this.fireTimeTf = new TextField(getTranslation("label.job-execution-time", UI.getCurrent().getLocale()));
        this.fireTimeTf.setEnabled(false);
        this.fireTimeTf.setValue(dateFormatter.getFormattedDate(upcomingScheduledProcess.getFireTime()));
        formLayout.add(fireTimeTf);

        this.formBinder.readBean(upcomingScheduledProcess);

        this.setHeight("600px");
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
