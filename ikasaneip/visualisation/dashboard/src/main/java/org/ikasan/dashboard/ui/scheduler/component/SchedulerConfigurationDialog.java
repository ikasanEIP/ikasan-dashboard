package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

public class SchedulerConfigurationDialog extends AbstractCloseableResizableDialog {

    private TextField jobNameTf;
    private TextField jobGroupTf;
    private TextArea jobDescriptionTa;
    private TextField cronExpressionTf;
    private ComboBox<String> timezoneCb;
    private TextArea commandLineTf;
    private TextField stdOutTf;
    private TextField stdErrTf;
    private TextField thresholdTf;
    private RadioButtonGroup eagerRg;


    public SchedulerConfigurationDialog() {
        super.showResize(false);
        super.title.setText("Scheduler Configuration");

        FormLayout formLayout = new FormLayout();
        this.jobNameTf = new TextField("Job name");
        formLayout.add(jobNameTf);
        this.jobGroupTf = new TextField("Job group");
        formLayout.add(jobGroupTf);
        this.jobDescriptionTa = new TextArea("Job description");
        jobDescriptionTa.getStyle().set("minHeight", "150px");
        formLayout.add(jobDescriptionTa, 2);
        this.cronExpressionTf = new TextField("Cron expression");
        formLayout.add(cronExpressionTf);
        this.timezoneCb = new ComboBox<>("Timezone");
        ComboBox.ItemFilter<String> filter = (element, filterString) -> {
            return element.toLowerCase().contains(filterString.toLowerCase());
        };
        this.timezoneCb.setItems(filter, this.getOrderedZoneIdsWithOffset());
        this.timezoneCb.setClearButtonVisible(true);
        this.timezoneCb.setPlaceholder("Choose a timezone");
        formLayout.add(timezoneCb);
        this.commandLineTf = new TextArea("Command line");
        formLayout.add(commandLineTf, 2);
        commandLineTf.getStyle().set("minHeight", "150px");
        this.stdOutTf = new TextField("Std out");
        formLayout.add(stdOutTf);
        this.stdErrTf = new TextField("Std err");
        formLayout.add(this.stdErrTf);
        this.thresholdTf = new TextField("Threshold");
        formLayout.add(this.thresholdTf);
        this.eagerRg = new RadioButtonGroup();
        this.eagerRg.setItems(true, false);
        this.eagerRg.setLabel("Eager");
        formLayout.add(this.eagerRg);

        this.setHeight("800px");
        this.setWidth("1000px");

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> save());

        Button deleteButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(true);
        buttonLayout.setSpacing(true);
        buttonLayout.add(saveButton, deleteButton);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(formLayout, buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        super.content.add(layout);
    }

    private List<String> getOrderedZoneIdsWithOffset() {
        Map<String, String> sortedMap = new LinkedHashMap<>();

        Map<String, String> allZoneIdsAndItsOffSet = getAllZoneIdsAndItsOffSet();

        allZoneIdsAndItsOffSet.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(e -> sortedMap.put(e.getKey(), e.getValue()));

        List<String> results = new ArrayList();

        // print map
        sortedMap.forEach((k, v) -> {
            results.add(String.format("%35s (UTC%s) %n", k, v).trim());
        });

        return results;
    }

    private Map<String, String> getAllZoneIdsAndItsOffSet() {

        Map<String, String> result = new HashMap<>();

        LocalDateTime localDateTime = LocalDateTime.now();

        for (String zoneId : ZoneId.getAvailableZoneIds()) {

            ZoneId id = ZoneId.of(zoneId);

            // LocalDateTime -> ZonedDateTime
            ZonedDateTime zonedDateTime = localDateTime.atZone(id);

            // ZonedDateTime -> ZoneOffset
            ZoneOffset zoneOffset = zonedDateTime.getOffset();

            //replace Z to +00:00
            String offset = zoneOffset.getId().replaceAll("Z", "+00:00");

            result.put(id.toString(), offset);

        }

        return result;
    }

    public void save() {

    }
}
