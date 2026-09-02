package org.ikasan.persistence.initialisation;

import org.ikasan.persistence.initialisation.core.DataJob;
import org.ikasan.persistence.initialisation.core.DataJobManager;
import org.ikasan.persistence.initialisation.security.BaselineSecurityDataLoader;
import org.ikasan.spec.persistence.service.SetupService;
import org.ikasan.spec.security.dao.IkasanPrincipalDao;
import org.ikasan.spec.security.dao.PolicyDao;
import org.ikasan.spec.security.dao.RoleDao;
import org.ikasan.spec.security.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class PersistenceInitialisationAutoConfiguration     {

    @Autowired
    private PolicyDao policyDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private IkasanPrincipalDao principalDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private SetupService setupService;

    @Bean
    public DataJobManager solrDataJobManager(List<DataJob> solrDataJobs) {
        return new DataJobManager(this.setupService, solrDataJobs);
    }

    @Bean
    public DataJob baselineSecurityDataLoader() {
        return new BaselineSecurityDataLoader(this.policyDao, this.roleDao, this.principalDao, this.userDao);
    }

    @Bean
    public List<DataJob> solrDataJobs() {
        ArrayList<DataJob> solrDataJobs = new ArrayList<>();
        solrDataJobs.add(this.baselineSecurityDataLoader());

        return solrDataJobs;
    }
}