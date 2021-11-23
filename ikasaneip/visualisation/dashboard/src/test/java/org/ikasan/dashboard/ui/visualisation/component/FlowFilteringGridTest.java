package org.ikasan.dashboard.ui.visualisation.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.dashboard.ui.visualisation.model.designer.business.stream.Flow;
import org.ikasan.module.metadata.model.SolrFlowMetaDataImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class FlowFilteringGridTest extends UITest {

    @MockBean
    private ModuleMetadataSearchResults moduleMetadataSearchResults;

    @MockBean
    private FlowSearchFilter searchFilter;


    public void setup_expectations() {
    }

    @Test
    public void test_search_admin_user_search_integration_module()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
        searchFilter, ModuleType.INTEGRATION_MODULE);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(50, moduleMetaData.size());
    }

    @Test
    public void test_search_admin_user_search_integration_modulenone_found()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.moduleMetadataService.findAll())
            .thenReturn(getModuleMetaData(25, ModuleType.INTEGRATION_MODULE));

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
