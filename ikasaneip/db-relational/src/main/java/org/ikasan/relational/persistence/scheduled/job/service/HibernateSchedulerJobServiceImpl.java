package org.ikasan.relational.persistence.scheduled.job.service;

import org.ikasan.relational.persistence.scheduled.job.dao.*;
import org.ikasan.relational.persistence.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hibernate/PostgreSQL implementation of SchedulerJobService.
 *
 * This service provides business logic for managing scheduler jobs using
 * Hibernate persistence. It delegates to DAO layer for data access and
 * manages polymorphic job types (FileEventDriven, InternalEventDriven,
 * QuartzScheduleDriven, GlobalEvent, ContextStart, ContextTerminal).
 */
public class HibernateSchedulerJobServiceImpl implements SchedulerJobService<HibernateSchedulerJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateSchedulerJobServiceImpl.class);

    private final HibernateFileEventDrivenJobDaoImpl fileEventDrivenJobDao;
    private final HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobDao;
    private final HibernateQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobDao;
    private final HibernateGlobalEventJobDaoImpl globalEventJobDao;
    private final HibernateContextStartJobDaoImpl contextStartJobDao;
    private final HibernateContextTerminalJobDaoImpl contextTerminalJobDao;
    private final HibernateSchedulerJobDaoImpl schedulerJobDao;
    private final HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobTemplateDao;

    /**
     * Constructor with required dependencies
     *
     * @param fileEventDrivenJobDao               DAO for file event driven jobs
     * @param internalEventDrivenJobDao           DAO for internal event driven jobs
     * @param quartzScheduleDrivenJobDao          DAO for quartz schedule driven jobs
     * @param globalEventJobDao                   DAO for global event jobs
     * @param contextStartJobDao                  DAO for context start jobs
     * @param contextTerminalJobDao               DAO for context terminal jobs
     * @param schedulerJobDao                     Polymorphic DAO for all scheduler jobs
     * @param internalEventDrivenJobTemplateDao   DAO for internal event driven job templates
     */
    public HibernateSchedulerJobServiceImpl(
            HibernateFileEventDrivenJobDaoImpl fileEventDrivenJobDao,
            HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobDao,
            HibernateQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobDao,
            HibernateGlobalEventJobDaoImpl globalEventJobDao,
            HibernateContextStartJobDaoImpl contextStartJobDao,
            HibernateContextTerminalJobDaoImpl contextTerminalJobDao,
            HibernateSchedulerJobDaoImpl schedulerJobDao,
            HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobTemplateDao) {

        this.fileEventDrivenJobDao = fileEventDrivenJobDao;
        if (this.fileEventDrivenJobDao == null) {
            throw new IllegalArgumentException("fileEventDrivenJobDao cannot be null!");
        }
        this.internalEventDrivenJobDao = internalEventDrivenJobDao;
        if (this.internalEventDrivenJobDao == null) {
            throw new IllegalArgumentException("internalEventDrivenJobDao cannot be null!");
        }
        this.quartzScheduleDrivenJobDao = quartzScheduleDrivenJobDao;
        if (this.quartzScheduleDrivenJobDao == null) {
            throw new IllegalArgumentException("quartzScheduleDrivenJobDao cannot be null!");
        }
        this.globalEventJobDao = globalEventJobDao;
        if (this.globalEventJobDao == null) {
            throw new IllegalArgumentException("globalEventJobDao cannot be null!");
        }
        this.contextStartJobDao = contextStartJobDao;
        if (this.contextStartJobDao == null) {
            throw new IllegalArgumentException("contextStartJobDao cannot be null!");
        }
        this.contextTerminalJobDao = contextTerminalJobDao;
        if (this.contextTerminalJobDao == null) {
            throw new IllegalArgumentException("contextTerminalJobDao cannot be null!");
        }
        this.schedulerJobDao = schedulerJobDao;
        if (this.schedulerJobDao == null) {
            throw new IllegalArgumentException("schedulerJobDao cannot be null!");
        }
        this.internalEventDrivenJobTemplateDao = internalEventDrivenJobTemplateDao;
        if (this.internalEventDrivenJobTemplateDao == null) {
            throw new IllegalArgumentException("internalEventDrivenJobTemplateDao cannot be null!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateSchedulerJobRecord findById(String id) {
        logger.debug("Finding job by id: {}", id);
        return this.schedulerJobDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByAgent(String agent, int limit, int offset) {
        logger.debug("Finding jobs by agent: {} with limit={}, offset={}", agent, limit, offset);
        return this.schedulerJobDao.findByAgent(agent, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateSchedulerJobRecord findByContextNameAndJobName(String contextName, String jobName) {
        logger.debug("Finding job by contextName: {} and jobName: {}", contextName, jobName);
        return this.schedulerJobDao.findByContextIdAndJobName(contextName, jobName);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding jobs by context: {} with limit={}, offset={}", contextId, limit, offset);
        return this.schedulerJobDao.findByContext(contextId, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByFilter(
            SchedulerJobSearchFilter filter, int limit, int offset, String sortColumn, String sortDirection) {
        logger.debug("Finding jobs by filter with limit={}, offset={}", limit, offset);
        return this.schedulerJobDao.findByFilter(filter, limit, offset, sortColumn, sortDirection);
    }

    @Override
    @Transactional
    public void delete(HibernateSchedulerJobRecord record) {
        logger.debug("Deleting job: {}", record.getId());
        this.schedulerJobDao.delete(record);
    }

    @Override
    @Transactional
    public void deleteByAgentName(String agentName) {
        logger.debug("Deleting all jobs for agent: {}", agentName);
        this.schedulerJobDao.deleteByAgentName(agentName);
    }

    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        logger.debug("Deleting all jobs for context: {}", contextName);
        this.schedulerJobDao.deleteByContextName(contextName);
    }

    @Override
    @Transactional
    public void save(List<SchedulerJob> records, String actor) {
        if (records != null && !records.isEmpty()) {
            logger.debug("Saving {} jobs by actor: {}", records.size(), actor);

            List<FileEventDrivenJob> fileEventDrivenJobs = new ArrayList<>();
            List<InternalEventDrivenJob> internalEventDrivenJobs = new ArrayList<>();
            List<InternalEventDrivenJob> internalEventDrivenJobTemplates = new ArrayList<>();
            List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs = new ArrayList<>();
            List<GlobalEventJob> globalEventJobs = new ArrayList<>();
            List<ContextStartJob> contextStartJobs = new ArrayList<>();
            List<ContextTerminalJob> contextTerminalJobs = new ArrayList<>();

            // Categorize jobs by type
            records.forEach(job -> {
                if (job instanceof InternalEventDrivenJob) {
                    if (Boolean.TRUE.equals(job.isTemplateJob())) {
                        internalEventDrivenJobTemplates.add((InternalEventDrivenJob) job);
                    } else {
                        internalEventDrivenJobs.add((InternalEventDrivenJob) job);
                    }
                } else if (job instanceof FileEventDrivenJob) {
                    fileEventDrivenJobs.add((FileEventDrivenJob) job);
                } else if (job instanceof QuartzScheduleDrivenJob) {
                    quartzScheduleDrivenJobs.add((QuartzScheduleDrivenJob) job);
                } else if (job instanceof GlobalEventJob) {
                    globalEventJobs.add((GlobalEventJob) job);
                } else if (job instanceof ContextStartJob) {
                    contextStartJobs.add((ContextStartJob) job);
                } else if (job instanceof ContextTerminalJob) {
                    contextTerminalJobs.add((ContextTerminalJob) job);
                }
            });

            // Save each job type
            if (!internalEventDrivenJobs.isEmpty()) {
                this.saveInternalEventDrivenJobs(internalEventDrivenJobs, actor);
            }
            if (!internalEventDrivenJobTemplates.isEmpty()) {
                this.saveInternalEventDrivenJobTemplates(internalEventDrivenJobTemplates, actor);
            }
            if (!fileEventDrivenJobs.isEmpty()) {
                this.saveFileEventDrivenJobs(fileEventDrivenJobs, actor);
            }
            if (!quartzScheduleDrivenJobs.isEmpty()) {
                this.saveQuartzScheduledJobs(quartzScheduleDrivenJobs, actor);
            }
            if (!globalEventJobs.isEmpty()) {
                this.saveGlobalEventJobs(globalEventJobs, actor);
            }
            if (!contextStartJobs.isEmpty()) {
                this.saveContextStartJobs(contextStartJobs, actor);
            }
            if (!contextTerminalJobs.isEmpty()) {
                this.saveContextTerminalJobs(contextTerminalJobs, actor);
            }
        }
    }

    @Override
    @Transactional
    public void saveFileEventDrivenJobRecord(FileEventDrivenJobRecord fileEventDrivenJobRecord) {
        logger.debug("Saving FileEventDrivenJobRecord: {}", fileEventDrivenJobRecord.getId());
        this.fileEventDrivenJobDao.save((HibernateFileEventDrivenJobRecord) fileEventDrivenJobRecord);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJobRecord(InternalEventDrivenJobRecord internalEventDrivenJobRecord) {
        logger.debug("Saving InternalEventDrivenJobRecord: {}", internalEventDrivenJobRecord.getId());
        this.internalEventDrivenJobDao.save((HibernateInternalEventDrivenJobRecord) internalEventDrivenJobRecord);
    }

    @Override
    @Transactional
    public void saveQuartzScheduledJobRecord(QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord) {
        logger.debug("Saving QuartzScheduleDrivenJobRecord: {}", quartzScheduleDrivenJobRecord.getId());
        this.quartzScheduleDrivenJobDao.save((HibernateQuartzScheduleDrivenJobRecord) quartzScheduleDrivenJobRecord);
    }

    @Override
    @Transactional
    public void saveGlobalEventJobRecord(GlobalEventJobRecord globalEventJobRecord) {
        logger.debug("Saving GlobalEventJobRecord: {}", globalEventJobRecord.getId());
        this.globalEventJobDao.save((HibernateGlobalEventJobRecord) globalEventJobRecord);
    }

    @Override
    @Transactional
    public void saveContextStartJobRecord(ContextStartJobRecord contextStartJobRecord) {
        logger.debug("Saving ContextStartJobRecord: {}", contextStartJobRecord.getId());
        this.contextStartJobDao.save((HibernateContextStartJobRecord) contextStartJobRecord);
    }

    @Override
    @Transactional
    public void saveContextTerminalJobRecord(ContextTerminalJobRecord contextTerminalJobRecord) {
        logger.debug("Saving ContextTerminalJobRecord: {}", contextTerminalJobRecord.getId());
        this.contextTerminalJobDao.save((HibernateContextTerminalJobRecord) contextTerminalJobRecord);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJobTemplateRecord(InternalEventDrivenJobRecord internalEventDrivenJobRecord, String modifiedBy) {
        logger.debug("Saving InternalEventDrivenJobTemplateRecord: {} by: {}", internalEventDrivenJobRecord.getId(), modifiedBy);
        internalEventDrivenJobRecord.setModifiedBy(modifiedBy);
        this.internalEventDrivenJobTemplateDao.save((HibernateInternalEventDrivenJobRecord) internalEventDrivenJobRecord);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJobTemplate(InternalEventDrivenJob internalEventDrivenJob, String modifiedBy) {
        logger.debug("Saving InternalEventDrivenJobTemplate: {} by: {}", internalEventDrivenJob.getJobName(), modifiedBy);

        String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + internalEventDrivenJob.getAgentName() + "_"
                + internalEventDrivenJob.getJobName() + "_" + internalEventDrivenJob.getContextName();

        HibernateInternalEventDrivenJobRecord record = this.internalEventDrivenJobTemplateDao.findById(id);

        internalEventDrivenJob.setTemplateJob(true);

        if (record == null) {
            record = createInternalEventDrivenJobRecord(internalEventDrivenJob, modifiedBy);
        }

        record.setModifiedBy(modifiedBy);
        record.setInternalEventDrivenJob(internalEventDrivenJob);

        this.internalEventDrivenJobTemplateDao.save(record);
    }

    @Override
    @Transactional
    public void saveFileEventDrivenJobRecords(List<FileEventDrivenJobRecord> fileEventDrivenJobRecords) {
        logger.debug("Saving {} FileEventDrivenJobRecords", fileEventDrivenJobRecords.size());
        List<HibernateFileEventDrivenJobRecord> records = this.castFileEventDrivenJobRecords(fileEventDrivenJobRecords);
        records.forEach(this.fileEventDrivenJobDao::save);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJobRecords(List<InternalEventDrivenJobRecord> internalEventDrivenJobRecords) {
        logger.debug("Saving {} InternalEventDrivenJobRecords", internalEventDrivenJobRecords.size());
        List<HibernateInternalEventDrivenJobRecord> records = this.castInternalEventDrivenJobRecords(internalEventDrivenJobRecords);
        records.forEach(this.internalEventDrivenJobDao::save);
    }

    @Override
    @Transactional
    public void saveQuartzScheduledJobRecords(List<QuartzScheduleDrivenJobRecord> quartzScheduleDrivenJobRecords) {
        logger.debug("Saving {} QuartzScheduleDrivenJobRecords", quartzScheduleDrivenJobRecords.size());
        List<HibernateQuartzScheduleDrivenJobRecord> records = this.castQuartzScheduleDrivenJobRecords(quartzScheduleDrivenJobRecords);
        records.forEach(this.quartzScheduleDrivenJobDao::save);
    }

    @Override
    @Transactional
    public void saveGlobalEventJobRecords(List<GlobalEventJobRecord> globalEventJobRecords) {
        logger.debug("Saving {} GlobalEventJobRecords", globalEventJobRecords.size());
        List<HibernateGlobalEventJobRecord> records = this.castGlobalEventJobRecords(globalEventJobRecords);
        records.forEach(this.globalEventJobDao::save);
    }

    @Override
    @Transactional
    public void saveContextStartJobRecords(List<ContextStartJobRecord> contextStartJobRecords) {
        logger.debug("Saving {} ContextStartJobRecords", contextStartJobRecords.size());
        List<HibernateContextStartJobRecord> records = this.castContextStartJobRecords(contextStartJobRecords);
        records.forEach(this.contextStartJobDao::save);
    }

    @Override
    @Transactional
    public void saveContextTerminalJobRecord(List<ContextTerminalJobRecord> contextTerminalJobRecords) {
        logger.debug("Saving {} ContextTerminalJobRecords", contextTerminalJobRecords.size());
        List<HibernateContextTerminalJobRecord> records = this.castContextTerminalJobRecords(contextTerminalJobRecords);
        records.forEach(this.contextTerminalJobDao::save);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJobTemplateRecords(List<InternalEventDrivenJobRecord> internalEventDrivenJobRecords) {
        logger.debug("Saving {} InternalEventDrivenJobTemplateRecords", internalEventDrivenJobRecords.size());
        List<HibernateInternalEventDrivenJobRecord> records = this.castInternalEventDrivenJobRecords(internalEventDrivenJobRecords);
        records.forEach(this.internalEventDrivenJobDao::save);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJobTemplates(List<InternalEventDrivenJob> internalEventDrivenJobs, String actor) {
        logger.debug("Saving {} InternalEventDrivenJobTemplates by: {}", internalEventDrivenJobs.size(), actor);
        List<HibernateInternalEventDrivenJobRecord> records = new ArrayList<>();
        internalEventDrivenJobs.forEach(job -> {
            job.setTemplateJob(true);
            records.add(createInternalEventDrivenJobRecord(job, actor));
        });
        records.forEach(this.internalEventDrivenJobDao::save);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob, String modifiedBy) {
        logger.debug("Saving InternalEventDrivenJob: {} by: {}", internalEventDrivenJob.getJobName(), modifiedBy);

        String id = JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + internalEventDrivenJob.getAgentName() + "_"
                + internalEventDrivenJob.getJobName() + "_" + internalEventDrivenJob.getContextName();

        HibernateInternalEventDrivenJobRecord record = this.internalEventDrivenJobDao.findById(id);

        if (record == null) {
            record = createInternalEventDrivenJobRecord(internalEventDrivenJob, modifiedBy);
        }

        record.setInternalEventDrivenJob(internalEventDrivenJob);

        // Determine skipped status
        AtomicBoolean skipped = new AtomicBoolean(false);
        if (internalEventDrivenJob.getSkippedContexts() != null) {
            internalEventDrivenJob.getSkippedContexts().forEach((key, value) -> {
                if (Boolean.TRUE.equals(value)) {
                    skipped.set(true);
                }
            });
        }
        record.setSkipped(skipped.get());

        // Determine held status
        AtomicBoolean held = new AtomicBoolean(false);
        if (internalEventDrivenJob.getHeldContexts() != null) {
            internalEventDrivenJob.getHeldContexts().forEach((key, value) -> {
                if (Boolean.TRUE.equals(value)) {
                    held.set(true);
                }
            });
        }
        record.setHeld(held.get());
        record.setParticipatesInLock(internalEventDrivenJob.isParticipatesInLock());
        record.setModifiedBy(modifiedBy);

        this.saveInternalEventDrivenJobRecord(record);
    }

    @Override
    @Transactional
    public void saveInternalEventDrivenJobs(List<InternalEventDrivenJob> internalEventDrivenJobs, String actor) {
        logger.debug("Saving {} InternalEventDrivenJobs by: {}", internalEventDrivenJobs.size(), actor);
        List<HibernateInternalEventDrivenJobRecord> records = new ArrayList<>();
        internalEventDrivenJobs.forEach(job -> records.add(createInternalEventDrivenJobRecord(job, actor)));
        this.saveInternalEventDrivenJobRecords(new ArrayList<>(records));
    }

    @Override
    @Transactional
    public void saveQuartzScheduledJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob, String modifiedBy) {
        logger.debug("Saving QuartzScheduleDrivenJob: {} by: {}", quartzScheduleDrivenJob.getJobName(), modifiedBy);

        String id = JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB + "_" + quartzScheduleDrivenJob.getAgentName() + "_"
                + quartzScheduleDrivenJob.getJobName() + "_" + quartzScheduleDrivenJob.getContextName();

        HibernateQuartzScheduleDrivenJobRecord record = this.quartzScheduleDrivenJobDao.findById(id);

        if (record == null) {
            record = createQuartzScheduleDrivenJobRecord(quartzScheduleDrivenJob, modifiedBy);
        }

        record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);
        record.setModifiedBy(modifiedBy);
        this.saveQuartzScheduledJobRecord(record);
    }

    @Override
    @Transactional
    public void saveQuartzScheduledJobs(List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs, String actor) {
        logger.debug("Saving {} QuartzScheduleDrivenJobs by: {}", quartzScheduleDrivenJobs.size(), actor);
        List<HibernateQuartzScheduleDrivenJobRecord> records = new ArrayList<>();
        quartzScheduleDrivenJobs.forEach(job -> records.add(createQuartzScheduleDrivenJobRecord(job, actor)));
        this.saveQuartzScheduledJobRecords(new ArrayList<>(records));
    }

    @Override
    @Transactional
    public void saveFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob, String modifiedBy) {
        logger.debug("Saving FileEventDrivenJob: {} by: {}", fileEventDrivenJob.getJobName(), modifiedBy);

        String id = JobConstants.FILE_EVENT_DRIVEN_JOB + "_" + fileEventDrivenJob.getAgentName() + "_"
                + fileEventDrivenJob.getJobName() + "_" + fileEventDrivenJob.getContextName();

        HibernateFileEventDrivenJobRecord record = this.fileEventDrivenJobDao.findById(id);

        if (record == null) {
            record = createFileEventDrivenJobRecord(fileEventDrivenJob, modifiedBy);
        }

        record.setFileEventDrivenJob(fileEventDrivenJob);
        record.setModifiedBy(modifiedBy);
        this.saveFileEventDrivenJobRecord(record);
    }

    @Override
    @Transactional
    public void saveFileEventDrivenJobs(List<FileEventDrivenJob> fileEventDrivenJobs, String actor) {
        logger.debug("Saving {} FileEventDrivenJobs by: {}", fileEventDrivenJobs.size(), actor);
        List<HibernateFileEventDrivenJobRecord> records = new ArrayList<>();
        fileEventDrivenJobs.forEach(job -> records.add(createFileEventDrivenJobRecord(job, actor)));
        this.saveFileEventDrivenJobRecords(new ArrayList<>(records));
    }

    @Override
    @Transactional
    public void saveGlobalEventJob(GlobalEventJob globalEventJob, String modifiedBy) {
        logger.debug("Saving GlobalEventJob: {} by: {}", globalEventJob.getJobName(), modifiedBy);

        String id = JobConstants.GLOBAL_EVENT_JOB + "_" + globalEventJob.getAgentName() + "_"
                + globalEventJob.getJobName() + "_" + globalEventJob.getContextName();

        HibernateGlobalEventJobRecord record = this.globalEventJobDao.findById(id);

        if (record == null) {
            record = createGlobalEventJobRecord(globalEventJob, modifiedBy);
        }

        record.setGlobalEventJob(globalEventJob);
        record.setModifiedBy(modifiedBy);
        this.saveGlobalEventJobRecord(record);
    }

    @Override
    @Transactional
    public void saveContextStartJob(ContextStartJob contextStartJob, String modifiedBy) {
        logger.debug("Saving ContextStartJob: {} by: {}", contextStartJob.getJobName(), modifiedBy);

        String id = JobConstants.CONTEXT_START_JOB + "_"
                + contextStartJob.getJobName() + "_" + contextStartJob.getContextName();

        HibernateContextStartJobRecord record = this.contextStartJobDao.findById(id);

        if (record == null) {
            record = createContextStartJobRecord(contextStartJob, modifiedBy);
        }

        record.setContextStartJob(contextStartJob);
        record.setModifiedBy(modifiedBy);
        this.saveContextStartJobRecord(record);
    }

    @Override
    @Transactional
    public void saveContextTerminalJob(ContextTerminalJob contextTerminalJob, String modifiedBy) {
        logger.debug("Saving ContextTerminalJob: {} by: {}", contextTerminalJob.getJobName(), modifiedBy);

        String id = JobConstants.CONTEXT_TERMINAL_JOB + "_"
                + contextTerminalJob.getJobName() + "_" + contextTerminalJob.getContextName();

        HibernateContextTerminalJobRecord record = this.contextTerminalJobDao.findById(id);

        if (record == null) {
            record = createContextTerminalJobRecord(contextTerminalJob, modifiedBy);
        }

        record.setContextTerminalJob(contextTerminalJob);
        record.setModifiedBy(modifiedBy);
        this.saveContextTerminalJobRecord(record);
    }

    @Override
    @Transactional
    public void saveGlobalEventJobs(List<GlobalEventJob> globalEventJobs, String actor) {
        logger.debug("Saving {} GlobalEventJobs by: {}", globalEventJobs.size(), actor);
        List<HibernateGlobalEventJobRecord> records = new ArrayList<>();
        globalEventJobs.forEach(job -> records.add(createGlobalEventJobRecord(job, actor)));
        this.saveGlobalEventJobRecords(new ArrayList<>(records));
    }

    @Override
    @Transactional
    public void saveContextStartJobs(List<ContextStartJob> contextStartJobs, String actor) {
        logger.debug("Saving {} ContextStartJobs by: {}", contextStartJobs.size(), actor);
        List<HibernateContextStartJobRecord> records = new ArrayList<>();
        contextStartJobs.forEach(job -> records.add(createContextStartJobRecord(job, actor)));
        this.saveContextStartJobRecords(new ArrayList<>(records));
    }

    @Override
    @Transactional
    public void saveContextTerminalJobs(List<ContextTerminalJob> contextTerminalJobs, String actor) {
        logger.debug("Saving {} ContextTerminalJobs by: {}", contextTerminalJobs.size(), actor);
        List<HibernateContextTerminalJobRecord> records = new ArrayList<>();
        contextTerminalJobs.forEach(job -> records.add(createContextTerminalJobRecord(job, actor)));
        this.saveContextTerminalJobRecord(new ArrayList<>(records));
    }

    @Override
    @Transactional
    public void skip(HibernateSchedulerJobRecord jobRecord, List<String> childContextNames, String actor) {
        logger.debug("Skipping job: {} for childContexts: {} by: {}", jobRecord.getId(), childContextNames, actor);

        if (jobRecord.getJob() instanceof InternalEventDrivenJob) {
            HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord(
                    (InternalEventDrivenJob) jobRecord.getJob(), actor);
            record.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobDao.skip(record, childContextNames, actor);
        } else if (jobRecord.getJob() instanceof GlobalEventJob) {
            HibernateGlobalEventJobRecord record = createGlobalEventJobRecord(
                    (GlobalEventJob) jobRecord.getJob(), actor);
            record.setTimestamp(jobRecord.getTimestamp());
            this.globalEventJobDao.skip(record, childContextNames, actor);
        } else {
            throw new IllegalArgumentException("Only internal event driven jobs and global event jobs can be skipped.");
        }
    }

    @Override
    @Transactional
    public void hold(HibernateSchedulerJobRecord jobRecord, List<String> childContextNames, String actor) {
        logger.debug("Holding job: {} for childContexts: {} by: {}", jobRecord.getId(), childContextNames, actor);

        if (jobRecord.getJob() instanceof InternalEventDrivenJob) {
            HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord(
                    (InternalEventDrivenJob) jobRecord.getJob(), actor);
            record.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobDao.hold(record, childContextNames, actor);
        } else {
            throw new IllegalArgumentException("Only internal event driven jobs can be held.");
        }
    }

    @Override
    @Transactional
    public void enable(HibernateSchedulerJobRecord jobRecord, String contextTemplateName, String actor) {
        logger.debug("Enabling job: {} for contextTemplate: {} by: {}", jobRecord.getId(), contextTemplateName, actor);

        if (jobRecord.getJob() instanceof InternalEventDrivenJob) {
            HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord(
                    (InternalEventDrivenJob) jobRecord.getJob(), actor);
            record.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobDao.enable(record, actor);
        } else if (jobRecord.getJob() instanceof GlobalEventJob) {
            GlobalEventJob job = (GlobalEventJob) jobRecord.getJob();
            if (job.getSkippedContexts() != null) {
                job.getSkippedContexts().remove(contextTemplateName);
            }
            HibernateGlobalEventJobRecord record = createGlobalEventJobRecord(job, actor);
            record.setTimestamp(jobRecord.getTimestamp());
            this.globalEventJobDao.enable(record, actor);
        } else {
            throw new IllegalArgumentException("Only internal event driven jobs and global event jobs can be enabled.");
        }
    }

    @Override
    @Transactional
    public void release(HibernateSchedulerJobRecord jobRecord, String actor) {
        logger.debug("Releasing job: {} by: {}", jobRecord.getId(), actor);

        if (jobRecord.getJob() instanceof InternalEventDrivenJob) {
            HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord(
                    (InternalEventDrivenJob) jobRecord.getJob(), actor);
            record.setTimestamp(jobRecord.getTimestamp());
            this.internalEventDrivenJobDao.release(record, actor);
        } else {
            throw new IllegalArgumentException("Only internal event driven jobs can be released.");
        }
    }

    @Override
    @Transactional
    public void releaseAll(String contextName, String actor) {
        logger.debug("Releasing all held jobs for context: {} by: {}", contextName, actor);

        List<HibernateInternalEventDrivenJobRecord> jobsToRelease = getFilteredInternalEventDrivenJobRecords(
                contextName, true, null);

        if (!jobsToRelease.isEmpty()) {
            this.internalEventDrivenJobDao.releaseAll(jobsToRelease, actor);
        }
    }

    @Override
    @Transactional
    public void enableAll(String contextName, String actor) {
        logger.debug("Enabling all skipped jobs for context: {} by: {}", contextName, actor);

        List<HibernateInternalEventDrivenJobRecord> jobsToEnable = getFilteredInternalEventDrivenJobRecords(
                contextName, null, true);

        if (!jobsToEnable.isEmpty()) {
            this.internalEventDrivenJobDao.enableAll(jobsToEnable, actor);
        }

        // Also handle global event jobs
        List<HibernateSchedulerJobRecord> globalJobsToEnable = getSkippedFilteredGlobalEventJobRecords(contextName);
        globalJobsToEnable.forEach(globalEventJobRecord ->
                this.enable(globalEventJobRecord, contextName, actor));
    }

    @Override
    @Transactional
    public void holdAll(String contextName, String actor) {
        logger.debug("Holding all jobs for context: {} by: {}", contextName, actor);

        List<HibernateInternalEventDrivenJobRecord> jobsToHold = getFilteredInternalEventDrivenJobRecords(
                contextName, null, null);

        if (!jobsToHold.isEmpty()) {
            this.internalEventDrivenJobDao.holdAll(jobsToHold, actor);
        }
    }

    @Override
    @Transactional
    public void renameContextForJobs(String oldName, String newName, String actor) {
        logger.debug("Renaming context from: {} to: {} by: {}", oldName, newName, actor);

        SearchResults<HibernateSchedulerJobRecord> schedulerJobRecords = this.findByContext(oldName, -1, -1);

        List<InternalEventDrivenJob> internalEventDrivenJobs = new ArrayList<>();
        List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs = new ArrayList<>();
        List<FileEventDrivenJob> fileEventDrivenJobs = new ArrayList<>();
        List<GlobalEventJob> globalEventJobs = new ArrayList<>();
        List<ContextStartJob> contextStartJobs = new ArrayList<>();
        List<ContextTerminalJob> contextTerminalJobs = new ArrayList<>();

        schedulerJobRecords.getResultList().forEach(schedulerJobRecord -> {
            SchedulerJob job = schedulerJobRecord.getJob();
            job.setContextName(newName);

            if (job instanceof InternalEventDrivenJob) {
                internalEventDrivenJobs.add((InternalEventDrivenJob) job);
            } else if (job instanceof FileEventDrivenJob) {
                fileEventDrivenJobs.add((FileEventDrivenJob) job);
            } else if (job instanceof QuartzScheduleDrivenJob) {
                quartzScheduleDrivenJobs.add((QuartzScheduleDrivenJob) job);
            } else if (job instanceof GlobalEventJob) {
                globalEventJobs.add((GlobalEventJob) job);
            } else if (job instanceof ContextStartJob) {
                contextStartJobs.add((ContextStartJob) job);
            } else if (job instanceof ContextTerminalJob) {
                contextTerminalJobs.add((ContextTerminalJob) job);
            }
        });

        this.deleteByContextName(oldName);
        this.saveInternalEventDrivenJobs(internalEventDrivenJobs, actor);
        this.saveQuartzScheduledJobs(quartzScheduleDrivenJobs, actor);
        this.saveFileEventDrivenJobs(fileEventDrivenJobs, actor);
        this.saveGlobalEventJobs(globalEventJobs, actor);
        this.saveContextStartJobs(contextStartJobs, actor);
        this.saveContextTerminalJobs(contextTerminalJobs, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, InternalEventDrivenJob> getCommandExecutionJobsForContext(String contextName) {
        logger.debug("Getting command execution jobs for context: {}", contextName);

        SearchResults<HibernateSchedulerJobRecord> schedulerJobRecords =
                this.schedulerJobDao.findByContextAndType(contextName, JobConstants.INTERNAL_EVENT_DRIVEN_JOB, -1, -1);

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = new HashMap<>();

        schedulerJobRecords.getResultList().forEach(record ->
                internalEventDrivenJobMap.put(record.getJob().getIdentifier(),
                        (InternalEventDrivenJob) record.getJob()));

        return internalEventDrivenJobMap;
    }

    // Helper methods to create record instances

    private HibernateInternalEventDrivenJobRecord createInternalEventDrivenJobRecord(
            InternalEventDrivenJob job, String actor) {
        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setAgentName(job.getAgentName());
        record.setJobName(job.getJobName());
        record.setContextName(job.getContextName());
        record.setInternalEventDrivenJob(job);
        record.setTimestamp(System.currentTimeMillis());
        record.setHeld(job.getHeldContexts() != null && !job.getHeldContexts().isEmpty());
        record.setSkipped(job.getSkippedContexts() != null && !job.getSkippedContexts().isEmpty());
        record.setModifiedBy(actor);
        return record;
    }

    private HibernateQuartzScheduleDrivenJobRecord createQuartzScheduleDrivenJobRecord(
            QuartzScheduleDrivenJob job, String actor) {
        HibernateQuartzScheduleDrivenJobRecord record = new HibernateQuartzScheduleDrivenJobRecord();
        record.setAgentName(job.getAgentName());
        record.setContextName(job.getContextName());
        record.setJobName(job.getJobName());
        record.setTimestamp(System.currentTimeMillis());
        record.setQuartzScheduleDrivenJob(job);
        record.setModifiedBy(actor);
        return record;
    }

    private HibernateFileEventDrivenJobRecord createFileEventDrivenJobRecord(
            FileEventDrivenJob job, String actor) {
        HibernateFileEventDrivenJobRecord record = new HibernateFileEventDrivenJobRecord();
        record.setAgentName(job.getAgentName());
        record.setJobName(job.getJobName());
        record.setContextName(job.getContextName());
        record.setTimestamp(System.currentTimeMillis());
        record.setFileEventDrivenJob(job);
        record.setModifiedBy(actor);
        return record;
    }

    private HibernateGlobalEventJobRecord createGlobalEventJobRecord(
            GlobalEventJob job, String actor) {
        HibernateGlobalEventJobRecord record = new HibernateGlobalEventJobRecord();
        record.setAgentName(job.getAgentName());
        record.setJobName(job.getJobName());
        record.setContextName(job.getContextName());
        record.setTimestamp(System.currentTimeMillis());
        record.setGlobalEventJob(job);
        record.setModifiedBy(actor);
        return record;
    }

    private HibernateContextStartJobRecord createContextStartJobRecord(
            ContextStartJob job, String actor) {
        HibernateContextStartJobRecord record = new HibernateContextStartJobRecord();
        record.setAgentName(job.getAgentName());
        record.setJobName(job.getJobName());
        record.setContextName(job.getContextName());
        record.setTimestamp(System.currentTimeMillis());
        record.setContextStartJob(job);
        record.setModifiedBy(actor);
        return record;
    }

    private HibernateContextTerminalJobRecord createContextTerminalJobRecord(
            ContextTerminalJob job, String actor) {
        HibernateContextTerminalJobRecord record = new HibernateContextTerminalJobRecord();
        record.setAgentName(job.getAgentName());
        record.setJobName(job.getJobName());
        record.setContextName(job.getContextName());
        record.setTimestamp(System.currentTimeMillis());
        record.setContextTerminalJob(job);
        record.setModifiedBy(actor);
        return record;
    }

    // Helper methods for filtering

    private List<HibernateInternalEventDrivenJobRecord> getFilteredInternalEventDrivenJobRecords(
            String contextName, Boolean held, Boolean skipped) {
        SearchResults<HibernateSchedulerJobRecord> searchResults = this.schedulerJobDao.findByContextAndType(
                contextName, JobConstants.INTERNAL_EVENT_DRIVEN_JOB, -1, -1);

        List<HibernateInternalEventDrivenJobRecord> filteredJobs = new ArrayList<>();
        searchResults.getResultList().forEach(schedulerJobRecord -> {
            if (schedulerJobRecord.getJob() instanceof InternalEventDrivenJob) {
                boolean matchesFilter = true;
                if (held != null && schedulerJobRecord.isHeld() != held) {
                    matchesFilter = false;
                }
                if (skipped != null && schedulerJobRecord.isSkipped() != skipped) {
                    matchesFilter = false;
                }

                if (matchesFilter) {
                    HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord(
                            (InternalEventDrivenJob) schedulerJobRecord.getJob(), "");
                    record.setTimestamp(schedulerJobRecord.getTimestamp());
                    filteredJobs.add(record);
                }
            }
        });

        return filteredJobs;
    }

    private List<HibernateSchedulerJobRecord> getSkippedFilteredGlobalEventJobRecords(String contextName) {
        SearchResults<HibernateSchedulerJobRecord> searchResults = this.schedulerJobDao.findByType(
                JobConstants.GLOBAL_EVENT_JOB, -1, -1);

        List<HibernateSchedulerJobRecord> filteredJobs = new ArrayList<>();
        searchResults.getResultList().forEach(schedulerJobRecord -> {
            if (schedulerJobRecord.getJob() instanceof GlobalEventJob) {
                GlobalEventJob globalEventJob = (GlobalEventJob) schedulerJobRecord.getJob();
                if (globalEventJob.getSkippedContexts() != null &&
                        globalEventJob.getSkippedContexts().containsKey(contextName)) {
                    filteredJobs.add(schedulerJobRecord);
                }
            }
        });

        return filteredJobs;
    }

    // Type casting helper methods

    private List<HibernateFileEventDrivenJobRecord> castFileEventDrivenJobRecords(
            List<FileEventDrivenJobRecord> records) {
        List<HibernateFileEventDrivenJobRecord> result = new ArrayList<>();
        records.forEach(r -> result.add((HibernateFileEventDrivenJobRecord) r));
        return result;
    }

    private List<HibernateInternalEventDrivenJobRecord> castInternalEventDrivenJobRecords(
            List<InternalEventDrivenJobRecord> records) {
        List<HibernateInternalEventDrivenJobRecord> result = new ArrayList<>();
        records.forEach(r -> result.add((HibernateInternalEventDrivenJobRecord) r));
        return result;
    }

    private List<HibernateQuartzScheduleDrivenJobRecord> castQuartzScheduleDrivenJobRecords(
            List<QuartzScheduleDrivenJobRecord> records) {
        List<HibernateQuartzScheduleDrivenJobRecord> result = new ArrayList<>();
        records.forEach(r -> result.add((HibernateQuartzScheduleDrivenJobRecord) r));
        return result;
    }

    private List<HibernateGlobalEventJobRecord> castGlobalEventJobRecords(
            List<GlobalEventJobRecord> records) {
        List<HibernateGlobalEventJobRecord> result = new ArrayList<>();
        records.forEach(r -> result.add((HibernateGlobalEventJobRecord) r));
        return result;
    }

    private List<HibernateContextStartJobRecord> castContextStartJobRecords(
            List<ContextStartJobRecord> records) {
        List<HibernateContextStartJobRecord> result = new ArrayList<>();
        records.forEach(r -> result.add((HibernateContextStartJobRecord) r));
        return result;
    }

    private List<HibernateContextTerminalJobRecord> castContextTerminalJobRecords(
            List<ContextTerminalJobRecord> records) {
        List<HibernateContextTerminalJobRecord> result = new ArrayList<>();
        records.forEach(r -> result.add((HibernateContextTerminalJobRecord) r));
        return result;
    }
}
