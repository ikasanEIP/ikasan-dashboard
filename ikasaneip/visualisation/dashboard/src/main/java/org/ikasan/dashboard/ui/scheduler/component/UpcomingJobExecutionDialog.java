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
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

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
        super.title.setText("Scheduled Job Execution Details");

        formBinder = new Binder<>();

        FormLayout formLayout = new FormLayout();
        this.agentNameTf = new TextField("Agent name");
        this.agentNameTf.setEnabled(false);
        formBinder.forField(this.agentNameTf)
            .bind(UpcomingScheduledProcess::getAgentName, UpcomingScheduledProcess::setAgentName);
        formLayout.add(agentNameTf);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        this.agentUrlLf = new TextField("Agent URL");
        this.agentUrlLf.setPrefixComponent(link);
        this.agentUrlLf.setValue(" ");
        formLayout.add(this.agentUrlLf);

        this.jobNameTf = new TextField("Job name");
        this.jobNameTf.setEnabled(false);
        formBinder.forField(this.jobNameTf)
            .bind(UpcomingScheduledProcess::getJobName, UpcomingScheduledProcess::setJobName);
        formLayout.add(jobNameTf);


        this.jobGroupTf = new TextField("Job group");
        this.jobGroupTf.setEnabled(false);
        formBinder.forField(this.jobGroupTf)
            .bind(UpcomingScheduledProcess::getJobGroup, UpcomingScheduledProcess::setJobGroup);
        formLayout.add(jobGroupTf);

        this.jobDescriptionTa = new TextArea("Job description");
        this.jobDescriptionTa.setEnabled(false);
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .bind(UpcomingScheduledProcess::getJobDescription, UpcomingScheduledProcess::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.commandLineTf = new TextArea("Command line");
        this.commandLineTf.setEnabled(false);
        commandLineTf.getStyle().set("minHeight", "100px");
        upcomingScheduledProcess.getProcessExecutionBrokerConfigurationMetaData().getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("commandLine"))
            .findFirst().ifPresent(configurationParameterMetaData -> commandLineTf.setValue((String)configurationParameterMetaData.getValue()));
        formLayout.add(commandLineTf, 2);

        DateFormatter dateFormatter = new DateFormatter();

        this.fireTimeTf = new TextField("Job execution time");
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
