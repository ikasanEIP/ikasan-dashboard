package org.ikasan.dashboard.ui.dashboard.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.dashboard.component.*;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.search.component.ChangePasswordDialog;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.service.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.DownloadLogFileService;
import org.ikasan.spec.security.model.User;
import org.ikasan.spec.security.service.UserService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.security.PermitAll;

@Route(value = "", layout = IkasanAppLayout.class)
@UIScope
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@PermitAll
@PreserveOnRefresh
public class DashboardView extends HorizontalLayout implements BeforeEnterObserver
{
    @Autowired
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Autowired
    private DownloadLogFileService downloadLogFileService;

    @Autowired
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    @Autowired
    private UserService userService;

    private Dashboard board;

    private boolean initialised = false;

    public DashboardView()
    {
        board = new Dashboard();
        board.setMinimumColumnWidth("100px");
        board.setDenseLayout(true);
        board.setSizeFull();
        board.setMaximumColumnCount(12);
        board.setMinimumRowHeight("100px");

        this.add(board);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {

        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.DASHBOARD_READ
            , SecurityConstants.DASHBOARD_WRITE
            , SecurityConstants.DASHBOARD_ADMIN
            , SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage(beforeEnterEvent);
            return;
        }

        if(!initialised) {
            DashboardWidget businessStreams = new BusinessStreamWidget(this.businessStreamMetaDataService, this.moduleMetadataService);
            businessStreams.setColspan(4);
            board.add(businessStreams);

            DashboardWidget modules = new ModuleWidget(moduleMetadataService, downloadLogFileService);
            modules.setColspan(4);
            board.add(modules);

            DashboardWidget status = new StatusWidget(moduleMetadataService, UI.getCurrent());
            status.setColspan(4);
            board.add(status);

            DashboardWidget hospital = new HospitalEventsWidget(solrGeneralService);
            hospital.setColspan(6);
            board.add(hospital);

            DashboardWidget error = new ErrorEventWidget(solrGeneralService);
            error.setColspan(6);
            board.add(error);

            initialised = true;

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if(authentication != null)
            {
                User user = this.userService.loadUserByUsername(authentication.getName());

                if (user.isRequiresPasswordChange())
                {
                    ChangePasswordDialog dialog = new ChangePasswordDialog(user, this.userService);
                    dialog.setCloseOnOutsideClick(false);
                    dialog.setCloseOnEsc(false);

                    dialog.open();
                }
            }
        }
    }
}

