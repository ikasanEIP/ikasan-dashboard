package org.ikasan.dashboard.ui.security.view;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.servlet.http.Cookie;
import org.ikasan.dashboard.internationalisation.IkasanI18NProvider;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Tag("sa-login-view")
@Route(LoginView.ROUTE)
@PageTitle("Ikasan - Login")
@Component
@UIScope
@AnonymousAllowed
public class LoginView extends VerticalLayout implements LocaleChangeObserver
    , AfterNavigationObserver, BeforeEnterObserver
{
    public static final String ROUTE = "login";

    @Resource
    private transient AuthenticationService authenticationService;

    @Resource
    private transient UserService userService;

    @Resource
    private transient SystemEventLogger systemEventLogger;

    @Resource
    private transient SecurityContextRepository securityContextRepository;

    @Value("${banner.text.message:}")
    private String bannerTextMessage;

    private NativeLabel versionLabel;
    private NativeLabel timestamp;

    private LoginForm login = new LoginForm();

    private H3 environmentLabel;

    private Select<Locale> lang;

    public LoginView()
    {
        super();
    }

    private void init() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        login.setForgotPasswordButtonVisible(false);
        login.setI18n(this.getI18n());

        Image ikasan = new Image(new StreamResource("Mr Squid",
            () -> LoginView.class.getResourceAsStream("/META-INF/resources/frontend/images/mr_squid_titling_dashboard.png")), "Mr Squid");
        ikasan.setHeight("180px");

        this.environmentLabel = new H3();

        BuildProperties buildProperties = (BuildProperties) DashboardApplicationContextProvider.getContext().getBean("buildProperties");

        versionLabel = new NativeLabel(getTranslation("label.build-version", getLocale()) + " " + buildProperties.getVersion());
        versionLabel.getStyle().set("margin-top", "50px");
        timestamp =  new NativeLabel(getTranslation("label.build-date-time", getLocale()) + " " + DateFormatter.instance(ZoneId.systemDefault())
            .getLongFormattedDate(buildProperties.getTime().toEpochMilli()));
        timestamp.getStyle().set("margin-top", "20px");

        this.buildLanguageSelect();

        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, ikasan, environmentLabel, login, this.lang, versionLabel, timestamp);
        layout.add(ikasan, environmentLabel, login, this.lang, versionLabel, timestamp);

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
                loginEvent.getSource().setError(true);
            }
        });

        this.add(layout);
    }

    private void buildLanguageSelect() {
        lang = new Select<>();
        lang.setLabel(getTranslation("label.language", getLocale()));
        lang.setItems(IkasanI18NProvider.providedLocales);
        lang.setItemLabelGenerator(item -> item.getDisplayLanguage(item));

        Cookie localeCookie = CookieUtil.getCookieByName("ikasan-language",
            VaadinRequest.getCurrent());

        if(localeCookie != null) {
            Optional<Locale> optionalLocale = IkasanI18NProvider.providedLocales.stream().filter(locale
                -> locale.getLanguage().equals(localeCookie.getValue())).findFirst();

            if(optionalLocale.isPresent()) {
                lang.setValue(optionalLocale.get());
            }
            else {
                lang.setValue(Locale.ENGLISH);
            }
        }

        lang.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                getUI().ifPresent(ui -> {
                    ui.getSession().setAttribute("locale",
                        e.getValue().getLanguage());
                    ui.setLocale(e.getValue());
                    Cookie myCookie = new Cookie("ikasan-language", e.getValue().getLanguage());
                    myCookie.setMaxAge(60 * 60 * 24 * 7 * 52);
                    myCookie.setPath("/");
                    VaadinService.getCurrentResponse().addCookie(myCookie);
                });
            }
        });
    }

    private LoginI18n getI18n() {
        LoginI18n i18n = new LoginI18n();

        LoginI18n.Form form = new LoginI18n.Form();
        form.setPassword(getTranslation("label.password"));
        form.setUsername(getTranslation("label.username"));
        form.setTitle(getTranslation("label.login"));
        form.setSubmit(getTranslation("button.login"));
        i18n.setForm(form);
        LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle(getTranslation("error.login-title"));
        errorMessage.setMessage(getTranslation("error.login-message"));
        i18n.setErrorMessage(errorMessage);
        return i18n;
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.init();
        this.environmentLabel.setText(this.bannerTextMessage);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        Cookie localeCookie = CookieUtil.getCookieByName("ikasan-language",
            VaadinRequest.getCurrent());
        if (localeCookie != null && localeCookie.getValue() != null) {
            Optional<Locale> locale = IkasanI18NProvider.providedLocales.stream()
                .filter(loc -> loc.getLanguage()
                    .equals(localeCookie.getValue()))
                .findFirst();
            if(lang != null) {
                lang.setValue(locale.get());
            }
            VaadinSession.getCurrent().setLocale(locale.get());
            event.getLocationChangeEvent().getUI().setLocale(locale.get());
        }
    }

    @Override
    public void localeChange(LocaleChangeEvent localeChangeEvent) {
        login.setI18n(getI18n());
        BuildProperties buildProperties = (BuildProperties) DashboardApplicationContextProvider.getContext().getBean("buildProperties");
        if(versionLabel != null && timestamp != null && this.lang != null) {
            versionLabel.setText(getTranslation("label.build-version", getLocale()) + " " + buildProperties.getVersion());
            timestamp.setText(getTranslation("label.build-date-time", getLocale()) + " " + DateFormatter.instance(ZoneId.systemDefault())
                .getLongFormattedDate(buildProperties.getTime().toEpochMilli()));

            lang.setLabel(getTranslation("label.language", getLocale()));
        }
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



    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Cookie uiLanguage = CookieUtil.getCookieByName("ikasan-language", VaadinRequest.getCurrent());

        if (uiLanguage != null){
            Optional<Locale> optionalLocale = IkasanI18NProvider.providedLocales.stream()
                .filter(locale -> locale.getLanguage().equals(uiLanguage.getValue()))
                .findFirst();

            if(optionalLocale.isPresent()) {
                beforeEnterEvent.getUI().getSession().setAttribute("locale",
                    optionalLocale.get().getLanguage());
                VaadinSession.getCurrent().setLocale(optionalLocale.get());
                beforeEnterEvent.getUI().setLocale(optionalLocale.get());
            }
        }
    }
}