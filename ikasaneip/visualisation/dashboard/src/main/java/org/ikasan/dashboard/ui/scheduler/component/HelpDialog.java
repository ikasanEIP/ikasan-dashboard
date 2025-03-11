package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class HelpDialog extends ConfirmDialog
{
    public HelpDialog(String helpText)
    {
        super.setHeader(getTranslation("help.help-header", UI.getCurrent().getLocale()));
        super.setConfirmText(getTranslation("button.ok", UI.getCurrent().getLocale()));
        init(helpText);
    }

    private void init(String helpText)
    {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setMargin(false);
        verticalLayout.setSpacing(false);

        super.setText(helpText);
    }
}
