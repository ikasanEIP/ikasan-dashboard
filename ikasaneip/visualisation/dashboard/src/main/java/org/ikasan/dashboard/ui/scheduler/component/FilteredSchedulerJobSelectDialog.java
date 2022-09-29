package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;

public class FilteredSchedulerJobSelectDialog extends AbstractCloseableResizableDialog {
    private SchedulerJobSelectGridWidget schedulerJobSelectGridWidget;

    public FilteredSchedulerJobSelectDialog(SchedulerJobService schedulerJobService, ContextTemplate contextTemplate,
                                            SchedulerJobSearchFilter schedulerJobSearchFilter, String headerLabel, String bodyLabel) {
        this.schedulerJobSelectGridWidget = new SchedulerJobSelectGridWidget(schedulerJobService, contextTemplate, this,
            schedulerJobSearchFilter, bodyLabel);

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
