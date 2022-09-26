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
    private Long lockCount;
    public NewJobLockDialog(Map<String, JobLock> existingLocks) {
        this.existingLocks = existingLocks;
        this.init();
    }

    private void init() {

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        TextField lockNameTf = new TextField("Lock Name");
        lockNameTf.setRequired(true);

        TextField lockCountTf = new TextField("Lock Count");
        lockCountTf.setRequired(true);

        FormLayout formLayout = new FormLayout();
        formLayout.add(lockNameTf, lockCountTf);

        layout.add(formLayout);

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.addClickListener(event -> {
            boolean error = false;
            if(lockNameTf.getValue() == null || lockNameTf.getValue().isEmpty()) {
                lockNameTf.setInvalid(true);
                lockNameTf.setErrorMessage("A lock name is required!");
                error = true;
            }
            else if(this.existingLocks.containsKey(lockNameTf.getValue())) {
                lockNameTf.setInvalid(true);
                lockNameTf.setErrorMessage("A lock with that name already exists! Lock names must be unique.");
                error = true;
            }

            if(lockCountTf.getValue() == null || lockCountTf.getValue().isEmpty()) {
                lockCountTf.setInvalid(true);
                lockCountTf.setErrorMessage("A lock count is required!");
                error = true;
            }
            else {
                try {
                    Long.parseLong(lockCountTf.getValue());
                }
                catch (NumberFormatException e) {
                    lockCountTf.setInvalid(true);
                    lockCountTf.setErrorMessage("The lock count must be a number!");
                    error = true;
                }
            }

            if(error)return;

            this.lockName = lockNameTf.getValue();
            this.lockCount = Long.parseLong(lockCountTf.getValue());

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
        super.title.setText(getTranslation("table-header.context-name", UI.getCurrent().getLocale()));

        super.showResize(false);
        super.setResizable(false);

        super.setHeight("230px");
        super.setWidth("1400px");
    }

    public String getLockName() {
        return lockName;
    }

    public Long getLockCount() {
        return lockCount;
    }
}
