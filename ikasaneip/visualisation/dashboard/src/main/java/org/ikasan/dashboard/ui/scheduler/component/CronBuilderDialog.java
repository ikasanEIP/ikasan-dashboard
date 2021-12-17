package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class CronBuilderDialog extends AbstractCloseableResizableDialog {

    private Tab secondsTab;
    private Tab minutesTab;
    private Tab hoursTab;
    private Tab daysTab;
    private Tabs tabs;

    public CronBuilderDialog() {
        super.showResize(false);
        // todo translation
        super.title.setText("Cron Builder");

        // todo translation
        this.secondsTab = new Tab("Seconds");
        this.secondsTab.setId("secondsTab");
        this.minutesTab = new Tab("Minutes");
        this.minutesTab.setId("minutesTab");
        this.hoursTab = new Tab("Hours");
        this.hoursTab.setId("hoursTab");
        this.daysTab = new Tab("Days");
        this.daysTab.setId("daysTab");
        this.tabs = new Tabs(this.secondsTab, this.minutesTab, this.hoursTab, this.daysTab);

        Component secondsLayout = this.getSecondsLayout();

        Map<Tab, Component> tabsToPages = new HashMap<>();
        tabsToPages.put(this.secondsTab, secondsLayout);
        tabsToPages.put(this.minutesTab, new Label("Minutes"));
        tabsToPages.put(this.hoursTab, new Label("Hours"));
        tabsToPages.put(this.daysTab, new Label("Days"));

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });

        List<String> minutes = new ArrayList<>();
        IntStream.range(1, 59).forEach(i -> minutes.add(Integer.toString(i)));
        Select<String> select = new Select<>();
        select.setItems(minutes);

        super.content.add(tabs, secondsLayout, select);

        this.setWidth("900px");
        this.setHeight("500px");
    }

    private Component getSecondsLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();
        radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

        radioGroup.setItems(List.of(getEverySecond(), getEverySecondStartingAt()));
        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> timeComponent.getComponent()));

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.add(radioGroup);

        return horizontalLayout;
    }

    private TimeComponent getEverySecond() {
        Label label = new Label("Every Second");
        TimeComponent timeComponent = new TimeComponent("Every Second", label);

        return timeComponent;
    }

    private TimeComponent getEverySecondStartingAt() {
        Label label = new Label("Every ");
        List<String> minutes = new ArrayList<>();
        IntStream.range(1, 59).forEach(i -> minutes.add(Integer.toString(i)));
        Select<String> select = new Select<>();
        select.setItems(minutes);

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, select);

        TimeComponent timeComponent = new TimeComponent("Every Second Starting At", layout);

        return timeComponent;
    }

    private class TimeComponent {
        private String name;
        private Component component;

        public TimeComponent(String name, Component component) {
            this.name = name;
            this.component = component;
        }

        public String getName() {
            return name;
        }

        public Component getComponent() {
            return component;
        }
    }
}
