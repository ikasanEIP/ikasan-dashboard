package org.ikasan;

import org.ikasan.scheduled.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.service.SolrScheduledContextInstanceServiceImpl;
import org.ikasan.scheduled.service.SolrScheduledContextServiceImpl;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
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

}