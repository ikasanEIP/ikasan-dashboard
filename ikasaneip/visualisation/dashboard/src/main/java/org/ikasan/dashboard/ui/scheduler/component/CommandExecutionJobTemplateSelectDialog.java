package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;

public class CommandExecutionJobTemplateSelectDialog extends AbstractCloseableResizableDialog {
    private CommandExecutionJobSelectGridWidget schedulerJobSelectGridWidget;

    public CommandExecutionJobTemplateSelectDialog(SchedulerJobService schedulerJobService, ContextTemplate contextTemplate, String headerLabel, String bodyLabel) {
        this.schedulerJobSelectGridWidget = new CommandExecutionJobSelectGridWidget(schedulerJobService, contextTemplate, this, bodyLabel);

        this.setHeight("90vh");
        this.setWidth("90vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.schedulerJobSelectGridWidget);
        layout.getStyle().set("padding-bottom", "20px");

        super.title.setText(headerLabel);
        super.content.add(layout);
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectGridWidget.addSchedulerJobSelectedListener(listener);
    }

    public void removeSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectGridWidget.removeSchedulerJobSelectedListener(listener);
    }
}
