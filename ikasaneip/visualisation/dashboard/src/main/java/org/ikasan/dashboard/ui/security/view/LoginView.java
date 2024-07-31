package org.ikasan.dashboard.ui.security.view;


import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
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
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.security.model.User;
import org.ikasan.security.service.AuthenticationService;
import org.ikasan.security.service.AuthenticationServiceException;
import org.ikasan.security.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

@Tag("sa-login-view")
@Route(LoginView.ROUTE)
@PageTitle("Ikasan - Login")
@Component
@UIScope
@AnonymousAllowed
public class LoginView extends VerticalLayout
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

        H3 environmentLabel = new H3("PRODUCTION");

        Div loginDiv = new Div();
        loginDiv.add(login);

        BuildProperties buildProperties = (BuildProperties) ApplicationContextProvider.getContext().getBean("buildProperties");

        NativeLabel versionLabel = new NativeLabel("Version: " + buildProperties.getVersion());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy hh:mm:ss")
            .withZone(ZoneId.systemDefault());
        NativeLabel timestamp =  new NativeLabel("Build date/time: " + formatter.format(buildProperties.getTime()));

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, ikasan, environmentLabel, loginDiv, versionLabel, timestamp);

        layout.add(ikasan, environmentLabel, loginDiv, versionLabel, timestamp);

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
}