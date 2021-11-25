package org.ikasan.dashboard.ui.general.component;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.Optional;

public class ErrorDialog extends AbstractEntityViewDialog<IkasanSolrDocument>
{
    private TextField moduleNameTf;
    private TextField componentNameTf;
    private TextField flowNameTf;
    private TextField eventIdTf;
    private TextField errorActionTf;
    private TextField dateTimeTf;
    private TextField errorUriTf;
    private TextField errorClassTf;

    private StreamResource streamResource;
    private FileDownloadWrapper buttonWrapper;
    private Tooltip downloadButtonTooltip;

    private String errorEvent;
    private String errorDetails;

    private DateFormatter dateFormatter;

    private IkasanSolrDocument ikasanSolrDocument;

    public ErrorDialog(DateFormatter dateFormatter)
    {
        moduleNameTf = new TextField(getTranslation("text-field.module-name", UI.getCurrent().getLocale(), null));
        flowNameTf = new TextField(getTranslation("text-field.flow-name", UI.getCurrent().getLocale(), null));
        componentNameTf = new TextField(getTranslation("text-field.component-name", UI.getCurrent().getLocale(), null));
        eventIdTf = new TextField(getTranslation("text-field.event-id", UI.getCurrent().getLocale(), null));
        errorUriTf = new TextField(getTranslation("text-field.error-uri", UI.getCurrent().getLocale(), null));
        errorActionTf = new TextField(getTranslation("text-field.error-action", UI.getCurrent().getLocale(), null));
        dateTimeTf = new TextField(getTranslation("text-field.date-time", UI.getCurrent().getLocale(), null));
        errorClassTf = new TextField(getTranslation("text-field.exception-class", UI.getCurrent().getLocale(), null));
        this.dateFormatter = dateFormatter;
    }

    @Override
    public Component getEntityDetailsLayout()
    {
        Image errorImage = new Image("/frontend/images/error-service.png", "");
        errorImage.setHeight("70px");

        H3 errorLabel = new H3(getTranslation("label.error-event-details", UI.getCurrent().getLocale(), null));

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.add(errorImage, errorLabel);

        FormLayout formLayout = new FormLayout();

        moduleNameTf.setReadOnly(true);
        formLayout.add(moduleNameTf);

        componentNameTf.setReadOnly(true);
        formLayout.add(componentNameTf);

        flowNameTf.setReadOnly(true);
        formLayout.add(flowNameTf);

        eventIdTf.setReadOnly(true);
        formLayout.add(eventIdTf);

        dateTimeTf.setReadOnly(true);
        formLayout.add(dateTimeTf);

        errorUriTf.setReadOnly(true);
        formLayout.add(errorUriTf);

        errorActionTf.setReadOnly(true);
        formLayout.add(errorActionTf);

        errorClassTf.setReadOnly(true);
        formLayout.add(errorClassTf);

        formLayout.setSizeFull();

        Button downloadButton = new TableButton(VaadinIcon.DOWNLOAD.create());
        downloadButtonTooltip = TooltipHelper.getTooltipForComponentTopLeft(downloadButton
            , getTranslation("tooltip.download-error-event", UI.getCurrent().getLocale()));

        this.streamResource = new StreamResource("error.txt"
            , () -> new ByteArrayInputStream(super.aceEditor.getValue().getBytes() ));

        buttonWrapper = new FileDownloadWrapper(this.streamResource);
        buttonWrapper.wrapComponent(downloadButton);


        Tab errorTab = new Tab(getTranslation("tab-label.error", UI.getCurrent().getLocale()));
        Tab errorEventTab = new Tab(getTranslation("tab-label.error-event", UI.getCurrent().getLocale()));
        Tabs tabs = new Tabs(errorTab, errorEventTab);

        tabs.addSelectedChangeListener(event ->
        {
            if(tabs.getSelectedTab().equals(errorTab))
            {
                super.aceEditor.setValue(Optional.ofNullable(errorDetails)
                    .orElse(getTranslation("placeholder.not-content", UI.getCurrent().getLocale())));
            }
            else
            {
                String content;
                if(super.select.getValue() != null && super.select.getValue().equals("XML")){
                    content = super.formatXml(errorEvent);
                }
                else if(super.select.getValue() != null && super.select.getValue().equals("JSON")){
                    content = super.formatJson(errorEvent);
                }
                else {
                    content = errorEvent;
                }
                super.aceEditor.setValue(Optional.ofNullable(content)
                    .orElse(getTranslation("placeholder.not-content", UI.getCurrent().getLocale())));
            }
        });

        Button newWindowButton = new TableButton(VaadinIcon.EXTERNAL_LINK.create());
        newWindowButton.addClickListener(buttonClickEvent -> {
            EntityContentsViewDialog entityContentsViewDialog = new EntityContentsViewDialog("Error " + ikasanSolrDocument.getErrorUri());
            if(tabs.getSelectedTab().equals(errorTab)) {
                entityContentsViewDialog.open(this.errorDetails);
            }
            else {
                entityContentsViewDialog.open(this.errorEvent);
            }
        });
        HorizontalLayout iconLayout = new HorizontalLayout();
        iconLayout.add(super.select, buttonWrapper, downloadButtonTooltip, newWindowButton);
        iconLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, super.select);
        iconLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, buttonWrapper, newWindowButton);

        VerticalLayout layout = new VerticalLayout();
        layout.add(headerLayout, formLayout, iconLayout);

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, iconLayout);

        layout.add(tabs);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, tabs);

        return layout;
    }

    @Override
    public void populate(IkasanSolrDocument errorEvent)
    {
        this.ikasanSolrDocument = errorEvent;
        super.title.setText("Error " + errorEvent.getErrorUri());
        this.moduleNameTf.setValue(Optional.ofNullable(errorEvent.getModuleName()).orElse(""));
        this.flowNameTf.setValue(Optional.ofNullable(errorEvent.getFlowName()).orElse(""));
        this.componentNameTf.setValue(Optional.ofNullable(errorEvent.getComponentName()).orElse(""));
        this.eventIdTf.setValue(Optional.ofNullable(errorEvent.getEventId()).orElse(""));
        this.errorUriTf.setValue(Optional.ofNullable(errorEvent.getErrorUri()).orElse(""));
        this.errorActionTf.setValue(Optional.ofNullable(errorEvent.getErrorAction()).orElse(""));
        this.dateTimeTf.setValue(this.dateFormatter.getFormattedDate(errorEvent.getTimestamp()));
        this.errorClassTf.setValue(Optional.ofNullable(errorEvent.getExceptionClass()).orElse(""));

        this.errorEvent = errorEvent.getEvent();
        this.errorDetails = errorEvent.getErrorDetail();

        super.open(errorEvent.getErrorDetail());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        this.downloadButtonTooltip.attachToComponent(buttonWrapper);
    }
}
