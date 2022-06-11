package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.html.Div;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class SchedulerStatusDiv extends Div {
    public SchedulerStatusDiv() {
        super.getElement().getStyle().set("font-size", "16pt");
        super.getElement().getStyle().set("display", "flex");
        super.getElement().getStyle().set("align-items", "center");
        super.getElement().getStyle().set("text-align", "center");
        super.getElement().getStyle().set("color", "#FFF");
        super.getElement().getStyle().set("margin-top", "10px");
        super.getElement().getStyle().set("height", "30px");
        super.getElement().getStyle().set("border-radius", "5px");
        super.getElement().getStyle().set("position", "relative");
        super.getElement().getStyle().set("justify-content", "center");
    }

    public void setStatus(InstanceStatus status) {
        if(status == null) return;

        this.setStatus(status.name());
    }

    public void setStatus(String status) {
        if(status == null) return;

        if(status.equals(InstanceStatus.COMPLETE.name())) {
            super.getElement().getStyle().set("background-color", "#66bb6a");
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(InstanceStatus.COMPLETE.name());
        }
        else if(status.equals(InstanceStatus.WAITING.name())) {
            super.getElement().getStyle().set("background-color", "rgba(210, 215, 211, 1)");
            super.getElement().getStyle().set("color", "#000000");
            super.setText(InstanceStatus.WAITING.name());
        }
        else if(status.equals(InstanceStatus.ERROR.name())) {
            super.getElement().getStyle().set("background-color", "#ef5350");
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(InstanceStatus.ERROR.name());
        }
        else if(status.equals(InstanceStatus.ON_HOLD.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(InstanceStatus.ON_HOLD.name());
        }
        else if(status.equals(InstanceStatus.RUNNING.name())) {
            super.getElement().getStyle().set("background-color", "rgba(133,181,225,1.0)");
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(InstanceStatus.RUNNING.name());
        }
        else if(status.equals(InstanceStatus.SKIPPED.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(InstanceStatus.SKIPPED.name());
        }
        else if(status.equals(InstanceStatus.RELEASED.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RELEASED);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(InstanceStatus.RELEASED.name());
        }
    }
}
