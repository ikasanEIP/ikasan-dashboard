package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import liquibase.pro.packaged.L;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.model.JsonValidationError;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.job.orchestration.context.validation.ContextError;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;

import java.util.List;

public class JobPlanErrorsDialog extends AbstractCloseableResizableDialog {

    private List<ContextError> contextErrors;

    public JobPlanErrorsDialog(List<ContextError> contextErrors) {
        this.contextErrors = contextErrors;
        this.setWidth("70vw");
        this.setHeight("70vh");
        super.content.add(this.initialiseErrorWidget());
        super.title.setText(getTranslation("dialog-title.job-plan-errors", UI.getCurrent().getLocale()));
    }

    private Component initialiseErrorWidget() {
        Grid<ContextError> errorGrid = new Grid<>();
        errorGrid.addColumn(TemplateRenderer.<ContextError>of(
                "<div style=\"word-wrap:normal; white-space:normal\">[[item.error]]</div>")
            .withProperty("error", contextError -> contextError.getErrorMessage()))
            .setHeader(getTranslation("label.error-message", UI.getCurrent().getLocale()))
            .setKey("errorMessage");

        errorGrid.setWidth("100%");

        errorGrid.setItems(this.contextErrors);;

        return errorGrid;
    }
}
