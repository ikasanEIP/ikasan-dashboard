package org.ikasan.scheduled.job.service;

import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobRecordImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobRecordImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.solr.SolrServiceBase;

public class SolrSchedulerJobServiceImpl extends SolrServiceBase implements SchedulerJobService {

    private FileEventDrivenJobDao fileEventDrivenJobRecordDao;
    private InternalEventDrivenJobDao internalEventDrivenJobRecordDao;
    private QuartzScheduleDrivenJobDao quartzScheduleDrivenJobRecordDao;
    private SchedulerJobDao schedulerJobRecordDao;

    public SolrSchedulerJobServiceImpl(FileEventDrivenJobDao fileEventDrivenJobRecordDao
        , InternalEventDrivenJobDao internalEventDrivenJobRecordDao
        , QuartzScheduleDrivenJobDao quartzScheduleDrivenJobRecordDao
        , SchedulerJobDao schedulerJobRecordDao) {
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

    public void saveFileEventDrivenJobRecord(FileEventDrivenJobRecord fileEventDrivenJobRecord) {
        this.fileEventDrivenJobRecordDao.save(fileEventDrivenJobRecord);
    }

    public void saveInternalEventDrivenJobRecord(InternalEventDrivenJobRecord internalEventDrivenJobRecord) {
        this.internalEventDrivenJobRecordDao.save(internalEventDrivenJobRecord);
    }

    public void saveQuartzScheduledJobRecord(QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord) {
        this.quartzScheduleDrivenJobRecordDao.save(quartzScheduleDrivenJobRecord);
    }

    @Override
    public void saveFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob) {
        SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
        solrFileEventDrivenJobRecord.setAgentName(fileEventDrivenJob.getAgentName());
        solrFileEventDrivenJobRecord.setJobName(fileEventDrivenJob.getJobName());
        // todo sort out context
        solrFileEventDrivenJobRecord.setContextId("TBD");
        solrFileEventDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        solrFileEventDrivenJobRecord.setFileEventDrivenJob(fileEventDrivenJob);

        this.saveFileEventDrivenJobRecord(solrFileEventDrivenJobRecord);
    }

    @Override
    public void saveInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
        SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
        solrInternalEventDrivenJobRecord.setAgentName(internalEventDrivenJob.getAgentName());
        solrInternalEventDrivenJobRecord.setJobName(internalEventDrivenJob.getJobName());
        // sort out context
        solrInternalEventDrivenJobRecord.setContextId("TBD");
        solrInternalEventDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(internalEventDrivenJob);

        this.saveInternalEventDrivenJobRecord(solrInternalEventDrivenJobRecord);
    }

    @Override
    public void saveQuartzScheduledJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {
        QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
        quartzScheduleDrivenJobRecord.setAgentName(quartzScheduleDrivenJob.getAgentName());
        // todo work out how context fits
        quartzScheduleDrivenJobRecord.setContextId("TBD");
        quartzScheduleDrivenJobRecord.setJobName(quartzScheduleDrivenJob.getJobName());
        quartzScheduleDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        quartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        this.saveQuartzScheduledJobRecord(quartzScheduleDrivenJobRecord);
    }
}
