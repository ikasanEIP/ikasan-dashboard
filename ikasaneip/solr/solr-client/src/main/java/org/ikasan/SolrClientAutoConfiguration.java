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
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
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
    public SchedulerJobService solrSchedulerJobService(FileEventDrivenJobDao fileEventDrivenJobDao
        , InternalEventDrivenJobDao internalEventDrivenJobDao, QuartzScheduleDrivenJobDao quartzScheduleDrivenJobDao
        , SchedulerJobDao schedulerJobDao   ) {
        return new SolrSchedulerJobServiceImpl(fileEventDrivenJobDao
            ,internalEventDrivenJobDao, quartzScheduleDrivenJobDao
            , schedulerJobDao);
    }

    @Bean
    public InternalEventDrivenJobService internalEventDrivenJobService(InternalEventDrivenJobDao internalEventDrivenJobDao) {
        return new SolrInternalEventDrivenJobRecordServiceImpl(internalEventDrivenJobDao);
    }

    @Bean
    public FileEventDrivenJobDao fileEventDrivenJobRecordDao() {
        SolrFileEventDrivenJobDaoImpl dao = new SolrFileEventDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public InternalEventDrivenJobDao internalEventDrivenJobRecordDao() {
        SolrInternalEventDrivenJobDaoImpl dao = new SolrInternalEventDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public QuartzScheduleDrivenJobDao quartzScheduleDrivenJobRecordDao() {
        SolrQuartzScheduleDrivenJobDaoImpl dao = new SolrQuartzScheduleDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SchedulerJobDao schedulerJobRecordDao() {
        SolrSchedulerJobDaoImpl dao = new SolrSchedulerJobDaoImpl();
        dao.initStandalone(solrUrl, 30);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }
}