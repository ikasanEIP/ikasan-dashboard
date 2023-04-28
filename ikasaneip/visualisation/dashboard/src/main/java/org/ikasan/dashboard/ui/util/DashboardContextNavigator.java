package org.ikasan.dashboard.ui.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;

public class DashboardContextNavigator {

    public static void navigateToLandingPage() {
        if(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.DASHBOARD_READ
            , SecurityConstants.DASHBOARD_WRITE
            , SecurityConstants.DASHBOARD_ADMIN)) {
            UI.getCurrent().navigate("");
        } else if (ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SCHEDULER_READ
            , SecurityConstants.SCHEDULER_WRITE
            , SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_DEV_READ
            , SecurityConstants.SCHEDULER_DEV_WRITE
            , SecurityConstants.SCHEDULER_DEV_ADMIN
            , SecurityConstants.SCHEDULER_ALL_READ
            , SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN)) {
            UI.getCurrent().navigate("scheduler");
        }
        else {
            UI.getCurrent().navigate("noaccess");
        }
    }

    public static void navigateToLandingPage(BeforeEnterEvent event) {
        if(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.DASHBOARD_READ
            , SecurityConstants.DASHBOARD_WRITE
            , SecurityConstants.DASHBOARD_ADMIN)) {
            event.rerouteTo("");
        } else if (ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SCHEDULER_READ
            , SecurityConstants.SCHEDULER_WRITE
            , SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_DEV_READ
            , SecurityConstants.SCHEDULER_DEV_WRITE
            , SecurityConstants.SCHEDULER_DEV_ADMIN
            , SecurityConstants.SCHEDULER_ALL_READ
            , SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN)) {
            event.rerouteTo("scheduler");
        }
        else {
            event.rerouteTo("noaccess");
        }
    }
}
