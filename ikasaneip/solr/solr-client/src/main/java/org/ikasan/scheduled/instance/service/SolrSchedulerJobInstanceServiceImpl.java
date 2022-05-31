package org.ikasan.scheduled.instance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.SerializationUtils;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.search.SearchResults;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SolrSchedulerJobInstanceServiceImpl implements SchedulerJobInstanceService {

    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    private SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao;
    private SolrSchedulerJobDaoImpl solrSchedulerJobDao;


    public SolrSchedulerJobInstanceServiceImpl(SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao,
                                               SolrSchedulerJobDaoImpl solrSchedulerJobDao) {
        this.solrSchedulerJobInstanceDao = solrSchedulerJobInstanceDao;
        if (solrSchedulerJobInstanceDao == null) {
            throw new IllegalArgumentException("solrSchedulerJobInstanceDao cannot be null!");
        }
        this.solrSchedulerJobDao = solrSchedulerJobDao;
        if (solrSchedulerJobDao == null) {
            throw new IllegalArgumentException("solrSchedulerJobDao cannot be null!");
        }
    }

    @Override
    public SchedulerJobInstanceRecord findById(String id) {
        return this.solrSchedulerJobInstanceDao.findById(id);
    }

    @Override
    public void save(SchedulerJobInstanceRecord scheduledContextInstanceRecord) {
        this.solrSchedulerJobInstanceDao.save(scheduledContextInstanceRecord);
    }

    @Override
    public void update(SchedulerJobInstance schedulerJobInstance) {
        SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(schedulerJobInstance.getContextInstanceId());
        filter.setJobName(schedulerJobInstance.getJobName());
        filter.setChildContextName(schedulerJobInstance.getChildContextName());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1, 0, null, null);

        SchedulerJobInstanceRecord record = searchResults.getResultList().get(0);

        record.setStatus(schedulerJobInstance.getStatus().name());
        SchedulerJobInstance persistedInstance = record.getSchedulerJobInstance();
        persistedInstance.setScheduledProcessEvent(schedulerJobInstance.getScheduledProcessEvent());
        persistedInstance.setStatus(schedulerJobInstance.getStatus());
        record.setSchedulerJobInstance(schedulerJobInstance);
        record.setModifiedTimestamp(System.currentTimeMillis());
        // todo sort out modified by
        record.setModifiedBy("ContextMachine");

        this.solrSchedulerJobInstanceDao.save(record);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String contextInstanceId, int limit, int offset, String sortField, String sortDirection) {
        return this.solrSchedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(contextInstanceId, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String contextName, int limit, int offset, String sortField, String sortDirection) {
        return this.solrSchedulerJobInstanceDao.getSchedulerJobInstancesByContextName(contextName, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        return this.solrSchedulerJobInstanceDao.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }

    @Override
    public List<SchedulerJobInstance> initialiseSchedulerJobInstancesForContext(ContextInstance contextInstance) throws SchedulerJobInstanceInitialisationException {
        try {
            SearchResults<? extends SchedulerJobRecord> schedulerJobRecordSearchResults = this.solrSchedulerJobDao.findByContext(contextInstance.getName()
                , 0, 0);

            if (schedulerJobRecordSearchResults.getTotalNumberOfResults() == 0) {
                return new ArrayList<>();
            }

            schedulerJobRecordSearchResults = this.solrSchedulerJobDao.findByContext(contextInstance.getName()
                , (int) schedulerJobRecordSearchResults.getTotalNumberOfResults(), 0);

            List<SchedulerJobInstance> schedulerJobInstances = new ArrayList<>();
            for (SchedulerJobRecord schedulerJobRecord : schedulerJobRecordSearchResults.getResultList()) {
                if(schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                    schedulerJobInstances.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrFileEventDrivenJobInstanceImpl.class));
                }
                else if(schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                    schedulerJobInstances.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrInternalEventDrivenJobInstanceImpl.class));
                }
                else if(schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                    schedulerJobInstances.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrQuartzScheduleDrivenJobInstanceImpl.class));
                }
            }

            Map<String, SchedulerJobInstance> schedulerJobInstanceMap = schedulerJobInstances.stream()
                .collect(Collectors.toMap(SchedulerJobInstance::getIdentifier, Function.identity()));

            List<SchedulerJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

            contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
                SchedulerJobInstance instance = schedulerJobInstanceMap.get(schedulerJobInstance.getIdentifier());

                if(instance != null) {
                    SchedulerJobInstance contextualisedInstance = (SchedulerJobInstance)SerializationUtils.clone(instance);
                    contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());

                    contextualisedSchedulerJobInstances.add(contextualisedInstance);
                }
            });

            List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();

            contextualisedSchedulerJobInstances.forEach(job -> {
                SchedulerJobInstanceRecord instanceRecord = new SolrSchedulerJobInstanceRecordImpl();
                instanceRecord.setContextName(job.getContextId());
                instanceRecord.setJobName(job.getJobName());
                instanceRecord.setStatus(InstanceStatus.WAITING.name());
                instanceRecord.setTimestamp(System.currentTimeMillis());
                instanceRecord.setContextInstanceId(contextInstance.getId());
                instanceRecord.setChildContextName(job.getChildContextName());
                instanceRecord.setSchedulerJobInstance(job);

                schedulerJobInstanceRecords.add(instanceRecord);
            });

            this.solrSchedulerJobInstanceDao.save(schedulerJobInstanceRecords);

            return contextualisedSchedulerJobInstances;
        }
        catch (IOException e) {
            throw new SchedulerJobInstanceInitialisationException(String.format("An exception has occurred " +
                "attempting to initialise scheduler job instances for context[%s]", contextInstance.getName()), e);
        }
    }
}
