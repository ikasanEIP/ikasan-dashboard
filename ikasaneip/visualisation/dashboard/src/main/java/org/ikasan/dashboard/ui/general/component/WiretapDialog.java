package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.spec.search.model.IkasanESBDocument;

import java.io.ByteArrayInputStream;

public class WiretapDialog extends AbstractEntityViewDialog<IkasanESBDocument>
{
    private TextField moduleNameTf;
    private TextField componentNameTf;
    private TextField flowNameTf;
    private TextField eventIdTf;
    private TextField dateTimeTf;

    private Button downloadButton;

    private DateFormatter dateFormatter;

    private IkasanESBDocument wiretapEvent;

    public WiretapDialog(DateFormatter dateFormatter)
    {
        moduleNameTf = new TextField(getTranslation("text-field.module-name", UI.getCurrent().getLocale(), null));
        flowNameTf = new TextField(getTranslation("text-field.flow-name", UI.getCurrent().getLocale(), null));
        componentNameTf = new TextField(getTranslation("text-field.component-name", UI.getCurrent().getLocale(), null));
        eventIdTf = new TextField(getTranslation("text-field.event-id", UI.getCurrent().getLocale(), null));
        dateTimeTf = new TextField(getTranslation("text-field.date-time", UI.getCurrent().getLocale(), null));

        this.dateFormatter = dateFormatter;
    }

    @Override
    public Component getEntityDetailsLayout()
    {
        Image wiretapImage = new Image("/frontend/images/wiretap-service.png", "");
        wiretapImage.setHeight("70px");

        H3 wiretapLabel = new H3(getTranslation("label.wiretap-event-details", UI.getCurrent().getLocale(), null));

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.add(wiretapImage, wiretapLabel);

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

        formLayout.setSizeFull();

        downloadButton = new TableButton(VaadinIcon.DOWNLOAD.create());
        downloadButton.getElement().setAttribute("title",
            getTranslation("tooltip.download-wiretap-event", UI.getCurrent().getLocale()));

        Anchor downloadAnchor = new Anchor(DownloadHandler.fromInputStream(downloadEvent
            -> new DownloadResponse(new ByteArrayInputStream(super.aceEditor.getValue().getBytes())
            , "wiretap.txt",
            "text/plain", -1)), "");

        downloadAnchor.add(downloadButton);

        Button newWindowButton = new TableButton(VaadinIcon.EXTERNAL_LINK.create());
        newWindowButton.addClickListener(buttonClickEvent -> {
            EntityContentsViewDialog entityContentsViewDialog = new EntityContentsViewDialog("Wiretap " + wiretapEvent.getEventId());
            entityContentsViewDialog.populate(this.wiretapEvent);
        });
        HorizontalLayout buttonLayout = new HorizontalLayout(super.select, downloadAnchor, newWindowButton);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, super.select);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, downloadAnchor, newWindowButton);

        VerticalLayout layout = new VerticalLayout();
        layout.add(headerLayout, formLayout, buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        return layout;
    }

    @Override
    public void populate(IkasanESBDocument wiretapEvent)
    {
        this.wiretapEvent = wiretapEvent;
        super.title.setText("Wiretap " + wiretapEvent.getEventId());
        this.moduleNameTf.setValue(wiretapEvent.getModuleName());
        this.flowNameTf.setValue(wiretapEvent.getFlowName());
        this.componentNameTf.setValue(wiretapEvent.getComponentName());

        String route = RouteConfiguration.forSessionScope()
            .getUrl(EventLifeIdDeepLinkView.class, wiretapEvent.getEventId());
        Anchor link = new Anchor(route, wiretapEvent.getEventId());
        link.setTarget("_blank");
        add(link);
        link.getStyle().set("color", "blue");
        this.eventIdTf.setValue(" ");
        eventIdTf.setPrefixComponent(link);
        this.dateTimeTf.setValue(this.dateFormatter.getFormattedDate(wiretapEvent.getTimestamp()));

        super.open(wiretapEvent.getEvent());
    }
}
