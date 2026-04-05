package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
import org.ikasan.job.orchestration.broadcast.ContextViewUpdateEventBroadcastListener;
import org.ikasan.job.orchestration.broadcast.ContextViewUpdateEventBroadcaster;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class ContextTemplateViewMenuBar extends MenuBar implements ContextViewUpdateEventBroadcastListener {
    private ContextTemplate contextTemplate;
    private ContextProfileService contextProfileService;
    private SchedulerVisualisation schedulerVisualisation;
    private UI ui;

    public ContextTemplateViewMenuBar(ContextTemplate contextTemplate, ContextProfileService contextProfileService, SchedulerVisualisation schedulerVisualisation) {
        this.contextTemplate = contextTemplate;
        this.contextProfileService = contextProfileService;
        this.schedulerVisualisation = schedulerVisualisation;

        this.init();
    }

    private void init() {
        super.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem contextViews = createMenuItem(this, VaadinIcon.SITEMAP.create(), getTranslation("label.context-views", UI.getCurrent().getLocale()));

        SubMenu systemViewsSubMenu = contextViews.getSubMenu();
        SubMenu myViewsSubMenu = contextViews.getSubMenu();
        SubMenu sharedViewsSubMenu = contextViews.getSubMenu();

        MenuItem systemViews = systemViewsSubMenu.addItem(getTranslation("menu-item.system-views", UI.getCurrent().getLocale()));
        ContextProfileSearchFilter searchFilter = new SolrContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextTemplate.getName());
        searchFilter.setOwner(ContextProfileRecord.SYSTEM_OWNER);

        this.addContextViewsSubmenu(systemViews, searchFilter);

        MenuItem myViews = myViewsSubMenu.addItem(getTranslation("menu-item.my-views", UI.getCurrent().getLocale()));
        searchFilter = new SolrContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextTemplate.getName());
        searchFilter.setOwner(SecurityContextHolder.getContext().getAuthentication().getName());

        this.addContextViewsSubmenu(myViews, searchFilter);

        MenuItem sharedViews = sharedViewsSubMenu.addItem(getTranslation("menu-item.shared-views", UI.getCurrent().getLocale()));
        searchFilter = new SolrContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextTemplate.getName());
        searchFilter.setUser(SecurityContextHolder.getContext().getAuthentication().getName());

        this.addContextViewsSubmenu(sharedViews, searchFilter);;
    }

    private void addContextViewsSubmenu(MenuItem menuItem, ContextProfileSearchFilter searchFilter) {
        SubMenu subMenu = menuItem.getSubMenu();

        SearchResults<ContextProfileRecord> contextProfileRecords = this.contextProfileService
            .findByFilter(searchFilter, -1, -1, null, null);

        contextProfileRecords.getResultList().forEach(contextProfileRecord -> {
            contextProfileRecord.getContextProfile().getSubContexts().forEach(s -> {
                subMenu.addItem(s, menuItemClickEvent -> {
                    try {
                        if (this.contextTemplate.getName().equals(s)) {
                            this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate
                                , this.contextTemplate, null, true);
                        }
                        else {
                            ContextTemplate childContextTemplate = ContextHelper.getChildContextTemplate(s,
                                this.contextTemplate);
                            this.schedulerVisualisation.createSchedulerVisualisation(this.contextTemplate
                                , childContextTemplate, null, true);
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    private com.vaadin.flow.component.contextmenu.MenuItem createMenuItem(com.vaadin.flow.component.menubar.MenuBar menu, Icon icon, String label) {
        com.vaadin.flow.component.button.Button menuButton = new Button(label, icon);
        menuButton.setIconAfterText(true);

        MenuItem item = menu.addItem(menuButton);

        return item;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        ContextViewUpdateEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        ContextViewUpdateEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(String message) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                this.removeAll();
                this.init();
            });
        }
    }
}
