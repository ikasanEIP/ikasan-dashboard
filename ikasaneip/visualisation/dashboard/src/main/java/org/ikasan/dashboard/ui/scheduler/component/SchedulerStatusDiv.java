package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class SchedulerStatusDiv extends Div {
    public static final String STATUS_TRANSLATE_PREFIX = "status.";

    private final UI current;
    private final I18NProvider i18NProvider;

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

        this.current = UI.getCurrent();
        this.i18NProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
    }

    public void setStatus(InstanceStatus status) {
        if(status == null) return;

        this.setStatus(status.name());
    }

    public void setStatus(String status) {
        if(status == null) return;

        if(status.equals(InstanceStatus.COMPLETE.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.COMPLETE.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.WAITING.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_WAITING);
            super.getElement().getStyle().set("color", "#000000");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.WAITING.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.ERROR.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.ERROR.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.ON_HOLD.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.ON_HOLD.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.RUNNING.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RUNNING);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.RUNNING.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.LOCK_QUEUED.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.LOCK_QUEUED.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.SKIPPED.name()) || status.equals(InstanceStatus.SKIPPED_RUNNING.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.SKIPPED.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.SKIPPED_COMPLETE.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.SKIPPED.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.RELEASED.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RELEASED);
            super.getElement().getStyle().set("color", "#000000");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.RELEASED.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.ENDED.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ENDED);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.ENDED.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.DISABLED.name())) {
            super.getElement().getStyle().set("background-color", "#000000");
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.DISABLED.name()
                , this.current.getLocale()));
        }
        else if(status.equals(InstanceStatus.PREPARED.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.IKASAN_ORANGE);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + InstanceStatus.PREPARED.name()
                , this.current.getLocale()));
        }
    }
}
