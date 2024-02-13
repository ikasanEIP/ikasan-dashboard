package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.job.orchestration.context.validation.ContextError;

import java.util.List;

public class JobPlanWarningsDialog extends AbstractCloseableResizableDialog {

    private List<ContextError> contextWarnings;

    public JobPlanWarningsDialog(List<ContextError> contextWarnings) {
        this.contextWarnings = contextWarnings;
        this.setWidth("70vw");
        this.setHeight("70vh");
        this.setResizable(false);
        this.add(this.initialiseErrorWidget());
        super.title.setText(getTranslation("dialog-title.job-plan-warnings", UI.getCurrent().getLocale()));
    }

    private VerticalLayout initialiseErrorWidget() {
        VerticalLayout errorWidget = new VerticalLayout();
        errorWidget.setSizeFull();
        errorWidget.setMargin(true);
        errorWidget.setPadding(false);

        Grid<ContextError> errorGrid = new Grid<>();
        errorGrid.addColumn(ContextError::getErrorMessage)
            .setHeader(getTranslation("label.warning-message", UI.getCurrent().getLocale()))
            .setKey("warningMessage")
            .setFlexGrow(20);

        errorGrid.setItems(this.contextWarnings);
        errorWidget.add(errorGrid);

        return errorWidget;
    }
}
