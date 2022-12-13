package org.ikasan.scheduled.job.service;

import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SolrSchedulerJobServiceImpl extends SolrServiceBase implements SchedulerJobService<SchedulerJobRecord> {

    private static Logger logger = LoggerFactory.getLogger(SolrSchedulerJobServiceImpl.class);

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
    public SchedulerJobRecord findById(String id) {
        return this.schedulerJobRecordDao.findById(id);
    }

    @Override
    public SearchResults findByAgent(String agent, int limit, int offset) {
        return this.schedulerJobRecordDao.findByAgent(agent, limit, offset);
    }

    @Override
    public SchedulerJobRecord findByContextNameAndJobName(String contextId, String jobName) {
        return this.schedulerJobRecordDao.findByContextIdAndJobName(contextId, jobName);
    }

    @Override
    public SearchResults<? extends SchedulerJobRecord> findByContext(String contextId, int limit, int offset) {
        return this.schedulerJobRecordDao.findByContext(contextId, limit, offset);
    }

    @Override
    public SearchResults<? extends SchedulerJobRecord> findByFilter(SchedulerJobSearchFilter filter, int limit, int offset, String sortColumn, String sortDirection) {
        return this.schedulerJobRecordDao.findByFilter(filter, limit, offset, sortColumn, sortDirection);
    }

    @Override
    public void delete(SchedulerJobRecord record) {
        this.schedulerJobRecordDao.delete(record);
    }

    @Override
    public void deleteByAgentName(String agentName) {
        this.schedulerJobRecordDao.deleteByAgentName(agentName);
    }

    @Override
    public void deleteByContextName(String contextName) {
        this.schedulerJobRecordDao.deleteByContextName(contextName);
    }

    @Override
    public void save(List<SchedulerJob> records, String actor) {
        if (records != null && !records.isEmpty()) {
            List<FileEventDrivenJob> fileEventDrivenJobs = new ArrayList<>();
            List<InternalEventDrivenJob> internalEventDrivenJobs = new ArrayList<>();
            List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs = new ArrayList<>();
            records.forEach(job -> {
                if (job instanceof InternalEventDrivenJob) {
                    internalEventDrivenJobs.add((InternalEventDrivenJob) job);
                } else if (job instanceof FileEventDrivenJob) {
                    fileEventDrivenJobs.add((FileEventDrivenJob) job);
                } else if (job instanceof QuartzScheduleDrivenJob) {
                    quartzScheduleDrivenJobs.add((QuartzScheduleDrivenJob) job);
                }
            });

            if (!internalEventDrivenJobs.isEmpty()) {
                this.saveInternalEventDrivenJobs(internalEventDrivenJobs, actor);
            }

            if (!fileEventDrivenJobs.isEmpty()) {
                this.saveFileEventDrivenJobs(fileEventDrivenJobs, actor);
            }

            if (!quartzScheduleDrivenJobs.isEmpty()) {
                this.saveQuartzScheduledJobs(quartzScheduleDrivenJobs, actor);
            }
        }
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
    public void saveInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob, String modifiedBy) {
        InternalEventDrivenJobRecord internalEventDrivenJobRecord =  this.internalEventDrivenJobRecordDao
            .findById(org.ikasan.spec.scheduled.job.model.JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + internalEventDrivenJob.getAgentName() + "_"
                + internalEventDrivenJob.getJobName() + "_" + internalEventDrivenJob.getContextName());

        if(internalEventDrivenJobRecord == null) {
            internalEventDrivenJobRecord = this.internalEventDrivenJobRecord(internalEventDrivenJob, modifiedBy);
        }

        internalEventDrivenJobRecord.setInternalEventDrivenJob(internalEventDrivenJob);

        AtomicBoolean skipped = new AtomicBoolean(false);
        internalEventDrivenJob.getSkippedContexts().entrySet().forEach(entry -> {
            if(entry.getValue()) {
                skipped.set(true);
            }
        });

        internalEventDrivenJobRecord.setSkipped(skipped.get());

        AtomicBoolean held = new AtomicBoolean(false);
        internalEventDrivenJob.getHeldContexts().entrySet().forEach(entry -> {
            if(entry.getValue()) {
                held.set(true);
            }
        });

        internalEventDrivenJobRecord.setHeld(held.get());
        internalEventDrivenJobRecord.setParticipatesInLock(internalEventDrivenJob.isParticipatesInLock());
        internalEventDrivenJobRecord.setModifiedBy(modifiedBy);

        this.saveInternalEventDrivenJobRecord(internalEventDrivenJobRecord);
    }

    @Override
    public void saveInternalEventDrivenJobs(List<InternalEventDrivenJob> quartzScheduleDrivenJobs, String actor) {
        List<InternalEventDrivenJobRecord> records = new ArrayList<>();
        quartzScheduleDrivenJobs.forEach(job -> records.add(internalEventDrivenJobRecord(job, actor)));
        this.saveInternalEventDrivenJobRecords(records);
    }

    private InternalEventDrivenJobRecord internalEventDrivenJobRecord(InternalEventDrivenJob internalEventDrivenJob, String actor) {
        SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
        solrInternalEventDrivenJobRecord.setAgentName(internalEventDrivenJob.getAgentName());
        solrInternalEventDrivenJobRecord.setJobName(internalEventDrivenJob.getJobName());
        solrInternalEventDrivenJobRecord.setContextName(internalEventDrivenJob.getContextName());
        solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        solrInternalEventDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        solrInternalEventDrivenJobRecord.setHeld(!internalEventDrivenJob.getHeldContexts().isEmpty());
        solrInternalEventDrivenJobRecord.setSkipped(!internalEventDrivenJob.getSkippedContexts().isEmpty());

        return solrInternalEventDrivenJobRecord;
    }

    @Override
    public void saveQuartzScheduledJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob, String modifiedBy) {
        QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord =  this.quartzScheduleDrivenJobRecordDao
            .findById(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB + "_" + quartzScheduleDrivenJob.getAgentName() + "_"
                + quartzScheduleDrivenJob.getJobName() + "_" + quartzScheduleDrivenJob.getContextName());

        if(quartzScheduleDrivenJobRecord == null) {
            quartzScheduleDrivenJobRecord = quartzScheduleDrivenJobRecord(quartzScheduleDrivenJob, modifiedBy);
        }

        quartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        quartzScheduleDrivenJobRecord.setModifiedBy(modifiedBy);
        this.saveQuartzScheduledJobRecord(quartzScheduleDrivenJobRecord);
    }

    @Override
    public void saveQuartzScheduledJobs(List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs, String actor) {
        List<QuartzScheduleDrivenJobRecord> records = new ArrayList<>();
        quartzScheduleDrivenJobs.forEach(job -> records.add(quartzScheduleDrivenJobRecord(job, actor)));
        this.saveQuartzScheduledJobRecords(records);
    }

    private QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord(QuartzScheduleDrivenJob quartzScheduleDrivenJob, String actor) {
        QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
        quartzScheduleDrivenJobRecord.setAgentName(quartzScheduleDrivenJob.getAgentName());
        quartzScheduleDrivenJobRecord.setContextName(quartzScheduleDrivenJob.getContextName());
        quartzScheduleDrivenJobRecord.setJobName(quartzScheduleDrivenJob.getJobName());
        quartzScheduleDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        quartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);
        quartzScheduleDrivenJobRecord.setModifiedBy(actor);

        return quartzScheduleDrivenJobRecord;
    }

    @Override
    public void saveFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob, String modifiedBy) {
        FileEventDrivenJobRecord fileEventDrivenJobRecord =  this.fileEventDrivenJobRecordDao
            .findById(JobConstants.FILE_EVENT_DRIVEN_JOB + "_" + fileEventDrivenJob.getAgentName() + "_"
                + fileEventDrivenJob.getJobName() + "_" + fileEventDrivenJob.getContextName());
        this.saveFileEventDrivenJobRecord(fileEventDrivenJobRecord(fileEventDrivenJob, modifiedBy));

        if(fileEventDrivenJobRecord == null) {
            fileEventDrivenJobRecord = fileEventDrivenJobRecord(fileEventDrivenJob, modifiedBy);
        }

        fileEventDrivenJobRecord.setFileEventDrivenJob(fileEventDrivenJob);
        fileEventDrivenJobRecord.setModifiedBy(modifiedBy);
        this.saveFileEventDrivenJobRecord(fileEventDrivenJobRecord);
    }

    @Override
    public void saveFileEventDrivenJobs(List<FileEventDrivenJob> quartzScheduleDrivenJobs, String actor) {
        List<FileEventDrivenJobRecord> records = new ArrayList<>();
        quartzScheduleDrivenJobs.forEach(job -> records.add(fileEventDrivenJobRecord(job, actor)));
        this.saveFileEventDrivenJobRecords(records);
    }

    private SolrFileEventDrivenJobRecordImpl fileEventDrivenJobRecord(FileEventDrivenJob fileEventDrivenJob, String actor) {
        SolrFileEventDrivenJobRecordImpl solrFileEventDrivenJobRecord = new SolrFileEventDrivenJobRecordImpl();
        solrFileEventDrivenJobRecord.setAgentName(fileEventDrivenJob.getAgentName());
        solrFileEventDrivenJobRecord.setJobName(fileEventDrivenJob.getJobName());
        solrFileEventDrivenJobRecord.setContextName(fileEventDrivenJob.getContextName());
        solrFileEventDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        solrFileEventDrivenJobRecord.setFileEventDrivenJob(fileEventDrivenJob);
        solrFileEventDrivenJobRecord.setModifiedBy(actor);

        return solrFileEventDrivenJobRecord;
    }

    @Override
    public void skip(SchedulerJobRecord jobRecord, List<String> childContextNames, String actor) {
        if(jobRecord.getJob() instanceof InternalEventDrivenJob) {
            InternalEventDrivenJobRecord internalEventDrivenJobRecord = this.internalEventDrivenJobRecord
                ((InternalEventDrivenJob)jobRecord.getJob(), actor);
            internalEventDrivenJobRecord.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobRecordDao.skip(internalEventDrivenJobRecord, childContextNames, actor);
        }
        else {
            throw new IllegalArgumentException("Only internal event driven jobs can be skipped.");
        }
    }

    @Override
    public void hold(SchedulerJobRecord jobRecord, List<String> childContextNames, String actor) {
        if(jobRecord.getJob() instanceof InternalEventDrivenJob) {
            InternalEventDrivenJobRecord internalEventDrivenJobRecord = this.internalEventDrivenJobRecord
                ((InternalEventDrivenJob)jobRecord.getJob(), actor);
            internalEventDrivenJobRecord.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobRecordDao.hold(internalEventDrivenJobRecord,
                childContextNames, actor);
        }
        else {
            throw new IllegalArgumentException("Only internal event driven jobs can be held.");
        }
    }

    @Override
    public void enable(SchedulerJobRecord jobRecord, String actor) {
        if(jobRecord.getJob() instanceof InternalEventDrivenJob) {
            InternalEventDrivenJobRecord internalEventDrivenJobRecord = this.internalEventDrivenJobRecord
                ((InternalEventDrivenJob)jobRecord.getJob(),actor);
            internalEventDrivenJobRecord.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobRecordDao.enable(internalEventDrivenJobRecord, actor);
        }
        else {
            throw new IllegalArgumentException("Only internal event driven jobs can be enabled.");
        }
    }

    @Override
    public void release(SchedulerJobRecord jobRecord, String actor) {
        if(jobRecord.getJob() instanceof InternalEventDrivenJob) {
            InternalEventDrivenJobRecord internalEventDrivenJobRecord = this.internalEventDrivenJobRecord
                ((InternalEventDrivenJob)jobRecord.getJob(), actor);
            internalEventDrivenJobRecord.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobRecordDao.release(internalEventDrivenJobRecord, actor);
        }
        else {
            throw new IllegalArgumentException("Only internal event driven jobs can be released.");
        }
    }

    @Override
    public void releaseAll(String contextName, String actor) {
        SchedulerJobSearchFilter filter = new SolrSchedulerJobSearchFilterImpl();
        filter.setContextSearchFilter(contextName);
        filter.setHeld(true);

        List<InternalEventDrivenJobRecord> jobsToRelease = this.getFilteredInternalEventDrivenJobRecords(filter);

        if(jobsToRelease.size() > 0) {
            this.internalEventDrivenJobRecordDao.releaseAll(jobsToRelease, actor);
        }
    }

    @Override
    public void enableAll(String contextName, String actor) {
        SchedulerJobSearchFilter filter = new SolrSchedulerJobSearchFilterImpl();
        filter.setContextSearchFilter(contextName);
        filter.setSkipped(true);

        List<InternalEventDrivenJobRecord> jobsToRelease = this.getFilteredInternalEventDrivenJobRecords(filter);

        if(jobsToRelease.size() > 0) {
            this.internalEventDrivenJobRecordDao.enableAll(jobsToRelease, actor);
        }
    }

    @Override
    public void holdAll(String contextName, String actor) {
        SchedulerJobSearchFilter filter = new SolrSchedulerJobSearchFilterImpl();
        filter.setContextSearchFilter(contextName);

        List<InternalEventDrivenJobRecord> jobsToRelease = this.getFilteredInternalEventDrivenJobRecords(filter);

        if(jobsToRelease.size() > 0) {
            this.internalEventDrivenJobRecordDao.holdAll(jobsToRelease, actor);
        }
    }

    private List<InternalEventDrivenJobRecord> getFilteredInternalEventDrivenJobRecords(SchedulerJobSearchFilter filter) {
        SearchResults<SchedulerJobRecord> searchResults = (SearchResults<SchedulerJobRecord>) this.schedulerJobRecordDao
            .findByFilter(filter, -1, -1, null, null);

        ArrayList<InternalEventDrivenJobRecord> filteredJobs = new ArrayList<>();
        searchResults.getResultList().forEach(schedulerJobRecord -> {
            if(schedulerJobRecord.getJob() instanceof InternalEventDrivenJob) {
                InternalEventDrivenJobRecord internalEventDrivenJobRecord = this.internalEventDrivenJobRecord
                    ((InternalEventDrivenJob)schedulerJobRecord.getJob(),"");
                internalEventDrivenJobRecord.setTimestamp(schedulerJobRecord.getTimestamp());
                filteredJobs.add(internalEventDrivenJobRecord);
            }
        });

        return filteredJobs;
    }

    @Override
    public void renameContextForJobs(String oldName, String newName, String actor) {
        SearchResults<? extends SchedulerJobRecord> schedulerJobRecords = this.findByContext(oldName, -1, -1);

        List<InternalEventDrivenJob> internalEventDrivenJobs = new ArrayList<>();
        List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs = new ArrayList<>();
        List<FileEventDrivenJob> fileEventDrivenJobs = new ArrayList<>();

        schedulerJobRecords.getResultList().forEach(schedulerJobRecord -> {
            SchedulerJob job = schedulerJobRecord.getJob();

            job.setContextName(newName);

            if(job instanceof InternalEventDrivenJob) {
                internalEventDrivenJobs.add((InternalEventDrivenJob) job);
            }
            else if(job instanceof FileEventDrivenJob) {
                fileEventDrivenJobs.add((FileEventDrivenJob) job);
            }
            else if(job instanceof QuartzScheduleDrivenJob) {
                quartzScheduleDrivenJobs.add((QuartzScheduleDrivenJob) job);
            }

        });

        this.deleteByContextName(oldName);
        this.saveInternalEventDrivenJobs(internalEventDrivenJobs, actor);
        this.saveQuartzScheduledJobs(quartzScheduleDrivenJobs, actor);
        this.saveFileEventDrivenJobs(fileEventDrivenJobs, actor);
    }

    @Override
    public Map<String, InternalEventDrivenJob> getCommandExecutionJobsForContext(String contextName) {
        SchedulerJobSearchFilter filter = new SolrSchedulerJobSearchFilterImpl();
        filter.setContextSearchFilter(contextName);
        filter.setJobTypeFilter(JobConstants.INTERNAL_EVENT_DRIVEN_JOB);

        SearchResults<SchedulerJobRecord> schedulerJobRecordDaoByFilter
            = (SearchResults<SchedulerJobRecord>) this.schedulerJobRecordDao.findByFilter(filter, -1, -1, null, null);

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = new HashMap<>();

        schedulerJobRecordDaoByFilter.getResultList().forEach(record
            -> internalEventDrivenJobMap.put(record.getJob().getIdentifier(), (InternalEventDrivenJob) record.getJob()));

        return internalEventDrivenJobMap;
    }
}
