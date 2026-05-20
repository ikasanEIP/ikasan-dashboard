package org.ikasan.solr.initialisation;

import org.ikasan.security.dao.SolrIkasanPrincipalDaoImpl;
import org.ikasan.security.dao.SolrPolicyDaoImpl;
import org.ikasan.security.dao.SolrRoleDaoImpl;
import org.ikasan.security.dao.SolrUserDaoImpl;
import org.ikasan.setup.service.SetupService;
import org.ikasan.solr.initialisation.core.SolrDataJob;
import org.ikasan.solr.initialisation.core.SolrDataJobManager;
import org.ikasan.solr.initialisation.security.BaselineSecurityDataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SolrInitialisationAutoConfiguration     {

    @Autowired
    private SolrPolicyDaoImpl policyDao;
    @Autowired
    private SolrRoleDaoImpl roleDao;
    @Autowired
    private SolrIkasanPrincipalDaoImpl principalDao;
    @Autowired
    private SolrUserDaoImpl userDao;
    @Autowired
    private SetupService setupService;

    private SolrDataJobManager solrDataJobManager;

    @Bean
    public SolrDataJobManager solrDataJobManager(List<SolrDataJob> solrDataJobs) {
        this.solrDataJobManager = new SolrDataJobManager(this.setupService, solrDataJobs);
        return solrDataJobManager;
    }

    @Bean
    public SolrDataJob baselineSecurityDataLoader() {
        return new BaselineSecurityDataLoader(this.policyDao, this.roleDao, this.principalDao, this.userDao);
    }

    @Bean
    public List<SolrDataJob> solrDataJobs() {
        ArrayList<SolrDataJob> solrDataJobs = new ArrayList<>();
        solrDataJobs.add(this.baselineSecurityDataLoader());

        return solrDataJobs;
    }
}