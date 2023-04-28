package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Route(value = "adminSearchView", layout = IkasanAppLayout.class)
@UIScope
@Component
@PageTitle("Ikasan - Administration Search")
public class AdministrationSearchView extends VerticalLayout implements BeforeEnterObserver
{
    private Logger logger = LoggerFactory.getLogger(AdministrationSearchView.class);

    @Resource
    private SystemEventSearchService systemEventSearchService;

    private Tabs tabs;

    private SystemEventSearchView systemEventSearchView;

    @Resource
    private DateFormatter dateFormatter;

    @Value("${max.download.bytes:50000000}")
    private int maxDownloadBytes;

    /**
     * Constructor
     */
    public AdministrationSearchView()
    {
        super();
    }

    protected void init()
    {
        tabs = new Tabs();
        tabs.getElement().getThemeList().remove("padding");
        tabs.setId("tabs");

        this.systemEventSearchView = new SystemEventSearchView(this.systemEventSearchService, this.dateFormatter, this.maxDownloadBytes);
        this.systemEventSearchView.init();
        this.systemEventSearchView.getThemeList().remove("padding");

        Tab systemEventTab = new Tab(getTranslation("tab-label.system-events", UI.getCurrent().getLocale()));
        tabs.add(systemEventTab);

        this.add(tabs, systemEventSearchView);
        this.setSizeFull();
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SYSTEM_EVENT_ADMIN, SecurityConstants.SYSTEM_EVENT_WRITE
            , SecurityConstants.SYSTEM_EVENT_READ, SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage();
            return;
        }

        if(this.tabs == null) {
            init();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {

    }
}
