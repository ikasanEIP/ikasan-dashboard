package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.beans.DashboardComponentFactory;
import org.ikasan.dashboard.ui.general.component.SessionDetailsDialog;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.security.view.LoginView;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Route(value = "sessionsView", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Administration Session Management")
@PermitAll
@PreserveOnRefresh
@Component
public class DashboardSessionsView extends VerticalLayout implements BeforeEnterObserver
{
    private Logger logger = LoggerFactory.getLogger(DashboardSessionsView.class);

    private DataProvider<VaadinSession, SessionFilter> dataProvider;
    private ConfigurableFilterDataProvider<VaadinSession, Void, SessionFilter> filteredDataProvider;

    private SessionFilter searchFilter;
    private Grid<VaadinSession> sessionGrid;
    private IkasanAuthentication ikasanAuthentication;

    /**
     * Constructor
     */
    public DashboardSessionsView()
    {
        super();
    }

    protected void init()
    {
        this.ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.sessionGrid = new Grid<>();
        this.sessionGrid.setSizeFull();
        this.setSizeFull();

        this.searchFilter = new SessionFilter();

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();

        headerLayout.add(new VerticalLayout(new H4(getTranslation("header.session-management", UI.getCurrent().getLocale()))));

        Button endAllSessionsButton = new Button(getTranslation("button.end-all-sessions", UI.getCurrent().getLocale()), VaadinIcon.EXCLAMATION.create());
        endAllSessionsButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("header.end-all-sessions", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("paragraph.end-all-sessions", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                DashboardComponentFactory.IkasanSessionListener.getActiveSessions().values().forEach(session -> {
                    if(!session.getSession().getAttribute(LoginView.USERNAME).equals(ikasanAuthentication.getName())) {
                        session.getSession().invalidate();
                    }
                });
                this.sessionGrid.getDataProvider().refreshAll();
            });
        });

        Button refreshButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(event -> this.sessionGrid.getDataProvider().refreshAll());

        VerticalLayout buttonWrapperLayout = new VerticalLayout();
        buttonWrapperLayout.setWidthFull();
        HorizontalLayout buttonLayout = new HorizontalLayout(endAllSessionsButton, refreshButton);
        buttonLayout.setHeightFull();
        buttonWrapperLayout.add(buttonLayout);
        buttonWrapperLayout.setHorizontalComponentAlignment(Alignment.END, buttonLayout);

        headerLayout.add(buttonWrapperLayout);

        this.add(headerLayout, sessionGrid);

        this.sessionGrid.addColumn(new ComponentRenderer<>(
                session -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    Button sessionButton = new Button(session.getSession().getId());
                    sessionButton.addClickListener(buttonClickEvent -> {
                        SessionDetailsDialog sessionDetailsDialog = new SessionDetailsDialog(session);
                        sessionDetailsDialog.open();
                    });

                    verticalLayout.add(sessionButton);

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.session-identifier", UI.getCurrent().getLocale()))
            .setKey("sessionIdentifier")
            .setSortable(true)
            .setFlexGrow(1);
        this.sessionGrid.addColumn(new ComponentRenderer<>(
                trigger -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(new Text((String) trigger.getSession()
                        .getAttribute(LoginView.USERNAME)));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.username", UI.getCurrent().getLocale()))
            .setKey("username")
            .setSortable(true)
            .setFlexGrow(1);
        this.sessionGrid.addColumn(new ComponentRenderer<>(
                session -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    AtomicLong size = new AtomicLong();
                    session.getSession().getAttributeNames().forEach(name -> {
                        try {
                            size.addAndGet(GraphLayout.parseInstance(session.getSession().getAttribute(name)).totalSize());
                        }
                        catch (Exception e) {
                            logger.debug("Could not calculate attribute size[{}] - message[{}]!", name, e.getMessage());
                        }
                    });

                    verticalLayout.add(new Text(String.valueOf(size.get())));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.session-size", UI.getCurrent().getLocale()))
            .setKey("sessionSize")
            .setSortable(true)
            .setFlexGrow(1);
        this.sessionGrid.addColumn(new ComponentRenderer<>(
                session -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(new Text(String.valueOf(session.getUIs().size())));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.number-of-uis", UI.getCurrent().getLocale()))
            .setKey("numberOfUIs")
            .setSortable(true)
            .setFlexGrow(1);
        this.sessionGrid.addColumn(new ComponentRenderer<>(
                session -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(new Text(DateFormatter.instance()
                        .getFormattedDate(session.getSession().getCreationTime())));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.session-creation-time", UI.getCurrent().getLocale()))
            .setKey("sessionCreateTime")
            .setSortable(true)
            .setFlexGrow(1);
        this.sessionGrid.addColumn(new ComponentRenderer<>(
                session -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(new Text(DateFormatter.instance()
                        .getFormattedDate(session.getSession().getLastAccessedTime())));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.session-last-accessed-time", UI.getCurrent().getLocale()))
            .setKey("lastAccessedTime")
            .setSortable(true)
            .setFlexGrow(1);
        this.sessionGrid.addColumn(new ComponentRenderer<>(
                session -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    Button endSessionButton = new Button(getTranslation("button.end-session"));
                    endSessionButton.addClickListener(event -> {
                        ConfirmDialog confirmDialog = new ConfirmDialog();
                        confirmDialog.setHeader(getTranslation("header.end-session", UI.getCurrent().getLocale()));
                        confirmDialog.setText(getTranslation("paragraph.end-session", UI.getCurrent().getLocale()));
                        confirmDialog.setCancelable(true);
                        confirmDialog.setConfirmText(getTranslation("button.ok"));
                        confirmDialog.setCancelText(getTranslation("button.cancel"));
                        confirmDialog.open();

                        confirmDialog.addConfirmListener(confirmEvent -> {
                            session.getSession().invalidate();
                            this.sessionGrid.getDataProvider().refreshAll();
                        });
                    });
                    verticalLayout.add(endSessionButton);

                    return verticalLayout;
                }))
            .setKey("endSession")
            .setSortable(false)
            .setFlexGrow(1);

        this.populateGrid();
    }

    private void populateGrid() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<SessionFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<VaadinSession> results;

            if(query.getSortOrders().size() > 0) {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted(),
                    query.getSortOrders().get(0).getDirection().name());
            }
            else {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.stream();
        }, query -> {
            Optional<SessionFilter> filter = query.getFilter();

            List<VaadinSession> results = this.getResults(filter.get(), -1, -1, null, null);

            return results.size();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.sessionGrid.setDataProvider(filteredDataProvider);

        HeaderRow hr = this.sessionGrid.appendHeaderRow();
        this.addGridFiltering(hr, this.searchFilter::setJobName, "sessionIdentifier");
        this.addGridFiltering(hr, this.searchFilter::setJobType, "username");
    }

    private void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey) {
        TextField textField = new TextField();
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{
            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(this.sessionGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    private List<VaadinSession> getResults(SessionFilter sessionFilter, int offset, int limit, String sortColumn, String sortOrder) {
        List<VaadinSession> items = new ArrayList<>();

        try {
            items.addAll(DashboardComponentFactory.IkasanSessionListener
                .getActiveSessions().values().stream().filter(s -> s.getSession() != null)
                .collect(Collectors.toList()));
            items = items.stream()
                .filter(item -> item.getSession().getAttribute(LoginView.USERNAME) != null)
                .collect(Collectors.toList());

            if(sessionFilter != null) {
                if(sessionFilter.getSessionIdentifier() != null && !sessionFilter.getSessionIdentifier().isEmpty()) {
                    items = items.stream()
                        .filter(item -> item.getSession().getId().toLowerCase().contains(sessionFilter.getSessionIdentifier().toLowerCase()))
                        .collect(Collectors.toList());
                }

                if(sessionFilter.getUsername() != null && !sessionFilter.getUsername().isEmpty()) {
                    items = items.stream()
                        .filter(item -> ((String)item.getSession().getAttribute(LoginView.USERNAME))
                            .toLowerCase().contains(sessionFilter.getUsername().toLowerCase()))
                        .collect(Collectors.toList());
                }
            }

            if(sortColumn != null && sortOrder != null) {
                items = items.stream().sorted((o1, o2) -> {
                    if (sortOrder.equals("ASCENDING")) {
                        if(sortColumn.equals("sessionIdentifier")) {
                            return o1.getSession().getId().toLowerCase().compareTo(o2.getSession().getId().toLowerCase());
                        }
                        else if(sortColumn.equals("username")) {
                            return ((String)o1.getSession().getAttribute(LoginView.USERNAME)).toLowerCase()
                                .compareTo(((String)o2.getSession().getAttribute(LoginView.USERNAME)).toLowerCase());
                        }
                        else if(sortColumn.equals("sessionCreateTime")) {
                             return o1.getSession().getCreationTime() < o2.getSession().getCreationTime() ? 1 : -1;
                        }
                        else if(sortColumn.equals("lastAccessTime")) {
                            return o1.getSession().getLastAccessedTime() < o2.getSession().getLastAccessedTime() ? 1 : -1;
                        }
                        else if(sortColumn.equals("sessionSize")) {
                            AtomicLong o1Size = new AtomicLong(0L);
                            o1.getSession().getAttributeNames().forEach(name -> {
                                try {
                                    o1Size.addAndGet(GraphLayout.parseInstance(o1.getSession().getAttribute(name)).totalSize());
                                }
                                catch (Exception e) {
                                    logger.debug("Could not calculate attribute size[{}] - message[{}]!", name, e.getMessage());
                                }
                            });
                            AtomicLong o2Size = new AtomicLong(0L);
                            o2.getSession().getAttributeNames().forEach(name -> {
                                try {
                                    o2Size.addAndGet(GraphLayout.parseInstance(o2.getSession().getAttribute(name)).totalSize());
                                }
                                catch (Exception e) {
                                    logger.debug("Could not calculate attribute size[{}] - message[{}]!", name, e.getMessage());
                                }
                            });
                            return o1Size.get() < o2Size.get() ? 1 : -1;
                        }
                        else if(sortColumn.equals("numberOfUIs")) {
                            return o1.getUIs().size() < o2.getUIs().size() ? 1 : -1;
                        }
                    }
                    else if (sortOrder.equals("DESCENDING")) {
                        if(sortColumn.equals("sessionIdentifier")) {
                            return o2.getSession().getId().toLowerCase().compareTo(o1.getSession().getId().toLowerCase());
                        }
                        else if(sortColumn.equals("username")) {
                            return ((String)o2.getSession().getAttribute(LoginView.USERNAME)).toLowerCase()
                                .compareTo(((String)o1.getSession().getAttribute(LoginView.USERNAME)).toLowerCase());
                        }
                        else if(sortColumn.equals("sessionCreateTime")) {
                            return o2.getSession().getCreationTime() < o1.getSession().getCreationTime() ? 1 : -1;
                        }
                        else if(sortColumn.equals("lastAccessTime")) {
                            return o2.getSession().getLastAccessedTime() < o1.getSession().getLastAccessedTime() ? 1 : -1;
                        }
                        else if(sortColumn.equals("sessionSize")) {
                            AtomicLong o1Size = new AtomicLong(0L);
                            o1.getSession().getAttributeNames().forEach(name -> {
                                try {
                                    o1Size.addAndGet(GraphLayout.parseInstance(o1.getSession().getAttribute(name)).totalSize());
                                }
                                catch (Exception e) {
                                    logger.debug("Could not calculate attribute size[{}] - message[{}]!", name, e.getMessage());
                                }
                            });
                            AtomicLong o2Size = new AtomicLong(0L);
                            o2.getSession().getAttributeNames().forEach(name -> {
                                try {
                                    o2Size.addAndGet(GraphLayout.parseInstance(o2.getSession().getAttribute(name)).totalSize());
                                }
                                catch (Exception e) {
                                    logger.debug("Could not calculate attribute size[{}] - message[{}]!", name, e.getMessage());
                                }
                            });
                            return o2Size.get() < o1Size.get() ? 1 : -1;
                        }
                        else if(sortColumn.equals("numberOfUIs")) {
                            return o2.getUIs().size() < o1.getUIs().size() ? 1 : -1;
                        }
                    }

                    return 0;
                }).collect(Collectors.toList());
            }

            if(offset >= 0 && limit > 0 && offset + limit >= items.size()) {
                items = items.subList(offset, items.size());
            }
            else if(offset >= 0 && limit > 0 && offset + limit < items.size()) {
                items = items.subList(offset, offset + limit);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        return items;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SCHEDULER_READ, SecurityConstants.SCHEDULER_WRITE,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_ALL_WRITE,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_DEV_READ, SecurityConstants.SCHEDULER_DEV_WRITE,
            SecurityConstants.SCHEDULER_DEV_ADMIN, SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage();
            return;
        }

        if(this.sessionGrid == null) {
            init();
        }
    }

    private class SessionFilter {
        private String jobName;
        private String jobType;

        public String getSessionIdentifier() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public String getUsername() {
            return jobType;
        }

        public void setJobType(String jobType) {
            this.jobType = jobType;
        }
    }
}
