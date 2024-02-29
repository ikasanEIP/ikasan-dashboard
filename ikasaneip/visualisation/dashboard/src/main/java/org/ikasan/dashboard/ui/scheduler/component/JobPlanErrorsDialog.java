package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.LitRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.job.orchestration.context.validation.ContextError;

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
        errorGrid.addColumn(LitRenderer.<ContextError>of(
                "<div style=\"word-wrap:normal; white-space:normal\">[[item.error]]</div>")
            .withProperty("error", contextError -> contextError.getErrorMessage()))
            .setHeader(getTranslation("label.error-message", UI.getCurrent().getLocale()))
            .setKey("errorMessage");

        errorGrid.setWidth("100%");

        errorGrid.setItems(this.contextErrors);;

        return errorGrid;
    }
}
