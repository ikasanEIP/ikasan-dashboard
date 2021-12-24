package org.ikasan;

import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.context.service.SolrScheduledContextServiceImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.service.SolrScheduledContextInstanceServiceImpl;
import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobRecordDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobRecordDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobRecordDaoImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobRecordDaoImpl;
import org.ikasan.scheduled.job.service.SolrSchedulerJobServiceImpl;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobRecordDao;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
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

    @Bean
    public ScheduledContextService scheduledContextService()
    {
        SolrScheduledContextDaoImpl dao = new SolrScheduledContextDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);
        SolrScheduledContextServiceImpl service = new SolrScheduledContextServiceImpl(dao);

        return service;
    }

    @Bean
    public ScheduledContextInstanceService scheduledContextInstanceService()
    {
        SolrScheduledContextInstanceDaoImpl dao = new SolrScheduledContextInstanceDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);
        SolrScheduledContextInstanceServiceImpl service = new SolrScheduledContextInstanceServiceImpl(dao);

        return service;
    }

    @Bean
    public SchedulerJobService solrSchedulerJobService(FileEventDrivenJobRecordDao fileEventDrivenJobRecordDao
        , InternalEventDrivenJobRecordDao internalEventDrivenJobRecordDao, QuartzScheduleDrivenJobRecordDao quartzScheduleDrivenJobRecordDao
        , SchedulerJobRecordDao schedulerJobRecordDao   ) {
        return new SolrSchedulerJobServiceImpl(fileEventDrivenJobRecordDao
            ,internalEventDrivenJobRecordDao, quartzScheduleDrivenJobRecordDao
            , schedulerJobRecordDao);
    }

    @Bean
    public FileEventDrivenJobRecordDao fileEventDrivenJobRecordDao() {
        SolrFileEventDrivenJobRecordDaoImpl dao = new SolrFileEventDrivenJobRecordDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public InternalEventDrivenJobRecordDao internalEventDrivenJobRecordDao() {
        SolrInternalEventDrivenJobRecordDaoImpl dao = new SolrInternalEventDrivenJobRecordDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public QuartzScheduleDrivenJobRecordDao quartzScheduleDrivenJobRecordDao() {
        SolrQuartzScheduleDrivenJobRecordDaoImpl dao = new SolrQuartzScheduleDrivenJobRecordDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SchedulerJobRecordDao schedulerJobRecordDao() {
        SolrSchedulerJobRecordDaoImpl dao = new SolrSchedulerJobRecordDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }
}