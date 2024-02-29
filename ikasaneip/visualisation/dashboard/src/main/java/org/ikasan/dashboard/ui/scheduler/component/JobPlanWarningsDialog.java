package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.LitRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.job.orchestration.context.validation.ContextError;

import java.util.List;

public class JobPlanWarningsDialog extends AbstractCloseableResizableDialog {

    private List<ContextError> contextWarnings;

    public JobPlanWarningsDialog(List<ContextError> contextWarnings) {
        this.contextWarnings = contextWarnings;
        this.setWidth("70vw");
        this.setHeight("70vh");
        super.content.add(this.initialiseErrorWidget());
        super.title.setText(getTranslation("dialog-title.job-plan-warnings", UI.getCurrent().getLocale()));
    }

    private Component initialiseErrorWidget() {

        Grid<ContextError> errorGrid = new Grid<>();
        errorGrid.addColumn(LitRenderer.<ContextError>of(
                "<div style=\"word-wrap:normal; white-space:normal\">[[item.error]]</div>")
            .withProperty("error", contextError -> contextError.getErrorMessage()))
            .setHeader(getTranslation("label.warning-message", UI.getCurrent().getLocale()))
            .setKey("warningMessage");

        errorGrid.setWidth("100%");

        errorGrid.setItems(this.contextWarnings);

        return errorGrid;
    }
}
