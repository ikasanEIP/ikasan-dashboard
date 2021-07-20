package org.ikasan.dashboard.ui.dashboard.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.dashboard.component.*;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.search.component.ChangePasswordDialog;
import org.ikasan.security.model.User;
import org.ikasan.security.service.UserService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Route(value = "", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class DashboardView extends HorizontalLayout implements BeforeEnterObserver
{
    @Resource
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Resource
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    @Resource
    private UserService userService;

    private Board board;

    private boolean initialised = false;

    public DashboardView()
    {
        board = new Board();
        board.addClassName("styled");
        board.setSizeFull();

        this.add(board);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            board.addRow(new BusinessStreamWidget(this.businessStreamMetaDataService, this.moduleMetadataService)
                , new ModuleWidget(moduleMetadataService), new StatusWidget(moduleMetadataService, UI.getCurrent()));
            board.addRow(new HospitalEventsWidget(solrGeneralService), new ErrorEventWidget(solrGeneralService));

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

