package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class NewJobLockDialog extends AbstractCloseableResizableDialog {

    private Map<String, JobLock> existingLocks;
    private String lockName;
    private Integer lockCount;

    public NewJobLockDialog(Map<String, JobLock> existingLocks) {
        this.existingLocks = existingLocks;
        this.init();
    }

    private void init() {

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        TextField lockNameTf = new TextField(getTranslation("label.lock-name", UI.getCurrent().getLocale()));
        lockNameTf.getElement().getThemeList().add("always-float-label");
        lockNameTf.setRequired(true);

        IntegerField lockCountTf = new IntegerField(getTranslation("label.lock-count", UI.getCurrent().getLocale()));
        lockCountTf.getElement().getThemeList().add("always-float-label");
        lockCountTf.setHasControls(true);
        lockCountTf.setMin(1);
        lockCountTf.setMax(Integer.MAX_VALUE);

        FormLayout formLayout = new FormLayout();
        formLayout.add(lockNameTf, lockCountTf);

        layout.add(formLayout);

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.addClickListener(event -> {
            boolean error = false;
            if(lockNameTf.getValue() == null || lockNameTf.getValue().isEmpty()) {
                lockNameTf.setInvalid(true);
                lockNameTf.setErrorMessage(getTranslation("error.lock-name-required", UI.getCurrent().getLocale()));
                error = true;
            }
            else if(this.existingLocks.containsKey(lockNameTf.getValue())) {
                lockNameTf.setInvalid(true);
                lockNameTf.setErrorMessage(getTranslation("error.lock-name-exists", UI.getCurrent().getLocale()));
                error = true;
            }

            if(lockCountTf.getValue() == null || lockCountTf.getValue() < 1 || lockCountTf.getValue() > Integer.MAX_VALUE) {
                lockCountTf.setInvalid(true);
                lockCountTf.setErrorMessage(String.format(getTranslation("error.job-lock-size"
                    , UI.getCurrent().getLocale()), Integer.MAX_VALUE));
                error = true;
            }

            if(error)return;

            this.lockName = lockNameTf.getValue();
            this.lockCount = lockCountTf.getValue();

            this.close();
        });
        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(event -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(okButton, cancelButton);
        buttonLayout.getElement().getStyle().set("position", "absolute");
        buttonLayout.getElement().getStyle().set("bottom", "20px");

        layout.add(buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

        super.content.add(layout);
        super.title.setText(getTranslation("label.new-job-lock", UI.getCurrent().getLocale()));

        super.showResize(false);
        super.setResizable(false);

        super.setHeight("230px");
        super.setWidth("1400px");
    }

    public String getLockName() {
        return lockName;
    }

    public Integer getLockCount() {
        return lockCount;
    }
}
