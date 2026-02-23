package org.ikasan.dashboard.backup.schedule;

import org.ikasan.dashboard.backup.SolrIndexBackupJob;
import org.ikasan.security.service.LdapServiceException;
import org.ikasan.spec.solr.SolrGeneralService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SolrIndexBackupJobTest {

    private Mockery mockery = new Mockery()
    {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    private SolrGeneralService solrGeneralService = mockery.mock(SolrGeneralService.class);
    private JobExecutionContext jobExecutionContext = mockery.mock(JobExecutionContext.class);

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_solr_general_service() {
        new SolrIndexBackupJob(null,
            "backup-path", "cron", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_backup_location() {
        new SolrIndexBackupJob(this.solrGeneralService,
            null, "cron", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_cron_expression() {
        new SolrIndexBackupJob(this.solrGeneralService,
            "backup-path", null, 2);
    }

    @Test
    public void test_backup_success() throws JobExecutionException, LdapServiceException {
        SolrIndexBackupJob job = new SolrIndexBackupJob(solrGeneralService,
            "backup-path", "cron", 2);

        mockery.checking(new Expectations(){{
            oneOf(solrGeneralService).backupIndex(with(any(String.class)), with(any(Integer.class)));

        }});

        job.execute(jobExecutionContext);

        mockery.assertIsSatisfied();
    }
}
