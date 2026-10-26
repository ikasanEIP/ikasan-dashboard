package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.IkasanColours;

/**
 * Help dialog that describes the various job actions available in the scheduler.
 * Provides users with comprehensive information about all job operations including:
 * skip, enable, hold, release, disable, reinstate, submit, reset, kill, acknowledge error,
 * and various viewing/monitoring actions along with their corresponding icons.
 */
public class JobActionsHelpDialog extends AbstractCloseableResizableDialog {

    /**
     * Constructor that initializes the help dialog with job action descriptions.
     */
    public JobActionsHelpDialog() {
        super();
        this.title.setText(getTranslation("header.job-actions-help", UI.getCurrent().getLocale()));
        this.setWidth("85hw");
        this.setHeight("auto");
        this.showResize(false);

        init();
    }

    /**
     * Initializes the dialog content with all job action descriptions.
     */
    private void init() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        // Introduction
        Div intro = new Div();
        intro.setText(getTranslation("help.job-actions-intro", UI.getCurrent().getLocale()));
        intro.getStyle()
            .set("margin-bottom", "20px")
            .set("font-size", "14px")
            .set("color", "#666");
        layout.add(intro);

        // Add each action description
        layout.add(createActionSection(
            VaadinIcon.FAST_FORWARD,
            getTranslation("help.job-action.skip.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.skip.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.PLAY,
            getTranslation("help.job-action.enable.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.enable.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.HAND,
            getTranslation("help.job-action.hold.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.hold.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.HANDS_UP,
            getTranslation("help.job-action.release.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.release.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.BAN,
            getTranslation("help.job-action.disable.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.disable.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.CHECK,
            getTranslation("help.job-action.reinstate.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.reinstate.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.ARROW_BACKWARD,
            getTranslation("help.job-action.reset.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.reset.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.PAPERPLANE,
            getTranslation("help.job-action.submit.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.submit.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.CLOSE_BIG,
            getTranslation("help.job-action.kill.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.kill.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.THUMBS_UP,
            getTranslation("help.job-action.acknowledge.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.acknowledge.description", UI.getCurrent().getLocale())
        ));

        // Add a divider for viewing/monitoring actions
        Div viewingHeader = new Div();
        viewingHeader.setText(getTranslation("help.job-actions-viewing-header", UI.getCurrent().getLocale()));
        viewingHeader.getStyle()
            .set("margin-top", "24px")
            .set("margin-bottom", "12px")
            .set("font-size", "18px")
            .set("font-weight", "600")
            .set("color", "#232F34")
            .set("border-bottom", "2px solid " + IkasanColours.IKASAN_ORANGE)
            .set("padding-bottom", "8px");
        layout.add(viewingHeader);

        layout.add(createActionSection(
            VaadinIcon.SITEMAP,
            getTranslation("help.job-action.visualisation.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.visualisation.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.FILE_PROCESS,
            getTranslation("help.job-action.view-log.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.view-log.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.FILE_REMOVE,
            getTranslation("help.job-action.view-error-log.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.view-error-log.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.CLIPBOARD_HEART,
            getTranslation("help.job-action.execution-history.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.execution-history.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.CALENDAR_CLOCK,
            getTranslation("help.job-action.view-event.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.view-event.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.COG,
            getTranslation("help.job-action.execution-details.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.execution-details.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.CHART,
            getTranslation("help.job-action.statistics.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-action.statistics.description", UI.getCurrent().getLocale())
        ));

        // Add a divider for status indicators
        Div statusHeader = new Div();
        statusHeader.setText(getTranslation("help.job-status-indicators-header", UI.getCurrent().getLocale()));
        statusHeader.getStyle()
            .set("margin-top", "24px")
            .set("margin-bottom", "12px")
            .set("font-size", "18px")
            .set("font-weight", "600")
            .set("color", "#232F34")
            .set("border-bottom", "2px solid " + IkasanColours.IKASAN_ORANGE)
            .set("padding-bottom", "8px");
        layout.add(statusHeader);

        layout.add(createActionSection(
            VaadinIcon.LOCK,
            getTranslation("help.job-indicator.lock.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-indicator.lock.description", UI.getCurrent().getLocale())
        ));

        layout.add(createActionSection(
            VaadinIcon.BULLSEYE,
            getTranslation("help.job-indicator.targeted.title", UI.getCurrent().getLocale()),
            getTranslation("help.job-indicator.targeted.description", UI.getCurrent().getLocale())
        ));

        content.add(layout);
    }

    /**
     * Creates a section for a single job action with icon, title, and description.
     *
     * @param vaadinIcon The VaadinIcon to display for this action
     * @param title The title of the action
     * @param description The description of what the action does
     * @return A HorizontalLayout containing the formatted action section
     */
    private HorizontalLayout createActionSection(VaadinIcon vaadinIcon, String title, String description) {
        HorizontalLayout section = new HorizontalLayout();
        section.setWidthFull();
        section.setAlignItems(FlexComponent.Alignment.START);
        section.setSpacing(true);
        section.getStyle()
            .set("padding", "12px")
            .set("margin-bottom", "8px")
            .set("border-radius", "4px")
            .set("background-color", "#f8f9fa");

        // Icon
        Icon icon = vaadinIcon.create();
        icon.setSize("24px");
        icon.getStyle()
            .set("color", IkasanColours.IKASAN_ORANGE)
            .set("margin-right", "12px")
            .set("flex-shrink", "0");

        // Content
        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        H3 titleElement = new H3(title);
        titleElement.getStyle()
            .set("margin", "0")
            .set("font-size", "16px")
            .set("font-weight", "600")
            .set("color", "#232F34");

        Span descriptionElement = new Span(description);
        descriptionElement.getStyle()
            .set("font-size", "14px")
            .set("color", "#666")
            .set("line-height", "1.5");

        textLayout.add(titleElement, descriptionElement);

        section.add(icon, textLayout);
        section.setFlexGrow(1, textLayout);

        return section;
    }
}
