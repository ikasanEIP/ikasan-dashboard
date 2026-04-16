package org.ikasan.dashboard.ui.scheduler;

import org.ikasan.dashboard.ui.UITest;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.scheduled.context.model.ScheduledContextRecordLiteImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.instance.model.SolrContextInstanceAggregateJobStatusImpl;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractSchedulerViewTest extends UITest {

    @Resource
    protected ModuleMetaDataService moduleMetadataService;
    @MockitoBean
    protected  SchedulerJobInstanceService schedulerJobInstanceService;
    @MockitoBean
    protected ScheduledContextInstanceService scheduledContextInstanceService;
    @MockitoBean
    protected ScheduledContextService scheduledContextService;
    @MockitoBean
    protected SchedulerJobService schedulerJobService;
    @MockitoBean
    protected ContextProfileService contextProfileService;
    @MockitoBean
    protected ScheduledProcessManagementService scheduledProcessManagementService;


    protected SearchResults<SchedulerJobRecord> getSchedulerJobs() {
        SearchResults<SchedulerJobRecord> searchResults = new SearchResultsImpl<>(List.of(), 0, 0);
        return searchResults;
    }

    protected SearchResults<ContextProfileRecord> getContextProfiles() {
        SearchResults<ContextProfileRecord> searchResults = new SearchResultsImpl<>(List.of(), 0, 0);
        return searchResults;
    }

    /**
     * Retrieves a list of ScheduledContextInstanceRecord instances with the specified count.
     *
     * @param count The number of ScheduledContextInstanceRecord instances to retrieve
     * @return List of ScheduledContextInstanceRecord instances containing the retrieved records
     */
    protected List<ScheduledContextInstanceRecord> getScheduledContextInstanceRecords(int count) {
        List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords
            = new ArrayList<>();

        for (int i=0; i<count; i++) {
            ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
            scheduledContextInstanceRecord.setContextName("contextName"+i);
            scheduledContextInstanceRecord.setContextInstanceId("contextInstanceId"+i);
            scheduledContextInstanceRecord.setStatus("PREPARED");
            scheduledContextInstanceRecord.setStartTime(100000000L);

            if(ContextMachineCache.instance().containsInstanceIdentifier(scheduledContextInstanceRecord.getContextInstanceId())) {
                scheduledContextInstanceRecord.setContextInstance(ContextMachineCache.instance()
                    .getByContextInstanceId(scheduledContextInstanceRecord.getContextInstanceId()).getContext());
            }
            else {
                ContextInstance contextInstance =  new ContextInstanceImpl();
                contextInstance.setStatus(InstanceStatus.PREPARED);
                contextInstance.setStartTime(100000000L);
                contextInstance.setDescription("Description"+i);
                contextInstance.setName("contextName"+i);
                contextInstance.setId("contextInstanceId"+i);
                scheduledContextInstanceRecord.setContextInstance(contextInstance);
            }

            scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
        }

        return scheduledContextInstanceRecords;
    }

    /**
     * Retrieves a list of scheduled context records with the specified count.
     *
     * @param count The number of scheduled context records to retrieve
     * @return List of ScheduledContextRecord instances containing the retrieved records
     */
    protected List<ScheduledContextRecord> getScheduledContextRecords(int count) {
        List<ScheduledContextRecord> scheduledContextInstanceRecords
            = new ArrayList<>();

        for (int i=0; i<count; i++) {
            ScheduledContextRecord scheduledContextInstanceRecord = new ScheduledContextRecordImpl();
            scheduledContextInstanceRecord.setContextName("contextName"+i);
            scheduledContextInstanceRecord.setTimestamp(100000000L);
            scheduledContextInstanceRecord.setModifiedTimestamp(110000000L);

            ContextTemplate contextTemplate =  new ContextTemplateImpl();
            contextTemplate.setDescription("Description"+i);
            contextTemplate.setName("contextName"+i);

            ContextParameterImpl filenameContextParam = new ContextParameterImpl();
            filenameContextParam.setName("filename_replacement");
            filenameContextParam.setDefaultValue("replacement");

            ContextParameterImpl filePathContextParam = new ContextParameterImpl();
            filePathContextParam.setName("filepath_replacement");
            filePathContextParam.setDefaultValue("replacement");

            List<ContextParameter> contextParameters = new ArrayList<>();
            contextParameters.add(filenameContextParam);
            contextParameters.add(filePathContextParam);
            contextTemplate.setContextParameters(contextParameters);

            scheduledContextInstanceRecord.setContext(contextTemplate);

            scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
        }

        return scheduledContextInstanceRecords;
    }

    /**
     * Retrieves a ScheduledContextRecord with predefined values for testing purposes.
     *
     * @return A ScheduledContextRecord instance containing the predefined values
     */
    protected ScheduledContextRecord getScheduledContextRecord() {
        ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setTimestamp(100000000L);
        scheduledContextRecord.setModifiedTimestamp(110000000L);

        ContextTemplate contextTemplate =  new ContextTemplateImpl();
        contextTemplate.setDescription("Description");
        contextTemplate.setName("contextName");

        ContextParameterImpl filenameContextParam = new ContextParameterImpl();
        filenameContextParam.setName("filename_replacement");
        filenameContextParam.setDefaultValue("replacement");

        ContextParameterImpl filePathContextParam = new ContextParameterImpl();
        filePathContextParam.setName("filepath_replacement");
        filePathContextParam.setDefaultValue("replacement");

        List<ContextParameter> contextParameters = new ArrayList<>();
        contextParameters.add(filenameContextParam);
        contextParameters.add(filePathContextParam);
        contextTemplate.setContextParameters(contextParameters);

        scheduledContextRecord.setContext(contextTemplate);

        return scheduledContextRecord;
    }

    /**
     * Retrieves a list of scheduled context records with the specified count.
     *
     * @param count The number of scheduled context records to retrieve
     * @return List of ScheduledContextRecord instances containing the retrieved records
     */
    protected List<ScheduledContextRecordLite> getScheduledContextRecordLites(int count) {
        List<ScheduledContextRecordLite> scheduledContextInstanceRecords
            = new ArrayList<>();

        for (int i=0; i<count; i++) {
            ScheduledContextRecordLite scheduledContextInstanceRecord = new ScheduledContextRecordLiteImpl();
            scheduledContextInstanceRecord.setContextName("contextName"+i);
            scheduledContextInstanceRecord.setTimestamp(100000000L);
            scheduledContextInstanceRecord.setModifiedTimestamp(110000000L);

            ContextTemplate contextTemplate =  new ContextTemplateImpl();
            contextTemplate.setDescription("Description"+i);
            contextTemplate.setName("contextName"+i);

            ContextParameterImpl filenameContextParam = new ContextParameterImpl();
            filenameContextParam.setName("filename_replacement");
            filenameContextParam.setDefaultValue("replacement");

            ContextParameterImpl filePathContextParam = new ContextParameterImpl();
            filePathContextParam.setName("filepath_replacement");
            filePathContextParam.setDefaultValue("replacement");

            List<ContextParameter> contextParameters = new ArrayList<>();
            contextParameters.add(filenameContextParam);
            contextParameters.add(filePathContextParam);
            contextTemplate.setContextParameters(contextParameters);

            scheduledContextInstanceRecord.setDescription(contextTemplate.getDescription());

            scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
        }

        return scheduledContextInstanceRecords;
    }

    /**
     * Retrieves a ScheduledContextInstanceRecord with the specified id. If the context instance exists in the
     * ContextMachineCache, it will be retrieved. Otherwise, a new ContextInstance will be created with the
     * provided id.
     *
     * @param id The id of the scheduled context instance
     * @return List of ScheduledContextInstanceRecord containing the retrieved or newly created context instance
     */
    protected List<ScheduledContextInstanceRecord> getScheduledContextInstanceRecordWithId(int id) {
        List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords
            = new ArrayList<>();

        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName("contextName"+id);
        scheduledContextInstanceRecord.setContextInstanceId("contextInstanceId"+id);
        scheduledContextInstanceRecord.setStatus("PREPARED");
        scheduledContextInstanceRecord.setStartTime(100000000L);

        if(ContextMachineCache.instance().containsInstanceIdentifier(scheduledContextInstanceRecord.getContextInstanceId())) {
            scheduledContextInstanceRecord.setContextInstance(ContextMachineCache.instance()
                .getByContextInstanceId(scheduledContextInstanceRecord.getContextInstanceId()).getContext());
        }
        else {
            ContextInstance contextInstance = new ContextInstanceImpl();
            contextInstance.setStatus(InstanceStatus.PREPARED);
            contextInstance.setStartTime(100000000L);
            contextInstance.setDescription("Description" + id);
            contextInstance.setName("contextName" + id);
            contextInstance.setId("contextInstanceId" + id);
            scheduledContextInstanceRecord.setContextInstance(contextInstance);
        }

        scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);


        return scheduledContextInstanceRecords;
    }

    /**
     * Retrieves a list of aggregate context instance statuses.
     *
     * @return List of ContextInstanceAggregateJobStatus representing the aggregate statuses of context instances
     */
    protected List<ContextInstanceAggregateJobStatus> getAggregateContextInstanceStatuses() {
        ContextInstanceAggregateJobStatus aggregateContextInstanceStatus = new SolrContextInstanceAggregateJobStatusImpl("contextInstanceId",
            "contextName", Map.of(InstanceStatus.WAITING.name(), 1, InstanceStatus.RUNNING.name(), 5, InstanceStatus.COMPLETE.name(), 15,
            InstanceStatus.SKIPPED.name(), 0, InstanceStatus.ERROR.name(), 0, InstanceStatus.ON_HOLD.name(), 1, InstanceStatus.LOCK_QUEUED.name(), 0), true);

        aggregateContextInstanceStatus.setRepeatingJobsStatusCounts(Map.of(InstanceStatus.COMPLETE.name(), 5, InstanceStatus.ERROR.name(), 2));

        return List.of(aggregateContextInstanceStatus);
    }

    /**
     * Retrieves a list of agents based on the specified size.
     *
     * @param size The number of agents to retrieve
     * @return ModuleMetadataSearchResults object containing the list of agents with their details
     */
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
