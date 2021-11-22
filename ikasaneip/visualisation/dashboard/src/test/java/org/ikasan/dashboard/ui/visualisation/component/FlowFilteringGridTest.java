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

import static org.mockito.ArgumentMatchers.argThat;

public class FlowFilteringGridTest extends UITest {

    @MockBean
    private ModuleMetadataSearchResults moduleMetadataSearchResults;

    @MockBean
    private FlowSearchFilter searchFilter;

    private ModuleType moduleType = ModuleType.INTEGRATION_MODULE;

    public void setup_expectations() {
    }

    @Test
    public void test_search_admin_user_search()
    {
        Mockito.when(super.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);

        Mockito.when(this.moduleMetadataService.find(argThat(strings -> strings.size() == 0)
            , Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(this.moduleMetadataSearchResults);
        Mockito.when(this.moduleMetadataSearchResults.getResultList())
            .thenReturn(this.getModuleMetaData(50));
        Mockito.when(this.moduleMetadataSearchResults.getTotalNumberOfResults())
            .thenReturn(25L);
        Mockito.when(this.moduleMetadataSearchResults.getQueryResponseTime())
            .thenReturn(100L);

        FlowFilteringGrid flowFilteringGrid = new FlowFilteringGrid(moduleMetadataService,
        searchFilter, moduleType);
        flowFilteringGrid.init();

        List<Flow> moduleMetaData = GridKt._findAll(flowFilteringGrid);

        Assert.assertEquals(100, moduleMetaData.size());
    }

    private List<ModuleMetaData> getModuleMetaData(int num) {
        List<ModuleMetaData> moduleMetaDataList = new ArrayList<>();

        IntStream.range(0, num).forEach(i -> {
            ModuleMetaDataImpl moduleMetaData = new ModuleMetaDataImpl();
            moduleMetaData.setName("moduleName"+i);
            moduleMetaData.setUrl("url"+i);
            moduleMetaData.setDescription("description"+i);
            moduleMetaData.setVersion("version"+i);

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
