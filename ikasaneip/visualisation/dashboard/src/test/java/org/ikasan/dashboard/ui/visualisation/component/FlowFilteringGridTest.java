package org.ikasan.dashboard.ui.visualisation.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.dashboard.ui.visualisation.model.designer.business.stream.Flow;
import org.ikasan.module.metadata.model.SolrFlowMetaDataImpl;
import org.ikasan.security.model.IkasanPrincipal;
import org.ikasan.security.model.Role;
import org.ikasan.security.model.RoleModule;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;

public class FlowFilteringGridTest extends UITest {

    @MockBean
    private FlowSearchFilter searchFilter;

    @MockBean
    private Set<IkasanPrincipal> principals;

    @MockBean
    private IkasanPrincipal principal;

    @MockBean
    private Set<Role> roles;

    @MockBean
    private Role role;

    @MockBean
    private Set<RoleModule> roleModules;

    @MockBean
    private RoleModule roleModule;


    public void setup_expectations() {
    }

    @Test
    public void test_search_admin_user_search_integration_module()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("");

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
        searchFilter, ModuleType.INTEGRATION_MODULE);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(50, moduleMetaData.size());
    }

    @Test
    public void test_search_non_admin_user_search_integration_module_no_accessible_modules()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(false);

        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(false);

        Mockito.when(super.ikasanAuthentication.getPrincipal())
            .thenReturn(super.user);
        Mockito.when(super.user.getPrincipals())
            .thenReturn(this.principals);
        Mockito.doCallRealMethod().when(this.principals).forEach((any(Consumer.class)));
        Mockito.when(this.principals.iterator()).thenReturn(Set.of(principal).iterator(), Set.of(principal).iterator());
        Mockito.when(principal.getRoles()).thenReturn(this.roles);
        Mockito.doCallRealMethod().when(this.roles).forEach((any(Consumer.class)));
        Mockito.when(this.roles.iterator()).thenReturn(Set.of(role).iterator(), Set.of(role).iterator());
        Mockito.when(role.getRoleModules()).thenReturn(this.roleModules);
        Mockito.doCallRealMethod().when(this.roleModules).forEach((any(Consumer.class)));
        Mockito.when(this.roleModules.iterator()).thenReturn(new HashSet<RoleModule>().iterator(), new HashSet<RoleModule>().iterator());

        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("");

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
            searchFilter, ModuleType.INTEGRATION_MODULE);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(0, moduleMetaData.size());
    }

    @Test
    public void test_search_non_admin_user_search_integration_module_with_accessible_modules()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(false);

        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(false);

        Mockito.when(super.ikasanAuthentication.getPrincipal())
            .thenReturn(super.user);
        Mockito.when(super.user.getPrincipals())
            .thenReturn(this.principals);
        Mockito.doCallRealMethod().when(this.principals).forEach((any(Consumer.class)));
        Mockito.when(this.principals.iterator()).thenReturn(Set.of(principal).iterator(), Set.of(principal).iterator());
        Mockito.when(principal.getRoles()).thenReturn(this.roles);
        Mockito.doCallRealMethod().when(this.roles).forEach((any(Consumer.class)));
        Mockito.when(this.roles.iterator()).thenReturn(Set.of(role).iterator(), Set.of(role).iterator());
        Mockito.when(role.getRoleModules()).thenReturn(this.roleModules);
        Mockito.doCallRealMethod().when(this.roleModules).forEach((any(Consumer.class)));
        Mockito.when(this.roleModules.iterator()).thenReturn(Set.of(roleModule).iterator(), Set.of(roleModule).iterator());

        Mockito.when(this.roleModule.getModuleName()).thenReturn("moduleName0");

        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("");

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
            searchFilter, ModuleType.INTEGRATION_MODULE);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(2, moduleMetaData.size());
    }

    @Test
    public void test_search_admin_user_search_integration_module_with_module_name_filtering()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("moduleName0");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("moduleName0");

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
            searchFilter, ModuleType.INTEGRATION_MODULE);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(2, moduleMetaData.size());
    }

    @Test
    public void test_search_admin_user_search_integration_module_with_flow_name_filtering()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("flowA0");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("flowA0");

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
            searchFilter, ModuleType.INTEGRATION_MODULE);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(1, moduleMetaData.size());
    }

    @Test
    public void test_search_admin_user_search_integration_module_none_found()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));
        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("");

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
            searchFilter, ModuleType.SCHEDULER_AGENT);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(0, moduleMetaData.size());
    }

    @Test
    public void test_search_admin_user_search_scheduler_agent()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("");

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.SCHEDULER_AGENT));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
            searchFilter, ModuleType.SCHEDULER_AGENT);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(50, moduleMetaData.size());
    }

    @Test
    public void test_search_admin_user_search_scheduler_agent_none_found()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(this.searchFilter.getFlowNameFilter())
            .thenReturn("");
        Mockito.when(this.searchFilter.getModuleNameFilter())
            .thenReturn("");

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
            searchFilter, ModuleType.SCHEDULER_AGENT);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(0, moduleMetaData.size());
    }

    private List<ModuleMetaData> getModuleMetaData(int num, ModuleType moduleType) {
        List<ModuleMetaData> moduleMetaDataList = new ArrayList<>();

        IntStream.range(0, num).forEach(i -> {
            ModuleMetaDataImpl moduleMetaData = new ModuleMetaDataImpl();
            moduleMetaData.setName("moduleName"+i);
            moduleMetaData.setUrl("url"+i);
            moduleMetaData.setDescription("description"+i);
            moduleMetaData.setVersion("version"+i);
            moduleMetaData.setType(moduleType);

            moduleMetaData.setFlows(List.of(solrFlowMetaDataImpl("flowA"+i), solrFlowMetaDataImpl("flowB"+i)));

            moduleMetaDataList.add(moduleMetaData);
        });

        return moduleMetaDataList;
    }


    private SolrFlowMetaDataImpl solrFlowMetaDataImpl(String name) {
        SolrFlowMetaDataImpl flowMetaData = new SolrFlowMetaDataImpl();
        flowMetaData.setName(name);

        return flowMetaData;
    }
}
