package org.ikasan.scheduled.job.service;

import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobRecordDao;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.solr.SolrServiceBase;

public class SolrSchedulerJobServiceImpl extends SolrServiceBase implements SchedulerJobService {

    private FileEventDrivenJobRecordDao fileEventDrivenJobRecordDao;
    private InternalEventDrivenJobRecordDao internalEventDrivenJobRecordDao;
    private QuartzScheduleDrivenJobRecordDao quartzScheduleDrivenJobRecordDao;
    private SchedulerJobRecordDao schedulerJobRecordDao;

    public SolrSchedulerJobServiceImpl(FileEventDrivenJobRecordDao fileEventDrivenJobRecordDao
        , InternalEventDrivenJobRecordDao internalEventDrivenJobRecordDao
        , QuartzScheduleDrivenJobRecordDao quartzScheduleDrivenJobRecordDao
        , SchedulerJobRecordDao schedulerJobRecordDao) {
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
