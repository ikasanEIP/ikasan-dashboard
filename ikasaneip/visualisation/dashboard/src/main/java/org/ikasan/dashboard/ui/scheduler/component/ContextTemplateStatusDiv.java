package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class ContextTemplateStatusDiv extends Div {
    public static final String STATUS_TRANSLATE_PREFIX = "status.";

    private final UI current;
    private final I18NProvider i18NProvider;

    public ContextTemplateStatusDiv() {
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

    public void setStatus(boolean enabled) {

        if(enabled) {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + "enabled"
                , this.current.getLocale()));
        }
        else {
            super.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
            super.getElement().getStyle().set("color", "#FFF");
            super.setText(this.i18NProvider.getTranslation(STATUS_TRANSLATE_PREFIX + "disabled"
                , this.current.getLocale()));
        }
    }
}
