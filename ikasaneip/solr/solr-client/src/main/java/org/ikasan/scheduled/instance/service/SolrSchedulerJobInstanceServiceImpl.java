package org.ikasan.scheduled.instance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.SolrFileEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrInternalEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrQuartzScheduleDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceRecordImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.search.SearchResults;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public List<SchedulerJobInstance> initialiseSchedulerJobInstancesForContext(String contextName, String contextInstanceId) throws SchedulerJobInstanceInitialisationException {
        try {
            SearchResults<? extends SchedulerJobRecord> schedulerJobRecordSearchResults = this.solrSchedulerJobDao.findByContext(contextName, 0, 0);

            if (schedulerJobRecordSearchResults.getTotalNumberOfResults() == 0) {
                return new ArrayList<>();
            }

            schedulerJobRecordSearchResults = this.solrSchedulerJobDao.findByContext(contextName, (int) schedulerJobRecordSearchResults.getTotalNumberOfResults(), 0);

            List<SchedulerJobInstance> results = new ArrayList<>();
            for (SchedulerJobRecord schedulerJobRecord : schedulerJobRecordSearchResults.getResultList()) {
                if(schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                    results.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrFileEventDrivenJobInstanceImpl.class));
                }
                else if(schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                    results.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrInternalEventDrivenJobInstanceImpl.class));
                }
                else if(schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                    results.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrQuartzScheduleDrivenJobInstanceImpl.class));
                }
            }

            List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();

            results.forEach(job -> {
                SchedulerJobInstanceRecord instanceRecord = new SolrSchedulerJobInstanceRecordImpl();
                instanceRecord.setContextName(job.getContextId());
                instanceRecord.setJobName(job.getJobName());
                instanceRecord.setStatus(InstanceStatus.WAITING.name());
                instanceRecord.setTimestamp(System.currentTimeMillis());
                instanceRecord.setContextInstanceId(contextInstanceId);
                instanceRecord.setSchedulerJobInstance(job);

                schedulerJobInstanceRecords.add(instanceRecord);
            });

            this.solrSchedulerJobInstanceDao.save(schedulerJobInstanceRecords);

            return results;
        }
        catch (IOException e) {
            throw new SchedulerJobInstanceInitialisationException(String.format("An exception has occurred " +
                "attempting to initialise scheduler job instances for context[%s]", contextName), e);
        }
    }
}
