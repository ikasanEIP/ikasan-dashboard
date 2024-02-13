package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import liquibase.pro.packaged.L;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.model.JsonValidationError;
import org.ikasan.job.orchestration.context.validation.ContextError;

import java.util.List;

public class JobPlanErrorsDialog extends AbstractCloseableResizableDialog {

    private List<ContextError> contextErrors;

    public JobPlanErrorsDialog(List<ContextError> contextErrors) {
        this.contextErrors = contextErrors;
        this.setWidth("70vw");
        this.setHeight("70vh");
        this.setResizable(false);
        this.add(this.initialiseErrorWidget());
        super.title.setText(getTranslation("dialog-title.job-plan-errors", UI.getCurrent().getLocale()));
    }

    private VerticalLayout initialiseErrorWidget() {
        VerticalLayout errorWidget = new VerticalLayout();
        errorWidget.setSizeFull();
        errorWidget.setMargin(true);
        errorWidget.setPadding(false);

        Grid<ContextError> errorGrid = new Grid<>();
        errorGrid.addColumn(ContextError::getErrorMessage)
            .setHeader(getTranslation("label.error-message", UI.getCurrent().getLocale()))
            .setKey("errorMessage")
            .setFlexGrow(20);

        errorGrid.setItems(this.contextErrors);
        errorWidget.add(errorGrid);

        return errorWidget;
    }
}
