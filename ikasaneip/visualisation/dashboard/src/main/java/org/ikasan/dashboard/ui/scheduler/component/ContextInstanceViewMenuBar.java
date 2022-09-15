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
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.scheduler.util.ContextViewUpdateEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerInstanceVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class ContextInstanceViewMenuBar extends MenuBar {

    private Registration contextInstanceViewUpdateRegistration;
    private Registration contextInstanceStateChangeRegistration;

    private ContextInstance contextInstance;
    private ContextProfileService contextProfileService;
    private SchedulerInstanceVisualisation schedulerInstanceVisualisation;

    public ContextInstanceViewMenuBar(ContextInstance contextInstance, ContextProfileService contextProfileService, SchedulerInstanceVisualisation schedulerInstanceVisualisation) {
        this.contextInstance = contextInstance;
        this.contextProfileService = contextProfileService;
        this.schedulerInstanceVisualisation = schedulerInstanceVisualisation;

        this.init();
    }

    private void init() {
        super.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem contextViews = createQuickAccessButton(this, VaadinIcon.SITEMAP.create(), getTranslation("label.context-views", UI.getCurrent().getLocale()));

        SubMenu systemViewsSubMenu = contextViews.getSubMenu();
        SubMenu myViewsSubMenu = contextViews.getSubMenu();
        SubMenu sharedViewsSubMenu = contextViews.getSubMenu();

        MenuItem systemViews = systemViewsSubMenu.addItem(getTranslation("menu-item.system-views", UI.getCurrent().getLocale()));
        ContextProfileSearchFilter searchFilter = new SolrContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextInstance.getName());
        searchFilter.setOwner(ContextProfileRecord.SYSTEM_OWNER);

        this.addContextViewsSubmenu(systemViews, searchFilter);

        MenuItem myViews = myViewsSubMenu.addItem(getTranslation("menu-item.my-views", UI.getCurrent().getLocale()));
        searchFilter = new SolrContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextInstance.getName());
        searchFilter.setOwner(SecurityContextHolder.getContext().getAuthentication().getName());

        this.addMyContextViewsSubmenu(myViews, searchFilter);

        MenuItem sharedViews = sharedViewsSubMenu.addItem(getTranslation("menu-item.shared-views", UI.getCurrent().getLocale()));
        searchFilter = new SolrContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextInstance.getName());
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
                        if(ContextMachineCache.instance().containsInstanceIdentifier(contextInstance.getId())) {
                            this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(contextInstance.getId()).getContext();
                        }

                        if (this.contextInstance.getName().equals(s)) {
                            this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance
                                , this.contextInstance, null);
                        }
                        else {
                            ContextInstance childContextInstance = ContextHelper.getChildContextInstance(s,
                                this.contextInstance);
                            this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance
                                , childContextInstance, null);
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    private void addMyContextViewsSubmenu(MenuItem menuItem, ContextProfileSearchFilter searchFilter) {
        SubMenu subMenu = menuItem.getSubMenu();

        SearchResults<ContextProfileRecord> contextProfileRecords = this.contextProfileService
            .findByFilter(searchFilter, -1, -1, null, null);

        contextProfileRecords.getResultList().forEach(contextProfileRecord -> {

            MenuItem myMenuItem = subMenu.addItem(contextProfileRecord.getProfileName());
            SubMenu myMenuItemSubMenuItem = myMenuItem.getSubMenu();

            contextProfileRecord.getContextProfile().getSubContexts().forEach(s -> {
                myMenuItemSubMenuItem.addItem(s, menuItemClickEvent -> {
                    try {
                        if(ContextMachineCache.instance().containsInstanceIdentifier(contextInstance.getId())) {
                            this.contextInstance = ContextMachineCache.instance().getByContextInstanceId(contextInstance.getId()).getContext();
                        }

                        if (this.contextInstance.getName().equals(s)) {
                            this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance
                                , this.contextInstance, null);
                        }
                        else {
                            ContextInstance childContextInstance = ContextHelper.getChildContextInstance(s,
                                this.contextInstance);
                            this.schedulerInstanceVisualisation.createSchedulerVisualisation(this.contextInstance
                                , childContextInstance, null);
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    private MenuItem createQuickAccessButton(MenuBar menu, Icon icon, String label) {
        Button quickAccessButton = new Button(label, icon);
        quickAccessButton.setIconAfterText(true);

        MenuItem item = menu.addItem(quickAccessButton);

        return item;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();

        contextInstanceViewUpdateRegistration = ContextViewUpdateEventBroadcaster.register(event -> ui.access(() -> {
            this.removeAll();
            this.init();
        }));

        contextInstanceStateChangeRegistration = ContextInstanceStateChangeEventBroadcaster.register(contextInstanceStateChangeEvent -> {
            if (contextInstanceStateChangeEvent.getContextInstance() != null) {
                if(this.contextInstance.getId().equals(contextInstanceStateChangeEvent.getContextInstance().getId())) {
                    this.contextInstance = contextInstanceStateChangeEvent.getContextInstance();
                }
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.contextInstanceViewUpdateRegistration != null) {
            this.contextInstanceViewUpdateRegistration.remove();
            this.contextInstanceViewUpdateRegistration = null;
        }

        if(this.contextInstanceStateChangeRegistration != null) {
            this.contextInstanceStateChangeRegistration.remove();
            this.contextInstanceStateChangeRegistration = null;
        }
    }
}
