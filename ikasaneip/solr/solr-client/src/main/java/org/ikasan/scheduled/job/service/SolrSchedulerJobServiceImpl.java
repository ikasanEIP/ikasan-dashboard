package org.ikasan.scheduled.job.service;

import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
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
}
