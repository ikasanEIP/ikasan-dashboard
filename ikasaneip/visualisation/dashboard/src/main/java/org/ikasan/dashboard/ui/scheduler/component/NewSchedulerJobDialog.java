package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class NewSchedulerJobDialog extends AbstractCloseableResizableDialog {

    public NewSchedulerJobDialog() {
        this.showResize(false);

        super.title.setText("New Scheduler Job");

        Div content = new Div();
        content.add("I will have a form to create a job");

        super.content.add(content);

        this.setWidth("400px");
        this.setHeight("500px");
    }
}
