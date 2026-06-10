package org.ikasan.dashboard.ui.visualisation.layout;

import com.vaadin.frontendtools.internal.commons.io.IOUtils;
import org.ikasan.dashboard.ui.visualisation.adapter.service.ModuleDraw2DAdapter;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.topology.metadata.JsonFlowMetaDataProvider;
import org.ikasan.topology.metadata.JsonModuleMetaDataProvider;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@Ignore
public class IkasanFlowLayoutManagerTest
{
    public static final String MODULE_JSON = "/data/graph/module.json";
    public static final String MODULE_FOUR_JSON = "/data/graph/module-four.json";

    JsonModuleMetaDataProvider jsonModuleMetaDataProvider
        = new JsonModuleMetaDataProvider(new JsonFlowMetaDataProvider());

    @Test
    public void test_simple_module() throws IOException
    {
        ModuleMetaData moduleMetaData = this.jsonModuleMetaDataProvider
            .deserialiseModule(loadDataFile(MODULE_JSON));

        ModuleDraw2DAdapter moduleVisjsAdapter = new ModuleDraw2DAdapter();

        org.ikasan.dashboard.ui.visualisation.model.flow.Module module
            = moduleVisjsAdapter.adapt(moduleMetaData, new ArrayList<>());

        IkasanFlowLayoutManager layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(0));

        layoutManager.layout();

        Assertions.assertEquals(2100, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(300, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(8, layoutManager.nodeList.size(), "node list size equals!");
    }

    @Test
    public void test_complex_module() throws IOException
    {
        ModuleMetaData moduleMetaData = this.jsonModuleMetaDataProvider
            .deserialiseModule(loadDataFile(MODULE_FOUR_JSON));

        ModuleDraw2DAdapter moduleVisjsAdapter = new ModuleDraw2DAdapter();

        org.ikasan.dashboard.ui.visualisation.model.flow.Module module
            = moduleVisjsAdapter.adapt(moduleMetaData, new ArrayList<>());

        IkasanFlowLayoutManager layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(0));

        layoutManager.layout();

        Assertions.assertEquals(2400, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(1500, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(18, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(1));

        layoutManager.layout();

        Assertions.assertEquals(3900, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(3900, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(67, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(2));

        layoutManager.layout();

        Assertions.assertEquals(1800, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(300, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(7, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(3));

        layoutManager.layout();

        Assertions.assertEquals(2100, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(300, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(8, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(4));

        layoutManager.layout();

        Assertions.assertEquals(2700, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(700, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(14, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(5));

        layoutManager.layout();

        Assertions.assertEquals(2400, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(500, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(11, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(6));

        layoutManager.layout();

        Assertions.assertEquals(900, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(300, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(4, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(7));

        layoutManager.layout();

        Assertions.assertEquals(2100, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(300, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(8, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(8));

        layoutManager.layout();

        Assertions.assertEquals(2700, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(700, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(14, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(9));

        layoutManager.layout();

        Assertions.assertEquals(2400, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(500, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(11, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(10));

        layoutManager.layout();

        Assertions.assertEquals(1200, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(500, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(6, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(11));

        layoutManager.layout();

        Assertions.assertEquals(4200, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(2700, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(46, layoutManager.nodeList.size(), "node list size equals!");

        layoutManager = new IkasanFlowLayoutManager(module.getFlows().get(12));

        layoutManager.layout();

        Assertions.assertEquals(3900, layoutManager.xExtent, "X extent equals!");
        Assertions.assertEquals(700, layoutManager.yExtent, "Y extent equals!");
        Assertions.assertEquals(19, layoutManager.nodeList.size(), "node list size equals!");
    }

    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}
