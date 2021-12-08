package org.ikasan.scheduled.job.service;

import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobRecordDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobRecordDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobRecordDaoImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobRecordDaoImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.solr.SolrServiceBase;

public class SolrSchedulerJobServiceImpl extends SolrServiceBase {

    private SolrFileEventDrivenJobRecordDaoImpl fileEventDrivenJobRecordDao;
    private SolrInternalEventDrivenJobRecordDaoImpl internalEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobRecordDaoImpl quartzScheduleDrivenJobRecordDao;
    private SolrSchedulerJobRecordDaoImpl schedulerJobRecordDao;

    public SolrSchedulerJobServiceImpl(SolrFileEventDrivenJobRecordDaoImpl fileEventDrivenJobRecordDao
        , SolrInternalEventDrivenJobRecordDaoImpl internalEventDrivenJobRecordDao
        , SolrQuartzScheduleDrivenJobRecordDaoImpl quartzScheduleDrivenJobRecordDao
        , SolrSchedulerJobRecordDaoImpl schedulerJobRecordDao) {
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
        this.fileEventDrivenJobRecordDao.setSolrUsername(solrUsername);
        this.fileEventDrivenJobRecordDao.setSolrPassword(solrPassword);
        this.fileEventDrivenJobRecordDao.save(fileEventDrivenJobRecord);
    }

    public void saveInternalEventDrivenJobRecord(InternalEventDrivenJobRecord internalEventDrivenJobRecord) {
        this.internalEventDrivenJobRecordDao.setSolrUsername(solrUsername);
        this.internalEventDrivenJobRecordDao.setSolrPassword(solrPassword);
        this.internalEventDrivenJobRecordDao.save(internalEventDrivenJobRecord);
    }

    public void saveQuartzScheduledJobRecord(QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord) {
        this.quartzScheduleDrivenJobRecordDao.setSolrUsername(solrUsername);
        this.quartzScheduleDrivenJobRecordDao.setSolrPassword(solrPassword);
        this.quartzScheduleDrivenJobRecordDao.save(quartzScheduleDrivenJobRecord);
    }
}
