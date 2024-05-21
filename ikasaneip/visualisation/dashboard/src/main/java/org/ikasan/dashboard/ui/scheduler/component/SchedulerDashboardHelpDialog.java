package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class SchedulerDashboardHelpDialog extends AbstractCloseableResizableDialog
{

    public SchedulerDashboardHelpDialog()
    {
        super.title.setText(getTranslation("help.search-help-header", UI.getCurrent().getLocale()));

        init();
    }

    private void init()
    {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setMargin(false);
        verticalLayout.setSpacing(false);


//        ListItem specialCharactersListItem = new ListItem(getTranslation("help.search-solr-special-characters", UI.getCurrent().getLocale()));
//        ListItem whiteSpaceListItem = new ListItem(getTranslation("help.search-whitespace", UI.getCurrent().getLocale()));
//        ListItem wildcardListItem = new ListItem(getTranslation("help.search-wildcard", UI.getCurrent().getLocale()));
//        ListItem logicListItem = new ListItem(getTranslation("help.search-logic", UI.getCurrent().getLocale()));
//
//        UnorderedList searchTermList = new UnorderedList(specialCharactersListItem, whiteSpaceListItem, wildcardListItem, logicListItem);

        verticalLayout
            .add(
                this.buildHelpComponent("frontend/images/job-status-counts.png"
                    , getTranslation("header.active-job-plan-instances-dashboard", UI.getCurrent().getLocale())
                    , getTranslation("help.active-job-plan-instances-dashboard-1", UI.getCurrent().getLocale())));
//                searchTermList,
//                this.buildHelpComponent("frontend/images/wiretap-service.png", getTranslation("help.wiretap-header", UI.getCurrent().getLocale()), getTranslation("help.search-wiretap", UI.getCurrent().getLocale())),
//                this.buildHelpComponent("frontend/images/replay-service.png", getTranslation("help.replay-header", UI.getCurrent().getLocale()), getTranslation("help.search-replay", UI.getCurrent().getLocale())),
//                this.buildHelpComponent("frontend/images/hospital-service.png", getTranslation("help.hospital-header", UI.getCurrent().getLocale()), getTranslation("help.search-hospital", UI.getCurrent().getLocale())),
//                this.buildHelpComponent("frontend/images/error-service.png", getTranslation("help.error-header", UI.getCurrent().getLocale()), getTranslation("help.search-error", UI.getCurrent().getLocale())));

        super.content.add(verticalLayout);
        this.setWidth("85%");
        this.setHeight("85%");
    }

    private Component buildHelpComponent(String image, String header, String helpText) {
        Image helpImage = new Image(image, "");
        helpImage.setWidth("100%");

        Image jobStatusFilter = new Image("frontend/images/job-status-filter.png", "");
        jobStatusFilter.setWidth("180px");

        Image jobStatusBreakout = new Image("frontend/images/job-status-breakout.png", "");
        jobStatusBreakout.setWidth("220px");

        Image repeatingJobs = new Image("frontend/images/repeating-jobs.png", "");
        repeatingJobs.setWidth("400px");

        Div helpTextParagraph = new Div();
        helpTextParagraph.setText(helpText);

        Div helpText2Paragraph = new Div();
        helpText2Paragraph.setText(getTranslation("help.active-job-plan-instances-dashboard-2", UI.getCurrent().getLocale()));

        Div helpText3Paragraph = new Div();
        helpText3Paragraph.setText(getTranslation("help.active-job-plan-instances-dashboard-3", UI.getCurrent().getLocale()));

        Div helpText4Paragraph = new Div();
        helpText4Paragraph.setText(getTranslation("help.active-job-plan-instances-dashboard-4", UI.getCurrent().getLocale()));

        VerticalLayout helpLayout = new VerticalLayout();
        helpLayout.setWidthFull();
        helpLayout.setMargin(false);
        helpLayout.setSpacing(true);
        Label headerLabel = new Label(header);
        headerLabel.getElement().getStyle().set("font-size", "20pt");
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.add(helpImage);
        headerLayout.setMargin(false);
        headerLayout.setSpacing(true);
        headerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, helpImage);
//        headerLabel.setWidth("50%");
        helpLayout.add(headerLabel);
        helpLayout.add(helpTextParagraph);
        helpLayout.add(headerLayout);
        helpLayout.add(helpText2Paragraph, jobStatusFilter, helpText3Paragraph, jobStatusBreakout, helpText4Paragraph, repeatingJobs);
        helpLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, headerLayout, helpTextParagraph);

        return helpLayout;
    }
}
