package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import org.vaadin.stefan.fullcalendar.FullCalendar;

@NpmPackage(value = "tippy.js", version = "6.2.3")
@Tag("full-calendar-with-tooltip")
@JsModule("./full-calendar-with-tooltip.js")
@CssImport("tippy.js/dist/tippy.css")
@CssImport("tippy.js/themes/light.css")
public class FullCalendarWithTooltip extends FullCalendar {
	private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param entryLimit
     */
    public FullCalendarWithTooltip(int entryLimit) {
        super(entryLimit);
    }
}