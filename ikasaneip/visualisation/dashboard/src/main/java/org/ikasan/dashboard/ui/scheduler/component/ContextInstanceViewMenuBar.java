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
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerInstanceVisualisation;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.profile.ContextProfileSearchFilterImpl;
import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventLocalBroadcastListener;
import org.ikasan.job.orchestration.broadcast.ContextViewUpdateEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class ContextInstanceViewMenuBar extends MenuBar implements ContextInstanceStateChangeEventLocalBroadcastListener
    , ContextViewUpdateEventLocalBroadcastListener {

    private ContextInstance contextInstance;
    private ContextProfileService contextProfileService;
    private SchedulerInstanceVisualisation schedulerInstanceVisualisation;
    private UI ui;

    public ContextInstanceViewMenuBar(ContextInstance contextInstance, ContextProfileService contextProfileService, SchedulerInstanceVisualisation schedulerInstanceVisualisation) {
        this.contextInstance = contextInstance;
        this.contextProfileService = contextProfileService;
        this.schedulerInstanceVisualisation = schedulerInstanceVisualisation;

        this.init();
    }

    private void init() {
        super.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem contextViews = creatContextViewsButton(this, VaadinIcon.SITEMAP.create(), getTranslation("label.context-views", UI.getCurrent().getLocale()));

        SubMenu systemViewsSubMenu = contextViews.getSubMenu();
        SubMenu myViewsSubMenu = contextViews.getSubMenu();
        SubMenu sharedViewsSubMenu = contextViews.getSubMenu();

        MenuItem systemViews = systemViewsSubMenu.addItem(getTranslation("menu-item.system-views", UI.getCurrent().getLocale()));
        ContextProfileSearchFilter searchFilter = new ContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextInstance.getName());
        searchFilter.setOwner(ContextProfileRecord.SYSTEM_OWNER);

        this.addContextViewsSubmenu(systemViews, searchFilter);

        MenuItem myViews = myViewsSubMenu.addItem(getTranslation("menu-item.my-views", UI.getCurrent().getLocale()));
        searchFilter = new ContextProfileSearchFilterImpl();
        searchFilter.setContextName(this.contextInstance.getName());
        searchFilter.setOwner(SecurityContextHolder.getContext().getAuthentication().getName());

        this.addMyContextViewsSubmenu(myViews, searchFilter);

        MenuItem sharedViews = sharedViewsSubMenu.addItem(getTranslation("menu-item.shared-views", UI.getCurrent().getLocale()));
        searchFilter = new ContextProfileSearchFilterImpl();
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
                        ContextMachine machine = ContextMachineCache.instance().getByContextInstanceId(contextInstance.getId());
                        if(machine != null) {
                            ContextInstance refreshed = machine.getContext();
                            if(refreshed != null) this.contextInstance = refreshed;
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
                        ContextMachine machine = ContextMachineCache.instance().getByContextInstanceId(contextInstance.getId());
                        if(machine != null) {
                            ContextInstance refreshed = machine.getContext();
                            if(refreshed != null) this.contextInstance = refreshed;
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

    private MenuItem creatContextViewsButton(MenuBar menu, Icon icon, String label) {
        Button quickAccessButton = new Button(label, icon);
        quickAccessButton.setIconAfterText(true);

        MenuItem item = menu.addItem(quickAccessButton);

        return item;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        ContextInstanceStateChangeEventBroadcaster.instance().register(this);
        ContextViewUpdateEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        ContextInstanceStateChangeEventBroadcaster.instance().unregister(this);
        ContextViewUpdateEventBroadcaster.instance().unregister(this);
    }



    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if (event.getContextInstance() != null) {
            if(this.contextInstance.getId().equals(event.getContextInstance().getId())) {
                this.contextInstance = event.getContextInstance();
            }
        }
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
