package org.ikasan.dashboard.ui.security.view;


import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.security.ContextCache;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.SessionAttributeConstants;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.security.model.User;
import org.ikasan.security.service.AuthenticationService;
import org.ikasan.security.service.AuthenticationServiceException;
import org.ikasan.security.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@Tag("sa-login-view")
@Route(LoginView.ROUTE)
@PageTitle("Ikasan - Login")
//@HtmlImport("frontend://styles/shared-styles.html")
//@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@Component
@UIScope
@AnonymousAllowed
public class LoginView extends VerticalLayout //implements AppShellConfigurator //implements PageConfigurator//, HasUrlParameter<String>
{
    public static final String ROUTE = "login";

    @Resource
    private AuthenticationService authenticationService;

    @Resource
    private UserService userService;

    @Resource
    private SystemEventLogger systemEventLogger;

    @Resource
    private SecurityContextRepository securityContextRepository;


    private LoginForm login = new LoginForm();

    public LoginView()
    {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        login.setForgotPasswordButtonVisible(false);

        Image ikasan = new Image(new StreamResource("Mr Squid",
            () -> LoginView.class.getResourceAsStream("/META-INF/resources/frontend/images/mr_squid_titling_dashboard.png")), "Mr Squid");
        ikasan.setHeight("180px");

        Div loginDiv = new Div();
        loginDiv.add(login);

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, ikasan, loginDiv);

        layout.add(ikasan, loginDiv);

        login.addLoginListener((ComponentEventListener<AbstractLogin.LoginEvent>) loginEvent ->
        {

            try
            {
                Authentication authentication = this.authenticationService.login(loginEvent.getUsername(),
                    loginEvent.getPassword());

                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
                securityContextRepository.saveContext(securityContext, VaadinServletRequest.getCurrent()
                    , VaadinServletResponse.getCurrent());

                User user = this.userService.loadUserByUsername(authentication.getName());
                user.setPreviousAccessTimestamp(System.currentTimeMillis());
                this.userService.updateUser(user);

                UI.getCurrent().getPage().retrieveExtendedClientDetails(extendedClientDetails -> {
                    this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_LOGIN_CONSTANTS
                        , SystemEventConstants.DASHBOARD_LOGIN_CONSTANTS, authentication.getName());

                    UI.getCurrent().getSession().setAttribute(SessionAttributeConstants.TIMEZONE_ID,
                        extendedClientDetails.getTimeZoneId());

                    String cacheContext = ContextCache.getContext(UI.getCurrent().getSession().getSession().getId());
                    if(cacheContext != null && isRouteValid(cacheContext)) {
                        UI.getCurrent().navigate(cacheContext);
                    }
                    else {
                        DashboardContextNavigator.navigateToLandingPage();
                    }
                });
            }
            catch (AuthenticationServiceException e)
            {
                login.setError(true);
            }
        });

        this.add(layout);
    }

    private boolean isRouteValid(String context) {
        if(context.isEmpty()) {
            return true;
        }

        List<RouteData> routes = RouteConfiguration.forApplicationScope().getAvailableRoutes();

        return routes.stream().anyMatch(route -> {
            String url = route.getTemplate();

            if(!url.isEmpty() && context.startsWith(url + "/")){
                return true;
            }

            return false;
        });
    }

//    @Override
//    public void configurePage(AppShellSettings settings) {
//        HashMap<String, String> attributes = new HashMap<>();
//        attributes.put("rel", "shortcut icon");
//        attributes.put("type", "image/png");
//        settings.addLink("icons/icon.png", attributes);
//        AppShellConfigurator.super.configurePage(settings);
//    }

//    @Override
//    public void configurePage(InitialPageSettings settings) {
//        HashMap<String, String> attributes = new HashMap<>();
//        attributes.put("rel", "shortcut icon");
//        attributes.put("type", "image/png");
//        settings.addLink("icons/icon.png", attributes);
//    }
}