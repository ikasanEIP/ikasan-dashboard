package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class DayOfWeekJobHelpDialog extends AbstractCloseableResizableDialog
{

    public DayOfWeekJobHelpDialog()
    {
        super.title.setText(getTranslation("help.search-help-header", UI.getCurrent().getLocale()));

        init();
    }

    private void init()
    {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setMargin(false);
        verticalLayout.setSpacing(false);

        verticalLayout
            .add(
                this.buildHelpComponent(getTranslation("help.day-of-week-job-help", UI.getCurrent().getLocale()))
            );

        super.content.add(verticalLayout);
        this.setWidth("700px");
        this.setHeight("250px");
    }

    private Component buildHelpComponent(String helpText) {
        Icon helpImage = VaadinIcon.QUESTION.create();
        helpImage.setSize("40px");
        helpImage.getStyle().set("color", "rgba(241, 90, 35, 1.0)");

        Div helpTextParagraph = new Div();
        helpTextParagraph.setText(helpText);

        HorizontalLayout helpLayout = new HorizontalLayout();
        helpLayout.setWidthFull();
        helpLayout.setMargin(true);
        helpLayout.setSpacing(true);
        helpLayout.add(helpImage, helpTextParagraph);

        return helpLayout;
    }
}
