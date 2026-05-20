package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.ArrayList;
import java.util.List;

public class ResidingContextSelectDialog extends AbstractCloseableResizableDialog {
    private List<Checkbox> checkboxes = new ArrayList<>();
    private ArrayList<String> selectedContexts = null;

    public ResidingContextSelectDialog(InternalEventDrivenJob internalEventDrivenJob, Action action) {
        this.init(internalEventDrivenJob, action);
    }

    private void init(InternalEventDrivenJob internalEventDrivenJob, Action action) {

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        NativeLabel label = new NativeLabel();
        if(action.equals(Action.HOLD)) {
            label.setText(getTranslation("label.select-held-contexts", UI.getCurrent().getLocale()));
        }
        else if(action.equals(Action.SKIP)) {
            label.setText(getTranslation("label.select-skipped-contexts", UI.getCurrent().getLocale()));
        }
        else if(action.equals(Action.SELECT_FOR_LOCK)) {
            label.setText("Select residing contexts to add to job lock");
        }

        Button selectAll = new Button(getTranslation("button.select-all", UI.getCurrent().getLocale()));
        Button deSelectAll = new Button(getTranslation("button.deselect-all", UI.getCurrent().getLocale()));
        deSelectAll.setVisible(false);
        selectAll.getElement().getStyle().set("position", "absolute");
        selectAll.getElement().getStyle().set("right", "30px");
        selectAll.addClickListener(event -> {
            checkboxes.forEach(cb -> cb.setValue(true));
            deSelectAll.setVisible(true);
            selectAll.setVisible(false);
        });

        deSelectAll.getElement().getStyle().set("position", "absolute");
        deSelectAll.getElement().getStyle().set("right", "30px");
        deSelectAll.addClickListener(event -> checkboxes.forEach(cb -> {
            cb.setValue(false);
            deSelectAll.setVisible(false);
            selectAll.setVisible(true);
        }));

        HorizontalLayout labelLayout = new HorizontalLayout();
        labelLayout.add(label, selectAll, deSelectAll);

        layout.add(labelLayout);

        Grid<ResidingContext> grid = new Grid<>();
        grid.addColumn(ResidingContext::getContextName)
            .setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale()))
            .setFlexGrow(8);
        grid.addColumn(new ComponentRenderer<>(
            residingContext -> {
                Checkbox checkbox = new Checkbox();
                checkbox.setValue(residingContext.isSelected());
                checkbox.addValueChangeListener(event -> residingContext.setSelected(event.getValue()));
                checkboxes.add(checkbox);
                return checkbox;
            }
        ))
            .setHeader(getTranslation("table-header.select", UI.getCurrent().getLocale()))
            .setFlexGrow(1);

        grid.setHeight("400px");
        grid.setWidthFull();

        grid.setItems(this.initialiseGridData(internalEventDrivenJob));

        layout.add(grid);

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.addClickListener(event -> {
            ((ListDataProvider<ResidingContext>)grid.getDataProvider()).getItems().forEach(residingContext -> {
                if(residingContext.isSelected()) {
                    if(this.selectedContexts == null) this.selectedContexts = new ArrayList<>();
                    this.selectedContexts.add(residingContext.getContextName());
                }
            });
            this.close();
        });
        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(event -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(okButton, cancelButton);

        layout.add(buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

        super.content.add(layout);
        super.title.setText(getTranslation("table-header.context-name", UI.getCurrent().getLocale()));

        super.showResize(false);
        super.setResizable(false);

        super.setHeight("500px");
        super.setWidth("1000px");
    }

    public ArrayList<String> getSelectedContexts() {
        return selectedContexts;
    }

    private List<ResidingContext> initialiseGridData(InternalEventDrivenJob internalEventDrivenJob) {
        List<ResidingContext> residingContexts = new ArrayList<>();

        internalEventDrivenJob.getChildContextNames().forEach(child -> {
            residingContexts.add(new ResidingContext(child, false));
        });

        return residingContexts;
    }

    private class ResidingContext {

        public ResidingContext(String contextName, boolean selected) {
            this.contextName = contextName;
            this.selected = selected;
        }

        private String contextName;
        private boolean selected;

        public String getContextName() {
            return contextName;
        }

        public void setContextName(String contextName) {
            this.contextName = contextName;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    public enum Action {
        HOLD,
        SKIP,
        SELECT_FOR_LOCK;
    }
}
