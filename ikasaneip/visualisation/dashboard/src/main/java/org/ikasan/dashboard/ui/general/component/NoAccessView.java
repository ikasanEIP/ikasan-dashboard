package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Where a logged in user lands when their roles give them no part of the dashboard: no dashboard and no scheduler
 * authority (see DashboardContextNavigator). It says so and lets them log out, instead of a "could not navigate" error.
 */
@Route(value = "noaccess")
public class NoAccessView extends VerticalLayout implements BeforeEnterObserver
{
    private final H3 message = new H3();

    public NoAccessView()
    {
        Image image = new Image("/frontend/images/mr-squid-head.png", "");
        image.setHeight("300px");

        message.setId("noAccessMessage");
        Paragraph advice = new Paragraph(getTranslation("message.no-access-advice", UI.getCurrent().getLocale()));

        Button logout = new Button(getTranslation("tooltip.log-out", UI.getCurrent().getLocale()));
        logout.setId("noAccessLogoutButton");
        logout.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        logout.addClickListener(event ->
        {
            SecurityContextHolder.getContext().setAuthentication(null);
            VaadinSession.getCurrent().close();
            UI.getCurrent().getPage().setLocation("/login");
        });

        this.setSizeFull();
        this.setSpacing(true);
        this.setMargin(true);
        this.add(image, message, advice, logout);
        this.setHorizontalComponentAlignment(Alignment.CENTER, image, message, advice, logout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        message.setText(String.format(getTranslation("message.no-access", UI.getCurrent().getLocale())
            , authentication != null ? authentication.getName() : ""));
    }
}
