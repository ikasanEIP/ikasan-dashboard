package org.ikasan.scheduled.job.service;

import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobRecordImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobRecordImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobRecordImpl;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;

import java.util.ArrayList;
import java.util.List;

public class SolrSchedulerJobServiceImpl extends SolrServiceBase implements SchedulerJobService<SolrSchedulerJobRecordImpl> {

    private SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao;
    private SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao;
    private SolrSchedulerJobDaoImpl schedulerJobRecordDao;

    public SolrSchedulerJobServiceImpl(SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao
        , SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao
        , SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao
        , SolrSchedulerJobDaoImpl schedulerJobRecordDao) {
        this.fileEventDrivenJobRecordDao = fileEventDrivenJobRecordDao;
        if(this.fileEventDrivenJobRecordDao == null)
        {
            throw new IllegalArgumentException("fileEventDrivenJobRecordDao cannot be null!");
        }
        this.internalEventDrivenJobRecordDao = internalEventDrivenJobRecordDao;
        if(this.internalEventDrivenJobRecordDao == null)
        {
            throw new IllegalArgumentException("internalEventDrivenJobRecordDao cannot be null!");
        }
        this.quartzScheduleDrivenJobRecordDao = quartzScheduleDrivenJobRecordDao;
        if(this.quartzScheduleDrivenJobRecordDao == null)
        {
            throw new IllegalArgumentException("quartzScheduleDrivenJobRecordDao cannot be null!");
        }
        this.schedulerJobRecordDao = schedulerJobRecordDao;
        if(this.schedulerJobRecordDao == null)
        {
            throw new IllegalArgumentException("schedulerJobRecordDao cannot be null!");
        }
    }

    @Override
    public SearchResults findByAgent(String agent, int limit, int offset) {
        return this.schedulerJobRecordDao.findByAgent(agent, limit, offset);
    }

    @Override
    public void delete(SolrSchedulerJobRecordImpl record) {
        this.schedulerJobRecordDao.delete(record);
    }

    @Override
    public void deleteByAgentName(String agentName) {
        this.schedulerJobRecordDao.deleteByAgentName(agentName);
    }

    @Override
    public void saveFileEventDrivenJobRecord(FileEventDrivenJobRecord fileEventDrivenJobRecord) {
        this.fileEventDrivenJobRecordDao.save(fileEventDrivenJobRecord);
    }

    @Override
    public void saveInternalEventDrivenJobRecord(InternalEventDrivenJobRecord internalEventDrivenJobRecord) {
        this.internalEventDrivenJobRecordDao.save(internalEventDrivenJobRecord);
    }

    @Override
    public void saveQuartzScheduledJobRecord(QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord) {
        this.quartzScheduleDrivenJobRecordDao.save(quartzScheduleDrivenJobRecord);
    }

    @Override
    public void saveFileEventDrivenJobRecords(List<FileEventDrivenJobRecord> fileEventDrivenJobRecords) {
        this.fileEventDrivenJobRecordDao.save(fileEventDrivenJobRecords);
    }

    @Override
    public void saveInternalEventDrivenJobRecords(List<InternalEventDrivenJobRecord> internalEventDrivenJobRecord) {
        this.internalEventDrivenJobRecordDao.save(internalEventDrivenJobRecord);
    }

    @Override
    public void saveQuartzScheduledJobRecords(List<QuartzScheduleDrivenJobRecord> quartzScheduleDrivenJobRecord) {
        this.quartzScheduleDrivenJobRecordDao.save(quartzScheduleDrivenJobRecord);
    }

    @Override
    public void saveInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
        this.saveInternalEventDrivenJobRecord(internalEventDrivenJobRecord(internalEventDrivenJob));
    }

    @Override
    public void saveInternalEventDrivenJobs(List<InternalEventDrivenJob> quartzScheduleDrivenJobs) {
        List<InternalEventDrivenJobRecord> records = new ArrayList<>();
        quartzScheduleDrivenJobs.forEach(job -> records.add(internalEventDrivenJobRecord(job)));
        this.saveInternalEventDrivenJobRecords(records);
    }

    private SolrInternalEventDrivenJobRecordImpl internalEventDrivenJobRecord(InternalEventDrivenJob internalEventDrivenJob) {
        SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
        solrInternalEventDrivenJobRecord.setAgentName(internalEventDrivenJob.getAgentName());
        solrInternalEventDrivenJobRecord.setJobName(internalEventDrivenJob.getJobName());
        // sort out context
        solrInternalEventDrivenJobRecord.setContextId("TBD");
        solrInternalEventDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(internalEventDrivenJob);

        return solrInternalEventDrivenJobRecord;
    }

    @Override
    public void saveQuartzScheduledJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {
        this.saveQuartzScheduledJobRecord(quartzScheduleDrivenJobRecord(quartzScheduleDrivenJob));
    }

    @Override
    public void saveQuartzScheduledJobs(List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs) {
        List<QuartzScheduleDrivenJobRecord> records = new ArrayList<>();
        quartzScheduleDrivenJobs.forEach(job -> records.add(quartzScheduleDrivenJobRecord(job)));
        this.saveQuartzScheduledJobRecords(records);
    }

    private QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {
        QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
        quartzScheduleDrivenJobRecord.setAgentName(quartzScheduleDrivenJob.getAgentName());
        // todo work out how context fits
        quartzScheduleDrivenJobRecord.setContextId("TBD");
        quartzScheduleDrivenJobRecord.setJobName(quartzScheduleDrivenJob.getJobName());
        quartzScheduleDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        quartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        return quartzScheduleDrivenJobRecord;
    }

    @Override
    public void saveFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob) {
        this.saveFileEventDrivenJobRecord(fileEventDrivenJobRecord(fileEventDrivenJob));
    }

    @Override
    public void saveFileEventDrivenJobs(List<FileEventDrivenJob> quartzScheduleDrivenJobs) {
        List<FileEventDrivenJobRecord> records = new ArrayList<>();
        quartzScheduleDrivenJobs.forEach(job -> records.add(fileEventDrivenJobRecord(job)));
        this.saveFileEventDrivenJobRecords(records);
    }

    private SolrFileEventDrivenJobRecordImpl fileEventDrivenJobRecord(FileEventDrivenJob fileEventDrivenJob) {
        SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
        solrFileEventDrivenJobRecord.setAgentName(fileEventDrivenJob.getAgentName());
        solrFileEventDrivenJobRecord.setJobName(fileEventDrivenJob.getJobName());
        // todo sort out context
        solrFileEventDrivenJobRecord.setContextId("TBD");
        solrFileEventDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        solrFileEventDrivenJobRecord.setFileEventDrivenJob(fileEventDrivenJob);

        return solrFileEventDrivenJobRecord;
    }
}
