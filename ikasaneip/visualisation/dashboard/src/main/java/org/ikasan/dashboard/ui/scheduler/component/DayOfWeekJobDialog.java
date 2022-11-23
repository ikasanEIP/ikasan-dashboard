package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.componentfactory.gridlayout.GridLayout;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.search.component.IkasanSearchHelpDialog;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SecurityConstants;

import java.util.ArrayList;
import java.util.List;

public class DayOfWeekJobDialog extends AbstractCloseableResizableDialog {

    private boolean isSaveClose = false;
    private List<Integer> daysOfWeek;
    private boolean editable;

    public DayOfWeekJobDialog(List<Integer> daysOfWeek, boolean editable) {
        this.daysOfWeek = daysOfWeek;
        this.editable = editable;

        if(daysOfWeek == null) {
            this.daysOfWeek = new ArrayList<>();
        }

        super.showResize(false);
        super.title.setText(getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()));

        init();
    }

    private void init() {

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.setVisible(this.editable);
        okButton.addClickListener(event -> {
            this.isSaveClose = true;
            this.close();
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.setVisible(this.editable);
        cancelButton.addClickListener(event -> this.close());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.add(okButton, cancelButton);

        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setWidth("100%");
        buttonLayout.add(buttons);
        buttonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttons);


        super.content.add(this.getSpecificDayOfWeek().getComponent(), buttonLayout);


        this.setWidth("1000px");
        this.setHeight("200px");
    }

    public boolean isSaveClose() {
        return isSaveClose;
    }


    private TimeComponent getSpecificDayOfWeek() {

        List<Checkbox> dayStart = new ArrayList<>();
        dayStart.add(this.createDayCheckbox("Monday", "label.monday"));
        dayStart.add(this.createDayCheckbox("Tuesday", "label.tuesday"));
        dayStart.add(this.createDayCheckbox("Wednesday", "label.wednesday"));
        dayStart.add(this.createDayCheckbox("Thursday", "label.thursday"));
        dayStart.add(this.createDayCheckbox("Friday", "label.friday"));
        dayStart.add(this.createDayCheckbox("Saturday", "label.saturday"));
        dayStart.add(this.createDayCheckbox("Sunday", "label.sunday"));

        GridLayout layout = new GridLayout(8, 1);
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-day-of-the-week"
            , UI.getCurrent().getLocale()), layout);

        dayStart.forEach(item -> {
            layout.addComponent(item);

            if(this.daysOfWeek.contains(this.dayOfWeek(item.getId().get()))) {
                item.setValue(true);
            }

            item.addValueChangeListener(event -> {
                this.daysOfWeek.clear();
                dayStart.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        this.daysOfWeek.add(this.dayOfWeek(checkbox.getId().get()));
                    }
                });
            });

            item.setEnabled(this.editable &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        });

        Icon helpIcon = new Icon(VaadinIcon.QUESTION_CIRCLE);
        helpIcon.setSize("18px");
        Button helpButton = new Button("Help", helpIcon);
        helpButton.setId("helpButton");
        helpButton.setIconAfterText(false);
        helpButton.setHeight("32px");

        helpButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            DayOfWeekJobHelpDialog dayOfWeekJobHelpDialog = new DayOfWeekJobHelpDialog();
            dayOfWeekJobHelpDialog.open();
        });

        layout.addComponent(helpButton);

        layout.setSizeFull();

        return timeComponent;
    }

    private Integer dayOfWeek(String textDay) {
        switch (textDay){
            case "Sunday" : return 1;
            case "Monday" : return 2;
            case "Tuesday" : return 3;
            case "Wednesday" : return 4;
            case "Thursday" : return 5;
            case "Friday" : return 6;
            case "Saturday" : return 7;
            default: return null;
        }
    }

    private class TimeComponent {
        private String name;
        private Component component;
        private String value;

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

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private Checkbox createDayCheckbox(String id, String dayTranslationKey) {
        Checkbox dayCheckbox = new Checkbox(getTranslation(dayTranslationKey
            , UI.getCurrent().getLocale()));
        dayCheckbox.setId(id);



        return dayCheckbox;
    }

    public List<Integer> getDaysOfWeek() {
        if(daysOfWeek.isEmpty()) return null;
        return daysOfWeek;
    }
}