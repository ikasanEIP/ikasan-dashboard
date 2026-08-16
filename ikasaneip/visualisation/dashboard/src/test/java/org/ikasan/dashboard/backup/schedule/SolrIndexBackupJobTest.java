package org.ikasan.dashboard.backup.schedule;

import org.ikasan.dashboard.backup.SolrIndexBackupJob;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.List;

public class SolrIndexBackupJobTest {

    private Mockery mockery = new Mockery()
    {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    private SolrGeneralServiceImpl solrGeneralService = mockery.mock(SolrGeneralServiceImpl.class);
    private JobExecutionContext jobExecutionContext = mockery.mock(JobExecutionContext.class);
    private ApplicationContext applicationContext = mockery.mock(ApplicationContext.class);
    private IkasanDocumentSearchResults ikasanSolrDocumentSearchResults
        = mockery.mock(IkasanDocumentSearchResults.class);

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solr_general_service() {
        new SolrIndexBackupJob(null,
            "backup-path", "cron", this.applicationContext
            , 2, 5, 200, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_backup_location() {
        new SolrIndexBackupJob(this.solrGeneralService,
            null, "cron", this.applicationContext
            , 2, 5, 200, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_cron_expression() {
        new SolrIndexBackupJob(this.solrGeneralService,
            "backup-path", null, this.applicationContext
            , 2, 5, 200, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_application_context() {
        new SolrIndexBackupJob(this.solrGeneralService,
            "backup-path", "cron", null
            , 2, 5, 200, false);
    }

    @Test
    public void test_backup_success() throws JobExecutionException {
        SolrIndexBackupJob job = new SolrIndexBackupJob(solrGeneralService,
            "backup-path", "cron", this.applicationContext
            , 2, 5, 200, false);

        mockery.checking(new Expectations(){{
            oneOf(solrGeneralService).search(with(any(String.class)), with(any(Long.class)), with(any(Long.class))
                , with(any(Integer.class)), with(any(Integer.class)), with(any(List.class)), with(any(Boolean.class))
                , with(aNull(String.class)), with(aNull(String.class)));
            will(returnValue(ikasanSolrDocumentSearchResults));
            oneOf(ikasanSolrDocumentSearchResults).getTotalNumberOfResults();
            will(returnValue(Long.valueOf(1000)));
            oneOf(solrGeneralService).backupIndex(with(any(String.class)), with(any(Integer.class)));
        }});

        job.execute(jobExecutionContext);

        mockery.assertIsSatisfied();
    }

    @Test(expected = JobExecutionException.class)
    public void test_backup_exception_invalid_index() throws JobExecutionException {
        SolrIndexBackupJob job = new SolrIndexBackupJob(solrGeneralService,
            "backup-path", "cron", this.applicationContext
            , 2, 5, 200, false);

        mockery.checking(new Expectations(){{
            exactly(5).of(solrGeneralService).search(with(any(String.class)), with(any(Long.class)), with(any(Long.class))
                , with(any(Integer.class)), with(any(Integer.class)), with(any(List.class)), with(any(Boolean.class))
                , with(aNull(String.class)), with(aNull(String.class)));
            will(throwException(new RuntimeException("invalid index!")));
        }});

        job.execute(jobExecutionContext);
    }

    @Test
    public void test_backup_exception_invalid_index_detected_but_recovers() throws JobExecutionException {
        SolrIndexBackupJob job = new SolrIndexBackupJob(solrGeneralService,
            "backup-path", "cron", this.applicationContext
            , 2, 5, 200, false);

        mockery.checking(new Expectations(){{
            exactly(3).of(solrGeneralService).search(with(any(String.class)), with(any(Long.class)), with(any(Long.class))
                , with(any(Integer.class)), with(any(Integer.class)), with(any(List.class)), with(any(Boolean.class))
                , with(aNull(String.class)), with(aNull(String.class)));
            will(throwException(new RuntimeException("invalid index!")));

            oneOf(solrGeneralService).search(with(any(String.class)), with(any(Long.class)), with(any(Long.class))
                , with(any(Integer.class)), with(any(Integer.class)), with(any(List.class)), with(any(Boolean.class))
                , with(aNull(String.class)), with(aNull(String.class)));
            will(returnValue(ikasanSolrDocumentSearchResults));
            oneOf(ikasanSolrDocumentSearchResults).getTotalNumberOfResults();
            will(returnValue(Long.valueOf(1000)));
            oneOf(solrGeneralService).backupIndex(with(any(String.class)), with(any(Integer.class)));
        }});

        job.execute(jobExecutionContext);

        mockery.assertIsSatisfied();
    }
}
