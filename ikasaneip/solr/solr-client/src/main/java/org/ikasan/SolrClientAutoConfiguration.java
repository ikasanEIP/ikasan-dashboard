package org.ikasan;

import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.context.service.SolrScheduledContextServiceImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.service.SolrScheduledContextInstanceServiceImpl;
import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.service.SolrInternalEventDrivenJobRecordServiceImpl;
import org.ikasan.scheduled.job.service.SolrSchedulerJobServiceImpl;
import org.ikasan.scheduled.joblockcache.dao.SolrJobLockCacheAuditDaoImpl;
import org.ikasan.scheduled.joblockcache.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.joblockcache.service.SolrJobLockCacheServiceImpl;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrClientAutoConfiguration {

    @Value("${solr.url}")
    private String solrUrl;

    @Value("${solr.username}")
    private String solrUsername;

    @Value("${solr.password}")
    private String solrPassword;

    @Value("${solr.joblockcacheaudit.retention.days:30}")
    private int solrJobLockCacheAuditRetentionDays;

    @Value("${solr.retention.days:30}")
    private int solrRetentionDays;

    @Bean
    public JobLockCacheService jobLockCacheService() {
        SolrJobLockCacheDaoImpl solrJobLockCacheDao = new SolrJobLockCacheDaoImpl();
        solrJobLockCacheDao.initStandalone(solrUrl, solrRetentionDays);
        solrJobLockCacheDao.setSolrUsername(solrUsername);
        solrJobLockCacheDao.setSolrPassword(solrPassword);

        SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao = new SolrJobLockCacheAuditDaoImpl();
        solrJobLockCacheAuditDao.initStandalone(solrUrl, solrJobLockCacheAuditRetentionDays);
        solrJobLockCacheAuditDao.setSolrUsername(solrUsername);
        solrJobLockCacheAuditDao.setSolrPassword(solrPassword);

        return new SolrJobLockCacheServiceImpl(solrJobLockCacheDao, solrJobLockCacheAuditDao);
    }

    @Bean
    public ScheduledContextService scheduledContextService() {
        SolrScheduledContextDaoImpl dao = new SolrScheduledContextDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);
        return new SolrScheduledContextServiceImpl(dao);
    }

    @Bean
    public ScheduledContextInstanceService scheduledContextInstanceService() {
        SolrScheduledContextInstanceDaoImpl dao = new SolrScheduledContextInstanceDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);
        return new SolrScheduledContextInstanceServiceImpl(dao);
    }

    @Bean
    public SchedulerJobService solrSchedulerJobService(SolrFileEventDrivenJobDaoImpl fileEventDrivenJobDao
        , SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobDao, SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobDao
        , SolrSchedulerJobDaoImpl schedulerJobDao) {
        return new SolrSchedulerJobServiceImpl(fileEventDrivenJobDao
            ,internalEventDrivenJobDao, quartzScheduleDrivenJobDao
            , schedulerJobDao);
    }

    @Bean
    public InternalEventDrivenJobService internalEventDrivenJobService(InternalEventDrivenJobDao internalEventDrivenJobDao) {
        return new SolrInternalEventDrivenJobRecordServiceImpl(internalEventDrivenJobDao);
    }

    @Bean
    public SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao() {
        SolrFileEventDrivenJobDaoImpl dao = new SolrFileEventDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao() {
        SolrInternalEventDrivenJobDaoImpl dao = new SolrInternalEventDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao() {
        SolrQuartzScheduleDrivenJobDaoImpl dao = new SolrQuartzScheduleDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SolrSchedulerJobDaoImpl schedulerJobRecordDao() {
        SolrSchedulerJobDaoImpl dao = new SolrSchedulerJobDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }
}