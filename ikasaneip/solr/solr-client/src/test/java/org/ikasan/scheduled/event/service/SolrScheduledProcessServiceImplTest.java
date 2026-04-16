package org.ikasan.scheduled.event.service;

import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDao;
import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.scheduled.event.dao.SolrScheduledProcessEventDao;
import org.ikasan.scheduled.event.model.SolrScheduledProcessEvent;
import org.ikasan.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.event.model.UpcomingScheduledProcess;
import org.ikasan.spec.metadata.model.*;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.solr.BatchInsertEvent;
import org.ikasan.spec.solr.BatchInsertListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class SolrScheduledProcessServiceImplTest {

    private SolrScheduledProcessEventDao scheduledProcessEventDao;
    private SolrModuleMetadataDao solrModuleMetadataDao;
    private SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao;
    private SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao;
    private SolrScheduledProcessServiceImpl service;

    @Before
    public void setup() {
        scheduledProcessEventDao = mock(SolrScheduledProcessEventDao.class);
        solrModuleMetadataDao = mock(SolrModuleMetadataDao.class);
        solrComponentConfigurationMetadataDao = mock(SolrComponentConfigurationMetadataDao.class);
        solrBusinessStreamMetadataDao = mock(SolrBusinessStreamMetadataDao.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_scheduledProcessEventDao() {
        new SolrScheduledProcessServiceImpl(null, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solrModuleMetadataDao() {
        new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, null,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solrComponentConfigurationMetadataDao() {
        new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            null, solrBusinessStreamMetadataDao, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solrBusinessStreamMetadataDao() {
        new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, null, false);
    }

    @Test
    public void test_constructor_successful() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        Assert.assertNotNull(service);
    }

    @Test
    public void test_save_single_event() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ScheduledProcessEvent event = new SolrScheduledProcessEvent();
        event.setAgentName("testAgent");
        event.setJobName("testJob");

        service.save(event);

        verify(scheduledProcessEventDao, times(1)).save(event);
        verify(scheduledProcessEventDao, times(1)).setSolrUsername(null);
        verify(scheduledProcessEventDao, times(1)).setSolrPassword(null);
    }

    @Test
    public void test_save_list_of_events() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        List<ScheduledProcessEvent> events = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            ScheduledProcessEvent event = new SolrScheduledProcessEvent();
            event.setAgentName("testAgent" + i);
            event.setJobName("testJob" + i);
            events.add(event);
        }

        service.save(events);

        verify(scheduledProcessEventDao, times(1)).save(events);
        verify(scheduledProcessEventDao, times(1)).setSolrUsername(null);
        verify(scheduledProcessEventDao, times(1)).setSolrPassword(null);
    }

    @Test
    public void test_insert_with_batch_listeners_disabled() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        BatchInsertListener<ScheduledProcessEvent> listener = mock(BatchInsertListener.class);
        service.addBatchInsertListener(listener);

        List<ScheduledProcessEvent> events = Arrays.asList(new SolrScheduledProcessEvent());
        service.insert(events);

        verify(scheduledProcessEventDao, times(1)).save(events);
        verify(listener, never()).onBatchInsert(any());
    }

    @Test
    public void test_insert_with_batch_listeners_enabled() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, true);

        BatchInsertListener<ScheduledProcessEvent> listener = mock(BatchInsertListener.class);
        service.addBatchInsertListener(listener);

        List<ScheduledProcessEvent> events = new ArrayList<>();
        ScheduledProcessEvent event = new SolrScheduledProcessEvent();
        event.setAgentName("testAgent");
        events.add(event);

        service.insert(events);

        verify(scheduledProcessEventDao, times(1)).save(events);

        ArgumentCaptor<BatchInsertEvent> captor = ArgumentCaptor.forClass(BatchInsertEvent.class);
        verify(listener, times(1)).onBatchInsert(captor.capture());

        Assert.assertEquals(events, captor.getValue().getEvents());
    }

    @Test
    public void test_add_and_remove_batch_insert_listener() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, true);

        BatchInsertListener<ScheduledProcessEvent> listener = mock(BatchInsertListener.class);
        service.addBatchInsertListener(listener);

        List<ScheduledProcessEvent> events = Arrays.asList(new SolrScheduledProcessEvent());
        service.insert(events);

        verify(listener, times(1)).onBatchInsert(any());

        // Remove listener
        service.removeBatchInsertListener(listener);

        // Insert again
        service.insert(events);

        // Listener should only be called once (before removal)
        verify(listener, times(1)).onBatchInsert(any());
    }

    @Test
    public void test_getAllAgentNames() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        List<String> agentNames = Arrays.asList("agent1", "agent2", "agent3");
        when(scheduledProcessEventDao.getAllAgentNames()).thenReturn(agentNames);

        List<String> result = service.getAllAgentNames();

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("agent1", result.get(0));
        verify(scheduledProcessEventDao, times(1)).getAllAgentNames();
    }

    @Test
    public void test_getFlowsForAgent() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ModuleMetaData moduleMetaData = mock(ModuleMetaData.class);
        List<FlowMetaData> flows = new ArrayList<>();
        FlowMetaData flow1 = mock(FlowMetaData.class);
        when(flow1.getName()).thenReturn("flow1");
        FlowMetaData flow2 = mock(FlowMetaData.class);
        when(flow2.getName()).thenReturn("flow2");
        flows.add(flow1);
        flows.add(flow2);

        when(moduleMetaData.getFlows()).thenReturn(flows);
        when(solrModuleMetadataDao.findById("agent1")).thenReturn(moduleMetaData);

        List<FlowMetaData> result = service.getFlowsForAgent("agent1");

        Assert.assertEquals(2, result.size());
        verify(solrModuleMetadataDao, times(1)).findById("agent1");
    }

    @Test
    public void test_getFlowsForAgent_with_offset_and_limit() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ModuleMetaData moduleMetaData = mock(ModuleMetaData.class);
        List<FlowMetaData> flows = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            FlowMetaData flow = mock(FlowMetaData.class);
            when(flow.getName()).thenReturn("flow" + i);
            flows.add(flow);
        }

        when(moduleMetaData.getFlows()).thenReturn(flows);
        when(solrModuleMetadataDao.findById("agent1")).thenReturn(moduleMetaData);

        List<FlowMetaData> result = service.getFlowsForAgent("agent1", 5, 3);

        Assert.assertEquals(3, result.size());
        verify(solrModuleMetadataDao, times(1)).findById("agent1");
    }

    @Test
    public void test_getScheduledProcessEvents_by_agent() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ScheduledProcessEventSearchResults<ScheduledProcessEvent> expectedResults =
            new ScheduledProcessEventSearchResults<>(new ArrayList<>(), 0, 0);

        when(scheduledProcessEventDao.getScheduleProcessEvents("agent1", 0L, 1000L))
            .thenReturn(expectedResults);

        ScheduledProcessEventSearchResults<ScheduledProcessEvent> result =
            service.getScheduledProcessEvents("agent1", 0L, 1000L);

        Assert.assertNotNull(result);
        verify(scheduledProcessEventDao, times(1)).getScheduleProcessEvents("agent1", 0L, 1000L);
    }

    @Test
    public void test_getScheduledProcessEvents_with_filters() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        List<String> modules = Arrays.asList("module1", "module2");
        ScheduledProcessEventSearchResults<ScheduledProcessEvent> expectedResults =
            new ScheduledProcessEventSearchResults<>(new ArrayList<>(), 0, 0);

        when(scheduledProcessEventDao.getScheduleProcessEvents(modules, 0L, 1000L, "filter", true, 0, 10, "asc"))
            .thenReturn(expectedResults);

        ScheduledProcessEventSearchResults<ScheduledProcessEvent> result =
            service.getScheduledProcessEvents(modules, 0L, 1000L, "filter", true, 0, 10, "asc");

        Assert.assertNotNull(result);
        verify(scheduledProcessEventDao, times(1))
            .getScheduleProcessEvents(modules, 0L, 1000L, "filter", true, 0, 10, "asc");
    }

    @Test
    public void test_getScheduledProcessEvents_by_agent_job_group() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ScheduledProcessEventSearchResults<ScheduledProcessEvent> expectedResults =
            new ScheduledProcessEventSearchResults<>(new ArrayList<>(), 0, 0);

        when(scheduledProcessEventDao.getScheduleProcessEvents("agent1", "group1", "job1", 0L, 1000L, 0, 10, "asc"))
            .thenReturn(expectedResults);

        ScheduledProcessEventSearchResults<ScheduledProcessEvent> result =
            service.getScheduledProcessEvents("agent1", "group1", "job1", 0L, 1000L, 0, 10, "asc");

        Assert.assertNotNull(result);
        verify(scheduledProcessEventDao, times(1))
            .getScheduleProcessEvents("agent1", "group1", "job1", 0L, 1000L, 0, 10, "asc");
    }

    @Test
    public void test_getUpComingScheduledProcesses_with_null_agent() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ScheduledProcessEventSearchResults<UpcomingScheduledProcess> result =
            service.getUpComingScheduledProcesses(null, "flow1", 0L, 1000L, 0, 10);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getResultList().size());
        Assert.assertEquals(0, result.getTotalNumberOfResults());
    }

    @Test
    public void test_getUpComingScheduledProcesses_with_empty_agent() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ScheduledProcessEventSearchResults<UpcomingScheduledProcess> result =
            service.getUpComingScheduledProcesses("", "flow1", 0L, 1000L, 0, 10);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getResultList().size());
    }

    @Test
    public void test_getUpComingScheduledProcesses_with_null_flow() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ScheduledProcessEventSearchResults<UpcomingScheduledProcess> result =
            service.getUpComingScheduledProcesses("agent1", null, 0L, 1000L, 0, 10);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getResultList().size());
    }

    @Test
    public void test_getBusinessStreams() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        List<BusinessStreamMetaData> businessStreams = new ArrayList<>();
        when(solrBusinessStreamMetadataDao.findBusinessStreamsContainingFlow("agent1", "flow1", 0, 1000))
            .thenReturn(businessStreams);

        List<BusinessStreamMetaData> result = service.getBusinessStreams("agent1", "flow1");

        Assert.assertNotNull(result);
        verify(solrBusinessStreamMetadataDao, times(1))
            .findBusinessStreamsContainingFlow("agent1", "flow1", 0, 1000);
    }

    @Test
    public void test_saveConfiguration() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ConfigurationMetaData configMetaData = mock(ConfigurationMetaData.class);

        service.saveConfiguration(configMetaData);

        verify(solrComponentConfigurationMetadataDao, times(1)).save(configMetaData);
    }

    @Test
    public void test_save_with_credentials() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        service.setSolrUsername("testUser");
        service.setSolrPassword("testPassword");

        ScheduledProcessEvent event = new SolrScheduledProcessEvent();
        service.save(event);

        verify(scheduledProcessEventDao, times(1)).setSolrUsername("testUser");
        verify(scheduledProcessEventDao, times(1)).setSolrPassword("testPassword");
        verify(scheduledProcessEventDao, times(1)).save(event);
    }

    @Test
    public void test_multiple_batch_insert_listeners() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, true);

        BatchInsertListener<ScheduledProcessEvent> listener1 = mock(BatchInsertListener.class);
        BatchInsertListener<ScheduledProcessEvent> listener2 = mock(BatchInsertListener.class);
        BatchInsertListener<ScheduledProcessEvent> listener3 = mock(BatchInsertListener.class);

        service.addBatchInsertListener(listener1);
        service.addBatchInsertListener(listener2);
        service.addBatchInsertListener(listener3);

        List<ScheduledProcessEvent> events = Arrays.asList(new SolrScheduledProcessEvent());
        service.insert(events);

        verify(listener1, times(1)).onBatchInsert(any());
        verify(listener2, times(1)).onBatchInsert(any());
        verify(listener3, times(1)).onBatchInsert(any());
    }

    @Test
    public void test_getScheduleProcessAggregateConfigurations_with_offset_limit_zero() {
        service = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao, solrModuleMetadataDao,
            solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);

        ModuleMetaData moduleMetaData = mock(ModuleMetaData.class);
        List<FlowMetaData> flows = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            FlowMetaData flow = mock(FlowMetaData.class);
            when(flow.getName()).thenReturn("flow" + i);
            flows.add(flow);
        }

        when(moduleMetaData.getFlows()).thenReturn(flows);
        when(solrModuleMetadataDao.findById("agent1")).thenReturn(moduleMetaData);

        var result = service.getScheduleProcessAggregateConfigurations("agent1", null, 0, 0);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getResultList().size());
        Assert.assertEquals(10, result.getTotalNumberOfResults());
    }
}
