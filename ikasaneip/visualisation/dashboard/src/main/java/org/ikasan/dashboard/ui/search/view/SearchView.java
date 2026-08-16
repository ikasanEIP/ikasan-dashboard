package org.ikasan.dashboard.ui.search.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.general.component.SearchResults;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.search.component.ChangePasswordDialog;
import org.ikasan.dashboard.ui.search.component.SearchForm;
import org.ikasan.dashboard.ui.search.listener.SearchListener;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ReplayService;
import org.ikasan.spec.module.client.ResubmissionService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.ikasan.spec.security.model.User;
import org.ikasan.spec.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;
import java.util.List;

@Route(value = "Search", layout = IkasanAppLayout.class)
@UIScope
@Component
@PageTitle("Ikasan - Search")
@PermitAll
@PreserveOnRefresh
public class SearchView extends VerticalLayout implements BeforeEnterObserver, SearchListener
{
    Logger logger = LoggerFactory.getLogger(SearchView.class);

    @Autowired
    @Qualifier("esbSearchService")
    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;

    @Autowired
    private HospitalAuditService hospitalAuditService;

    @Autowired
    private ResubmissionService resubmissionRestService;

    @Autowired
    private ReplayService replayRestService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Autowired
    private BatchInsert replayAuditService;

    @Autowired
    private UserService userService;

    @Autowired
    private DateFormatter dateFormatter;

    @Value("${max.download.bytes:50000000}")
    private int maxDownloadBytes;

    private boolean initialised = false;

    private SearchForm searchForm;
    private SearchResults searchResults;

    /**
     * Constructor
     */
    public SearchView()
    {
        this.setMargin(false);
        this.setSizeFull();
    }

    /**
     * Create the search form that appears at the top of the screen.
     */
    protected void createSearchForm()
    {
        this.searchForm = new SearchForm();
        this.searchForm.addSearchListener(this);
    }

    /**
     * Create the results grid layout.
     */
    protected void createSearchResults() {
        this.searchResults = new SearchResults(esbSearchService, hospitalAuditService, resubmissionRestService, replayRestService,
            moduleMetadataService, replayAuditService, this.dateFormatter, this.maxDownloadBytes);
        this.searchResults.setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SEARCH_ADMIN, SecurityConstants.SEARCH_READ,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage();
        }

        if(!initialised)
        {
            this.createSearchForm();
            this.createSearchResults();
            this.add(this.searchForm, this.searchResults);

            this.initialised = true;

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

    @Override
    public void search(String searchTerm, List<String> entityTypes, boolean negateQuery, long startDate, long endDate) {
        this.searchResults.search(startDate, endDate, searchTerm, entityTypes, negateQuery
            , List.of(), List.of());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
    }
}

