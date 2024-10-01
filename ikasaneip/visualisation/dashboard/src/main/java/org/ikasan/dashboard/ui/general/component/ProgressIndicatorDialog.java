package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.lumo.LumoUtility;


public class ProgressIndicatorDialog extends Dialog
{
    private boolean showCancelButton;
    private boolean isCancelled = false;

    public ProgressIndicatorDialog(boolean showCancelButton)
    {
        this.showCancelButton = showCancelButton;
    }

    public void open(String label, String text)
    {
        this.setCloseOnEsc(false);
        this.setCloseOnOutsideClick(false);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        NativeLabel progressBarLabel = new NativeLabel(label + "...");
        progressBarLabel.setId("pblbl");
        progressBarLabel.addClassName(LumoUtility.TextColor.SECONDARY);

        Span progressBarSubLabel = new Span(text);
        progressBarSubLabel.setId("sublbl");
        progressBarSubLabel.addClassNames(LumoUtility.TextColor.SECONDARY,
            LumoUtility.FontSize.XSMALL);


        progressBar.getElement().setAttribute("aria-labelledby", "pblbl");
        progressBar.getElement().setAttribute("aria-describedby", "sublbl");

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.isCancelled = true;
            this.close();
        });

        VerticalLayout layout = new VerticalLayout();
        layout.add(progressBarLabel, progressBar, progressBarSubLabel, cancelButton);

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, cancelButton);

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, progressBar);
        layout.setSizeFull();

        this.add(layout);

        cancelButton.setVisible(this.showCancelButton);

        this.open();
    }

    public boolean isCancelled()
    {
        return isCancelled;
    }

    public void cancel() {
        this.isCancelled = true;
    }
}
