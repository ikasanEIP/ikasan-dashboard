package org.ikasan.job.orchestration.provision.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.provision.job.JobProvisionServiceImpl;
import org.ikasan.job.orchestration.rest.client.JobProvisionModuleRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.module.metadata.service.SolrModuleMetadataServiceImpl;
import org.ikasan.scheduled.job.dao.*;
import org.ikasan.scheduled.job.service.SolrSchedulerJobServiceImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class ContextProvisionHelperTest extends AbstractTest {

    private SolrModuleMetadataDao solrModuleMetadataDao = new SolrModuleMetadataDao();

    private SolrModuleMetadataServiceImpl moduleMetaDataService;

    private SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao = new SolrFileEventDrivenJobDaoImpl();

    private SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao = new SolrInternalEventDrivenJobDaoImpl();

    private SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao = new SolrQuartzScheduleDrivenJobDaoImpl();

    private SolrGlobalEventJobDaoImpl globalEventJobRecordDao = new SolrGlobalEventJobDaoImpl();

    private SolrSchedulerJobDaoImpl schedulerJobRecordDao = new SolrSchedulerJobDaoImpl();

    private SchedulerJobService schedulerJobService;

    @Mock
    Environment environment;

    @Before
    public void init() {
        solrModuleMetadataDao.initStandalone("http://localhost:8983/solr", 30, 10000, 10000);
        solrModuleMetadataDao.setSolrUsername("ikasan");
        solrModuleMetadataDao.setSolrPassword("1ka5an");

        moduleMetaDataService = new SolrModuleMetadataServiceImpl(solrModuleMetadataDao);
        moduleMetaDataService.setSolrUsername("ikasan");
        moduleMetaDataService.setSolrPassword("1ka5an");

        fileEventDrivenJobRecordDao.initStandalone("http://localhost:8983/solr", 30, 10000, 10000);
        fileEventDrivenJobRecordDao.setSolrUsername("ikasan");
        fileEventDrivenJobRecordDao.setSolrPassword("1ka5an");

        internalEventDrivenJobRecordDao.initStandalone("http://localhost:8983/solr", 30, 10000, 10000);
        internalEventDrivenJobRecordDao.setSolrUsername("ikasan");
        internalEventDrivenJobRecordDao.setSolrPassword("1ka5an");

        quartzScheduleDrivenJobRecordDao.initStandalone("http://localhost:8983/solr", 30, 10000, 10000);
        quartzScheduleDrivenJobRecordDao.setSolrUsername("ikasan");
        quartzScheduleDrivenJobRecordDao.setSolrPassword("1ka5an");

        globalEventJobRecordDao.initStandalone("http://localhost:8983/solr", 30, 10000, 10000);
        globalEventJobRecordDao.setSolrUsername("ikasan");
        globalEventJobRecordDao.setSolrPassword("1ka5an");

        schedulerJobRecordDao.initStandalone("http://localhost:8983/solr", 30, 10000, 10000);
        schedulerJobRecordDao.setSolrUsername("ikasan");
        schedulerJobRecordDao.setSolrPassword("1ka5an");

        schedulerJobService = new SolrSchedulerJobServiceImpl(fileEventDrivenJobRecordDao, internalEventDrivenJobRecordDao,
            quartzScheduleDrivenJobRecordDao, globalEventJobRecordDao,  null, null, schedulerJobRecordDao);

    }

    @Test
    public void provision_jobs() throws IOException {

        List<SchedulerJob> schedulerJobs = new ArrayList<>();

//        loadFileJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-369160711/jobs/file");
//        loadCommandJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-369160711/jobs/internal");
//        loadQuartzJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-369160711/jobs/quartz");
//
//        loadFileJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1436221681/jobs/file");
//        loadCommandJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1436221681/jobs/internal");
//        loadQuartzJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1436221681/jobs/quartz");
//
//        loadFileJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1447508514/jobs/file");
//        loadCommandJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1447508514/jobs/internal");
//        loadQuartzJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1447508514/jobs/quartz");

        loadFileJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1793100514/jobs/file");
        loadCommandJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1793100514/jobs/internal");
        loadQuartzJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1793100514/jobs/quartz");

        this.provisionJobs(schedulerJobs);

    }

    private void provisionJobs(List<SchedulerJob> schedulerJobs) throws JsonProcessingException {
        when(environment.getProperty("rest.module.username")).thenReturn("admin");
        when(environment.getProperty("rest.module.password")).thenReturn("admin");

        JobProvisionModuleRestServiceImpl jobProvisionModuleRestService = new JobProvisionModuleRestServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());



        JobProvisionServiceImpl jobProvisionService = new JobProvisionServiceImpl(schedulerJobService, moduleMetaDataService, jobProvisionModuleRestService);

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        SchedulerJobWrapperImpl schedulerJobWrapper = new SchedulerJobWrapperImpl();
        schedulerJobWrapper.setJobs(schedulerJobs);

        String serialised = objectMapper.writeValueAsString(schedulerJobWrapper);
        SchedulerJobWrapperImpl schedulerJobWrapper1 = objectMapper.readValue(serialised, SchedulerJobWrapperImpl.class);


        jobProvisionService.provisionJobs(schedulerJobWrapper1.getJobs(), "system");
    }

}
