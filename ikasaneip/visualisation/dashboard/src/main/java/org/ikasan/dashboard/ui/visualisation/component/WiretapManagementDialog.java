package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.model.flow.AbstractWiretapNode;
import org.ikasan.dashboard.ui.visualisation.model.flow.Flow;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.model.Figure;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.DecoratorMetaData;
import org.ikasan.spec.module.client.TriggerService;
import org.ikasan.spec.trigger.TriggerRelationship;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WiretapManagementDialog extends Dialog
{
    public static final String BEFORE = TriggerRelationship.BEFORE.getDescription();
    public static final String AFTER = TriggerRelationship.AFTER.getDescription();
    public static final String WIRETAP = "wiretap";
    public static final String LOG = "log";

    private TriggerService triggerRestService;
    private Module module;
    private Flow flow;
    private DesignerCanvas designerCanvas;
    private Figure wiretapFigure;
    private String type;
    private String relationship;

    protected WiretapManagementDialog(TriggerService triggerRestService
        , Module module, Flow flow
        , Figure wiretapFigure, DesignerCanvas designerCanvas
        , String type, String relationship)
    {
        this.triggerRestService = triggerRestService;
        this.module = module;
        this.flow = flow;
        this.wiretapFigure = wiretapFigure;
        this.designerCanvas = designerCanvas;
        this.type = type;
        this.relationship = relationship;

        init();
    }

    private void init()
    {
        VerticalLayout verticalLayout = new VerticalLayout();

        Image mrSquidImage = new Image("/frontend/images/mr-squid-head.png", "");
        mrSquidImage.setHeight("35px");

        H3 flowOptions = new H3(String.format(getTranslation("label.wiretap-management", UI.getCurrent().getLocale())));

        HorizontalLayout header = new HorizontalLayout();
        header.add(mrSquidImage, flowOptions);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, mrSquidImage, flowOptions);

        verticalLayout.add(header);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, header);

        Button removeWiretapButton = new Button(getTranslation("button.remove-wiretap", UI.getCurrent().getLocale()));
        removeWiretapButton.setWidthFull();
        IkasanAuthentication ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        removeWiretapButton.addClickListener((ComponentEventListener<ClickEvent<Button>>)
            buttonClickEvent -> {
                AtomicBoolean success = new AtomicBoolean(true);

                if(!this.triggerRestService.delete(this.module.getUrl(), this.wiretapFigure.getUserData().getIdentifier(), ikasanAuthentication.getName())) {
                    success.set(false);
                };


                if(success.get()) {
                    UI.getCurrent().access(() -> this.designerCanvas.removeFigure(wiretapFigure.getIdentifier()));
                    NotificationHelper.showUserNotification(getTranslation("notification.wiretap-removed", UI.getCurrent().getLocale()));
                }
                else {
                    NotificationHelper.showErrorNotification(getTranslation("notification.error-removing-wiretap", UI.getCurrent().getLocale()));
                }

                this.close();
        });

        ComponentSecurityVisibility.applySecurity(removeWiretapButton, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.WIRETAP_ADMIN
            , SecurityConstants.WIRETAP_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_WRITE
            , SecurityConstants.WIRETAP_ALL_MODULES_ADMIN);

        verticalLayout.add(removeWiretapButton);

        this.add(verticalLayout);
    }
}
