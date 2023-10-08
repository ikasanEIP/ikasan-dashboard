package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

public class FileWatcherHelpDialog extends AbstractCloseableResizableDialog
{

    public FileWatcherHelpDialog()
    {
        super.title.setText(getTranslation("help.file-watcher-job-help-header", UI.getCurrent().getLocale()));

        init();
    }

    private void init()
    {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setMargin(false);
        verticalLayout.setSpacing(false);


        ListItem unixExample1ListItem = new ListItem(getTranslation("help.file-watcher-job-help-unix-example-1", UI.getCurrent().getLocale()));
        ListItem unixExample2ListItem = new ListItem(getTranslation("help.file-watcher-job-help-unix-example-2", UI.getCurrent().getLocale()));
        ListItem windowsExample1ListItem = new ListItem(getTranslation("help.file-watcher-job-help-windows-example-1", UI.getCurrent().getLocale()));
        ListItem windowsExample2ListItem = new ListItem(getTranslation("help.file-watcher-job-help-windows-example-2", UI.getCurrent().getLocale()));
        ListItem windowsExample3ListItem = new ListItem(getTranslation("help.file-watcher-job-help-windows-example-3", UI.getCurrent().getLocale()));
        Div archiveDirectory = new Div();
        archiveDirectory.setText(getTranslation("help.archive-directory-help", UI.getCurrent().getLocale()));

        UnorderedList fileWatchJobConfigList = new UnorderedList(unixExample1ListItem, unixExample2ListItem,
            windowsExample1ListItem, windowsExample2ListItem, windowsExample3ListItem);

        verticalLayout
            .add(
                this.buildHelpComponent("frontend/images/file_black.png", getTranslation("help.file-watcher-job-help-header"
                        , UI.getCurrent().getLocale()), getTranslation("help.file-watcher-job-help-text", UI.getCurrent().getLocale())),
                fileWatchJobConfigList, archiveDirectory);

        super.content.add(verticalLayout);
        this.setWidth("1200px");
        this.setHeight("450px");
    }

    private Component buildHelpComponent(String image, String header, String helpText) {
        Image helpImage = new Image(image, "");
        helpImage.setHeight("40px");

        Div helpTextParagraph = new Div();
        helpTextParagraph.setText(helpText);

        VerticalLayout helpLayout = new VerticalLayout();
        helpLayout.setWidthFull();
        helpLayout.setMargin(false);
        helpLayout.setSpacing(false);
        H4 headerLabel = new H4(header);
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.add(helpImage, headerLabel);
        headerLayout.setMargin(false);
        headerLayout.setSpacing(true);
        headerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, helpImage, headerLabel);
        helpLayout.add(headerLayout);
        helpLayout.add(helpTextParagraph);
        helpLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, headerLayout, helpTextParagraph);

        return helpLayout;
    }
}
