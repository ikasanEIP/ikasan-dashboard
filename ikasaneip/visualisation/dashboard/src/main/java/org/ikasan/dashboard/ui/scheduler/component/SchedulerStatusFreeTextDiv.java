package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.html.Div;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class SchedulerStatusFreeTextDiv extends Div {
    public SchedulerStatusFreeTextDiv() {
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

    public void setStatus(InstanceStatus status, String freeText) {
        if(status == null) return;
        super.setText(freeText);
        this.setStatus(status);
    }

    public void setStatus(InstanceStatus status) {
        if(status == null) return;

        if(status.equals(InstanceStatus.COMPLETE)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
            super.getElement().getStyle().set("color", "#FFF");
        }
        else if(status.equals(InstanceStatus.WAITING)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_WAITING);
            super.getElement().getStyle().set("color", "#000000");
        }
        else if(status.equals(InstanceStatus.ERROR)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
            super.getElement().getStyle().set("color", "#FFF");
        }
        else if(status.equals(InstanceStatus.ON_HOLD)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
            super.getElement().getStyle().set("color", "#FFF");
        }
        else if(status.equals(InstanceStatus.RUNNING)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RUNNING);
            super.getElement().getStyle().set("color", "#FFF");
        }
        else if(status.equals(InstanceStatus.LOCK_QUEUED)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
            super.getElement().getStyle().set("color", "#FFF");
        }
        else if(status.equals(InstanceStatus.SKIPPED) || status.equals(InstanceStatus.SKIPPED_RUNNING.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
            super.getElement().getStyle().set("color", "#FFF");
        }
        else if(status.equals(InstanceStatus.SKIPPED_COMPLETE)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
            super.getElement().getStyle().set("color", "#FFF");
        }
        else if(status.equals(InstanceStatus.RELEASED)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RELEASED);
            super.getElement().getStyle().set("color", "#000000");
        }
        else if(status.equals(InstanceStatus.ENDED)) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ENDED);
            super.getElement().getStyle().set("color", "#FFF");
        }
    }
}
