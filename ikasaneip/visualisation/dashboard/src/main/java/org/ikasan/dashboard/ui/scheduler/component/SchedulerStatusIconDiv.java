package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class SchedulerStatusIconDiv extends Div {
    public static final String STATUS_TRANSLATE_PREFIX = "status.";

    private final UI current;
    private final I18NProvider i18NProvider;

    public SchedulerStatusIconDiv() {
        super.getElement().getStyle().set("font-size", "14pt");
        super.getElement().getStyle().set("display", "flex");
        super.getElement().getStyle().set("align-items", "center");
        super.getElement().getStyle().set("text-align", "center");
        super.getElement().getStyle().set("color", "#FFF");
        super.getElement().getStyle().set("margin-top", "10px");
        super.getElement().getStyle().set("height", "30px");
        super.getElement().getStyle().set("width", "30px");
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

        if(status.equals(InstanceStatus.ON_HOLD.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ON_HOLD);
            super.getElement().getStyle().set("color", "#FFF");
            super.add(IconDecorator.decorate(VaadinIcon.HAND.create(), getTranslation("tooltip.context-nested-on-hold", UI.getCurrent().getLocale()), "14pt", "#FFF"));
        }
        else if(status.equals(InstanceStatus.SKIPPED.name()) || status.equals(InstanceStatus.SKIPPED_RUNNING.name())) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_SKIPPED);
            super.getElement().getStyle().set("color", "#FFF");
            super.add(IconDecorator.decorate(VaadinIcon.BAN.create(), getTranslation("tooltip.context-nested-skipped", UI.getCurrent().getLocale()), "14pt", "#FFF"));
        }
        else if(status.equals(InstanceStatus.DISABLED.name())) {
            super.getElement().getStyle().set("background-color", "#000000");
            super.getElement().getStyle().set("color", "#FFF");
            super.add(IconDecorator.decorate(VaadinIcon.CLOCK.create(), getTranslation("tooltip.context-nested-disabled", UI.getCurrent().getLocale()), "14pt", "#FFF"));
        }
    }
}
