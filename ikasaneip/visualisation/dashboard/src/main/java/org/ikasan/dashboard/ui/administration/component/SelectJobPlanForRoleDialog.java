package org.ikasan.dashboard.ui.administration.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.administration.filter.ContextTemplateFilter;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.FilteringGrid;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.RoleJobPlan;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SelectJobPlanForRoleDialog extends AbstractCloseableResizableDialog
{
    private Role role;
    private final ScheduledContextService scheduledContextService;
    private final SecurityService securityService;
    private final SystemEventLogger systemEventLogger;
    private List<ContextTemplate> contextTemplates;
    private final FilteringGrid<RoleJobPlan> roleModuleFilteringGrid;

    public SelectJobPlanForRoleDialog(Role role, ScheduledContextService scheduledContextService, SecurityService securityService
        , SystemEventLogger systemEventLogger, FilteringGrid<RoleJobPlan> roleJobPlanFilteringGrid)
    {
        this.role = role;
        if(this.role == null)
        {
            throw new IllegalArgumentException("role cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null)
        {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.securityService = securityService;
        if(this.securityService == null)
        {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null)
        {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.roleModuleFilteringGrid = roleJobPlanFilteringGrid;
        if(this.roleModuleFilteringGrid == null)
        {
            throw new IllegalArgumentException("roleModuleFilteringGrid cannot be null!");
        }

        init();
    }

    private void init()
    {
        super.title.setText(getTranslation("label.select-module", UI.getCurrent().getLocale()));
        H3 selectUserLabel = new H3(getTranslation("label.select-module", UI.getCurrent().getLocale()));

        contextTemplates = this.scheduledContextService.findAll()
            .getResultList()
            .stream()
            .map(record -> record.getContext())
            .collect(Collectors.toList());

        contextTemplates = this.removeAlreadyAssociatedModules(contextTemplates);

        ContextTemplateFilter contextTemplateFilter = new ContextTemplateFilter();

        FilteringGrid<ContextTemplate> contextTemplateFilteringGrid = new FilteringGrid<>(contextTemplateFilter);
        contextTemplateFilteringGrid.setSizeFull();

        contextTemplateFilteringGrid.addColumn(ContextTemplate::getName)
            .setKey("name")
            .setHeader(getTranslation("table-header.moduleName", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(2);

        contextTemplateFilteringGrid.addColumn(ContextTemplate::getDescription)
            .setKey("description")
            .setHeader(getTranslation("table-header.description", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(2);

        contextTemplateFilteringGrid.addItemDoubleClickListener(event ->
        {
            RoleJobPlan roleModule = this.securityService.createRoleJobPlan();
            roleModule.setRole(this.role);
            roleModule.setJobPlanName(event.getItem().getName());
            this.securityService.saveRoleJobPlan(roleModule);

            this.role = securityService.getRoleById(role.getId());
            role.addRoleJobPlan(roleModule);
            this.securityService.saveRole(this.role);


            String action = String.format("Module [%s] added to role [%s].", event.getItem().getName(), role.getName());

            this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_MODULE_ROLE_CHANGE_CONSTANTS, action, null);

            contextTemplateFilteringGrid.setItems(removeAlreadyAssociatedModules(contextTemplates));
            contextTemplateFilteringGrid.getDataProvider().refreshAll();

            Collection<RoleJobPlan> items = this.roleModuleFilteringGrid.getItems();
            items.add(roleModule);

            this.roleModuleFilteringGrid.setItems(items);
            this.roleModuleFilteringGrid.getDataProvider().refreshAll();
        });

        HeaderRow hr = contextTemplateFilteringGrid.appendHeaderRow();
        contextTemplateFilteringGrid.addGridFiltering(hr, contextTemplateFilter::setModuleNameFilter, "name");

        contextTemplateFilteringGrid.setItems(contextTemplates);

        contextTemplateFilteringGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(selectUserLabel, contextTemplateFilteringGrid);

        super.content.add(layout);
        super.setWidth("1200px");
        super.setHeight("700px");
    }

    protected List<ContextTemplate> removeAlreadyAssociatedModules(List<ContextTemplate> scheduledContextList)
    {
        List<String> modules = role.getRoleJobPlans().stream()
            .map(roleModule -> roleModule.getJobPlanName()).collect(Collectors.toList());

        return scheduledContextList.stream()
            .filter(contextTemplate -> !modules.contains(contextTemplate.getName()))
            .collect(Collectors.toList());
    }
}
