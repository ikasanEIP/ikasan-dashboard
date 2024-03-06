package org.ikasan.dashboard.ui.layout;


//import com.github.appreciated.app.layout.component.appbar.AppBarBuilder;
//import com.github.appreciated.app.layout.component.appbar.IconButton;
//import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
//import com.github.appreciated.app.layout.component.builder.AppLayoutBuilder;
//import com.github.appreciated.app.layout.component.menu.left.LeftMenuComponentWrapper;
//import com.github.appreciated.app.layout.component.menu.left.LeftSubmenu;
//import com.github.appreciated.app.layout.component.menu.left.builder.LeftAppMenuBuilder;
//import com.github.appreciated.app.layout.component.menu.left.builder.LeftSubMenuBuilder;
//import com.github.appreciated.app.layout.component.menu.left.items.LeftNavigationItem;
//import com.github.appreciated.app.layout.component.router.AppLayoutRouterLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.ikasan.dashboard.ui.administration.view.*;
import org.ikasan.dashboard.ui.dashboard.view.DashboardView;
import org.ikasan.dashboard.ui.general.component.AboutIkasanDialog;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerView;
import org.ikasan.dashboard.ui.search.view.SearchView;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.view.BusinessStreamDesignerView;
import org.ikasan.dashboard.ui.visualisation.view.GraphView;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Resource;


//@Push
@JsModule("./styles/shared-styles.js")
@CssImport("./styles/shared-styles.css")
@CssImport(value = "./styles/dialog-overlay.css", themeFor = "vaadin-dialog-overlay")
//@PreserveOnRefresh
//@UIScope
//@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
//@Theme(themeClass = Material.class)
//@PWA(name = "Ikasan Visualisation Dashboard",
//    shortName = "Ikasan")
public class IkasanAppLayout extends AppLayout
{
    @Resource
    private SystemEventLogger systemEventLogger;

    @Value("${banner.text.message:}")
    private String bannerTextMessage;

    @Value("${banner.text.color:}")
    private String bannerTextColor;

    @Value("${is.ikasan.enterprise.scheduler.instance:true}")
    private boolean isIkasanEnterpriseSchedulerInstance;

    private SideNavItem adminMenuItem;
    private SideNavItem dashboardMenuItem;
    private SideNavItem searchMenuItem;
    private SideNavItem visualisationMenuItem;
    private SideNavItem schedulerMenuItem;
    private SideNavItem systemEventMenuItem;
    private SideNavItem userManagementMenuItem;
    private SideNavItem groupManagementMenuItem;
    private SideNavItem roleManagementMenuItem;
    private SideNavItem policyManagementMenuItem;
    private SideNavItem userDirectoryManagementMenuItem;
    private SideNavItem businessStreamDesignerMenuItem;
    private SideNavItem quartzSchedulerMenuItem;

    private Button swaggerUI;

    public IkasanAppLayout()
    {
        Image ikasan = new Image("frontend/images/ikasan-titling-transparent.png", "");
        ikasan.setHeight("30px");

        Button logout = new Button(VaadinIcon.SIGN_OUT.create());
        logout.getElement().setProperty("title", "Log Out");
        logout.setId("logoutButton");

//        SecurityContextHolder.setContextHolderStrategy(new VaadinAwareSecurityContextHolderStrategy());
        logout.addClickListener((ComponentEventListener<ClickEvent<Button>>) divClickEvent ->
        {
            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();

            this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_LOGOUT_CONSTANTS
                , SystemEventConstants.DASHBOARD_LOGOUT_CONSTANTS, authentication.getName());
            SecurityContextHolder.getContext().setAuthentication(null);
            UI.getCurrent().navigate("");
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().getPage().executeJs("window.location.href=''");
            UI.getCurrent().getSession().close();
        });

        this.swaggerUI = new Button(VaadinIcon.CODE.create());
        this.swaggerUI.getElement().setProperty("title", "Swagger UI");
        this.swaggerUI.addClickListener(event -> UI.getCurrent().getPage().open("/swagger-ui.html", "_blank"));

        Button aboutButton = new Button(VaadinIcon.QUESTION.create());
        aboutButton.setId("aboutButton");
        aboutButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            AboutIkasanDialog aboutIkasanDialog = new AboutIkasanDialog();
            aboutIkasanDialog.open();
        });
        var header = new HorizontalLayout(new DrawerToggle(), ikasan, aboutButton, swaggerUI, logout);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
        addToDrawer(getSideNav());
//        AppLayoutBuilder appLayoutBuilder = AppLayoutBuilder
//            .get(LeftLayouts.LeftHybridSmall.class)
//            .withIconComponent(ikasan)
//            .withAppBar(AppBarBuilder.get()
//                .add(aboutButton)
//                .add(swaggerUI)
//                .add(logout)
//                .build());

//        LeftAppMenuBuilder leftAppMenuBuilder = LeftAppMenuBuilder.get();

//        this.dashboardMenuItem = new SideNavItem("Dashboard", DashboardView.class,  VaadinIcon.DASHBOARD.create());
//        this.dashboardMenuItem.setId("dashboardMenuItem");
//        this.dashboardMenuItem.getElement().getStyle().remove("padding");
//        leftAppMenuBuilder = leftAppMenuBuilder.add(dashboardMenuItem);

//        this.searchMenuItem = new LeftNavigationItem(getTranslation("menu-item.search", UI.getCurrent().getLocale(), null), VaadinIcon.SEARCH.create(), SearchView.class);
//        this.searchMenuItem.setId("searchMenuItem");
//        leftAppMenuBuilder = leftAppMenuBuilder.add(searchMenuItem);
//
//
//        this.visualisationMenuItem = new LeftNavigationItem(getTranslation("menu-item.visualisation", UI.getCurrent().getLocale(), null), VaadinIcon.CLUSTER.create(), GraphView.class);
//        this.visualisationMenuItem.setId("visualisationMenuItem");
//
//        leftAppMenuBuilder = leftAppMenuBuilder.add(this.visualisationMenuItem);
//
//        this.schedulerMenuItem = new LeftNavigationItem("Scheduler", VaadinIcon.CLOCK.create(), SchedulerView.class);
//        this.schedulerMenuItem.setId("schedulerMenuItem");
//
//        leftAppMenuBuilder = leftAppMenuBuilder.add(this.schedulerMenuItem);


    }

    private SideNav getSideNav() {
        SideNav sideNav = new SideNav();

        this.dashboardMenuItem = new SideNavItem("Dashboard", DashboardView.class,  VaadinIcon.DASHBOARD.create());
        this.dashboardMenuItem.setId("dashboardMenuItem");
        this.dashboardMenuItem.getElement().getStyle().remove("padding");

        sideNav.addItem(this.dashboardMenuItem);

        this.searchMenuItem = new SideNavItem(getTranslation("menu-item.search", getLocale(), null), SearchView.class, VaadinIcon.SEARCH.create());
        this.searchMenuItem.setId("searchMenuItem");

        sideNav.addItem(this.searchMenuItem);


        this.visualisationMenuItem = new SideNavItem(getTranslation("menu-item.visualisation", getLocale(), null), GraphView.class, VaadinIcon.CLUSTER.create());
        this.visualisationMenuItem.setId("visualisationMenuItem");

        sideNav.addItem(this.visualisationMenuItem);

        this.schedulerMenuItem = new SideNavItem("Scheduler", SchedulerView.class, VaadinIcon.CLOCK.create());
        this.schedulerMenuItem.setId("schedulerMenuItem");

        sideNav.addItem(this.schedulerMenuItem);

        this.buildAdminSideNav();
        sideNav.addItem(this.adminMenuItem);
        return sideNav;
    }

    private void buildAdminSideNav() {
        this.adminMenuItem = new SideNavItem(getTranslation("menu-item.administration", getLocale()));
        this.adminMenuItem.setPrefixComponent(VaadinIcon.TOOLS.create());
        this.adminMenuItem.setId("adminMenuItem");
//        LeftSubMenuBuilder leftSubMenuBuilder = LeftSubMenuBuilder
//            .get(getTranslation("menu-item.administration", getLocale(), null), VaadinIcon.TOOLS.create());
//
        this.systemEventMenuItem = new SideNavItem(getTranslation("menu-item.administration-events", getLocale(), null)
            , AdministrationSearchView.class, VaadinIcon.CROSSHAIRS.create());
        this.systemEventMenuItem.setId("systemEventMenuItem");
        adminMenuItem.addItem(this.systemEventMenuItem);

        this.userManagementMenuItem = new SideNavItem(getTranslation("menu-item.users",
            getLocale(), null), UserManagementView.class, VaadinIcon.USERS.create());
        this.userManagementMenuItem.setId("userManagementMenuItem");
        adminMenuItem.addItem(this.userManagementMenuItem);

        this.groupManagementMenuItem = new SideNavItem(getTranslation("menu-item.groups",
            getLocale(), null),  GroupManagementView.class, VaadinIcon.GROUP.create());
        this.groupManagementMenuItem.setId("groupManagementMenuItem");
        adminMenuItem.addItem(this.groupManagementMenuItem);

        this.roleManagementMenuItem = new SideNavItem(getTranslation("menu-item.roles",
            getLocale(), null), RoleManagementView.class, VaadinIcon.DOCTOR.create());
        this.roleManagementMenuItem.setId("roleManagementMenuItem");
        adminMenuItem.addItem(this.roleManagementMenuItem);

        this.policyManagementMenuItem = new SideNavItem(getTranslation("menu-item.policies",
            getLocale(), null), PolicyManagementView.class, VaadinIcon.SAFE.create());
        this.policyManagementMenuItem.setId("policyManagementMenuItem");
        adminMenuItem.addItem(this.policyManagementMenuItem);

        this.userDirectoryManagementMenuItem = new SideNavItem(getTranslation("menu-item.user-directories",
            getLocale(), null), UserDirectoriesView.class, VaadinIcon.COG.create());
        this.userDirectoryManagementMenuItem.setId("userDirectoryManagementMenuItem");
        adminMenuItem.addItem(this.userDirectoryManagementMenuItem);

        this.quartzSchedulerMenuItem = new SideNavItem(getTranslation("menu-item.quartz-scheduler",
            getLocale(), null), QuartzSchedulerView.class, VaadinIcon.CALENDAR_CLOCK.create());
        this.quartzSchedulerMenuItem.setId("quartzSchedulerViewMenuItem");
        adminMenuItem.addItem(this.quartzSchedulerMenuItem);

        this.businessStreamDesignerMenuItem = new SideNavItem("Designer", BusinessStreamDesignerView.class, VaadinIcon.PALETTE.create());
        this.businessStreamDesignerMenuItem.setId("businessStreamDesignerMenuItem");
        this.businessStreamDesignerMenuItem.getElement().getThemeList().remove("spacing-s");
        adminMenuItem.addItem(this.businessStreamDesignerMenuItem);
//
//        if(leftSubMenuBuilder != null)
//        {
//            this.leftSubmenu = leftSubMenuBuilder.build();
//            this.leftSubmenu.getSubmenuContainer().getThemeList().remove("spacing-s");
//            leftAppMenuBuilder.add(this.leftSubmenu);
//        }
//
//        this.leftAppMenu = (LeftMenuComponentWrapper) leftAppMenuBuilder.build();
//        this.leftAppMenu.getMenu().getThemeList().remove("spacing-s");
//
//        init((LeftLayouts.LeftHybridSmall)appLayoutBuilder.withAppMenu(leftAppMenu).build());
    }

    @Override
    public void onAttach(AttachEvent attachEvent)
    {
        if(this.bannerTextMessage != null && !this.bannerTextMessage.isEmpty()) {
            Span bannerText = new Span(bannerTextMessage);
            bannerText.getElement().getStyle().set("font-size", "30pt");
            bannerText.getElement().getStyle().set("color", bannerTextColor);

            HorizontalLayout bannerLayout = new HorizontalLayout(bannerText);
            bannerLayout.getElement().getStyle().set("position", "absolute");
            bannerLayout.getElement().getStyle().set("left", "50%");
            bannerLayout.getElement().getStyle().set("transform", "translate(-50%)");
//            super.getAppLayout().setTitleComponent(bannerLayout);
        }

        super.onAttach(attachEvent);
        this.dashboardMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY, SecurityConstants.DASHBOARD_READ, SecurityConstants.DASHBOARD_WRITE,
            SecurityConstants.DASHBOARD_ADMIN));

        this.searchMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SEARCH_ADMIN, SecurityConstants.SEARCH_READ, SecurityConstants.SEARCH_WRITE,
            SecurityConstants.ALL_AUTHORITY));

        this.schedulerMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ, SecurityConstants.SCHEDULER_WRITE
            , SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_ADMIN)
            && isIkasanEnterpriseSchedulerInstance);

        this.visualisationMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY));

//        this.leftSubmenu.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY, SecurityConstants.USER_ADMINISTRATION_ADMIN, SecurityConstants.USER_ADMINISTRATION_WRITE,
//            SecurityConstants.USER_ADMINISTRATION_READ, SecurityConstants.USER_DIRECTORY_ADMIN, SecurityConstants.USER_DIRECTORY_WRITE, SecurityConstants.USER_DIRECTORY_READ,
//            SecurityConstants.GROUP_ADMINISTRATION_ADMIN, SecurityConstants.GROUP_ADMINISTRATION_WRITE, SecurityConstants.GROUP_ADMINISTRATION_READ,
//            SecurityConstants.POLICY_ADMINISTRATION_ADMIN, SecurityConstants.POLICY_ADMINISTRATION_READ, SecurityConstants.POLICY_ADMINISTRATION_WRITE,
//            SecurityConstants.ROLE_ADMINISTRATION_ADMIN, SecurityConstants.ROLE_ADMINISTRATION_READ, SecurityConstants.ROLE_ADMINISTRATION_WRITE,SecurityConstants.ALL_AUTHORITY,
//            SecurityConstants.SYSTEM_EVENT_ADMIN, SecurityConstants.SYSTEM_EVENT_READ, SecurityConstants.SYSTEM_EVENT_WRITE));

        this.systemEventMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY, SecurityConstants.SYSTEM_EVENT_ADMIN, SecurityConstants.SYSTEM_EVENT_READ,
            SecurityConstants.SYSTEM_EVENT_WRITE));

        this.userManagementMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY, SecurityConstants.USER_ADMINISTRATION_ADMIN, SecurityConstants.USER_ADMINISTRATION_WRITE,
            SecurityConstants.USER_ADMINISTRATION_READ));

        this.groupManagementMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY, SecurityConstants.GROUP_ADMINISTRATION_ADMIN, SecurityConstants.GROUP_ADMINISTRATION_WRITE,
            SecurityConstants.GROUP_ADMINISTRATION_READ));

        this.roleManagementMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY, SecurityConstants.ROLE_ADMINISTRATION_ADMIN, SecurityConstants.ROLE_ADMINISTRATION_READ,
            SecurityConstants.ROLE_ADMINISTRATION_WRITE));

        this.policyManagementMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY, SecurityConstants.POLICY_ADMINISTRATION_ADMIN, SecurityConstants.POLICY_ADMINISTRATION_READ,
            SecurityConstants.POLICY_ADMINISTRATION_WRITE));

        this.userDirectoryManagementMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.USER_DIRECTORY_ADMIN, SecurityConstants.USER_DIRECTORY_WRITE, SecurityConstants.USER_DIRECTORY_READ));

        this.swaggerUI.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY));

        this.businessStreamDesignerMenuItem.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.BUSINESS_STREAM_ADMIN));
    }


//    @Override
//    public void configurePage(InitialPageSettings settings) {
//        HashMap<String, String> attributes = new HashMap<>();
//        attributes.put("rel", "shortcut icon");
//        attributes.put("type", "image/png");
//        settings.addLink("icons/icon.png", attributes);
//    }
}
