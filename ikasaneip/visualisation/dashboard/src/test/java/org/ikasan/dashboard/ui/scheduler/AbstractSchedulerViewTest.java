package org.ikasan.dashboard.ui.scheduler;

import org.ikasan.dashboard.ui.UITest;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.scheduled.instance.model.SolrContextInstanceAggregateJobStatusImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractSchedulerViewTest extends UITest {

    @Resource
    protected ModuleMetaDataService moduleMetadataService;
    @MockBean
    protected  SchedulerJobInstanceService schedulerJobInstanceService;
    @MockBean
    protected ScheduledContextInstanceService scheduledContextInstanceService;
    @MockBean
    protected ScheduledContextService scheduledContextService;

    protected List<ScheduledContextInstanceRecord> getScheduledContextInstanceRecords(int count) {
        List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords
            = new ArrayList<>();

        for (int i=0; i<count; i++) {
            ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
            scheduledContextInstanceRecord.setContextName("contextName"+i);
            scheduledContextInstanceRecord.setContextInstanceId("contextInstanceId"+i);
            scheduledContextInstanceRecord.setStatus("PREPARED");
            scheduledContextInstanceRecord.setStartTime(100000000L);

            ContextInstance contextInstance =  new ContextInstanceImpl();
            contextInstance.setStatus(InstanceStatus.PREPARED);
            contextInstance.setStartTime(100000000L);
            contextInstance.setDescription("Description"+i);
            contextInstance.setName("contextName"+i);
            contextInstance.setId("contextInstanceId"+i);
            scheduledContextInstanceRecord.setContextInstance(contextInstance);

            scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
        }

        return scheduledContextInstanceRecords;
    }

    protected List<ScheduledContextRecord> getScheduledContextRecords(int count) {
        List<ScheduledContextRecord> scheduledContextInstanceRecords
            = new ArrayList<>();

        for (int i=0; i<count; i++) {
            ScheduledContextRecord scheduledContextInstanceRecord = new ScheduledContextRecordImpl();
            scheduledContextInstanceRecord.setContextName("contextName"+i);
            scheduledContextInstanceRecord.setTimestamp(100000000L);
            scheduledContextInstanceRecord.setModifiedTimestamp(110000000L);

            ContextTemplate contextInstance =  new ContextTemplateImpl();
            contextInstance.setDescription("Description"+i);
            contextInstance.setName("contextName"+i);
            scheduledContextInstanceRecord.setContext(contextInstance);

            scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
        }

        return scheduledContextInstanceRecords;
    }

    protected List<ScheduledContextInstanceRecord> getScheduledContextInstanceRecordWithId(int id) {
        List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords
            = new ArrayList<>();

        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName("contextName"+id);
        scheduledContextInstanceRecord.setContextInstanceId("contextInstanceId"+id);
        scheduledContextInstanceRecord.setStatus("PREPARED");
        scheduledContextInstanceRecord.setStartTime(100000000L);

        ContextInstance contextInstance =  new ContextInstanceImpl();
        contextInstance.setStatus(InstanceStatus.PREPARED);
        contextInstance.setStartTime(100000000L);
        contextInstance.setDescription("Description"+id);
        contextInstance.setName("contextName"+id);
        contextInstance.setId("contextInstanceId"+id);
        scheduledContextInstanceRecord.setContextInstance(contextInstance);

        scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);


        return scheduledContextInstanceRecords;
    }

    protected List<ContextInstanceAggregateJobStatus> getAggregateContextInstanceStatuses() {
        ContextInstanceAggregateJobStatus aggregateContextInstanceStatus = new SolrContextInstanceAggregateJobStatusImpl("contextInstanceId",
            "contextName", Map.of(InstanceStatus.WAITING.name(), 1, InstanceStatus.RUNNING.name(), 5, InstanceStatus.COMPLETE.name(), 15,
            InstanceStatus.SKIPPED.name(), 0, InstanceStatus.ERROR.name(), 0, InstanceStatus.ON_HOLD.name(), 1, InstanceStatus.LOCK_QUEUED.name(), 0));

        return List.of(aggregateContextInstanceStatus);
    }

    protected ModuleMetadataSearchResults getAgents(int size) {
        List<ModuleMetaData> moduleMetaDataList = new ArrayList<>();
        for (int i=0; i<size; i++) {
            ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
            moduleMetaData.setName("agent"+i);
            moduleMetaData.setUrl("https://www.agent.url");
            moduleMetaData.setDescription("agent description"+i);

            moduleMetaDataList.add(moduleMetaData);
        }

        return new ModuleMetadataSearchResults(moduleMetaDataList, size, 0);
    }
}
