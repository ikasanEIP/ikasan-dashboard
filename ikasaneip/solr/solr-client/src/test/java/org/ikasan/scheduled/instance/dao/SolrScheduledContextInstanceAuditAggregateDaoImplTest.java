package org.ikasan.scheduled.instance.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SolrScheduledContextInstanceAuditAggregateDaoImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao;

    private Path tmpPath;
    private EmbeddedSolrServer server;

    @Before
    public void setup() throws SolrServerException, IOException {
        this.tmpPath = createTempDir();
        NodeConfig config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        this.server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        this.server.request(createRequest);

        this.scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        this.scheduledContextInstanceAuditAggregateDao.setSolrClient(this.server);

    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test
    public void test_get_repeating_job_status_count_success() {
        List<ScheduledContextInstanceAuditAggregateRecord> auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id1", "contextName",
            20, InstanceStatus.COMPLETE);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id1", "contextName",
            3, InstanceStatus.ERROR);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id2", "contextName",
            27, InstanceStatus.COMPLETE);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id2", "contextName",
            5, InstanceStatus.ERROR);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        Map<String, Map<String, Integer>> statusCounts = this.scheduledContextInstanceAuditAggregateDao
            .getRepeatingJobStatusCounts(List.of("id1", "id2"));

        Assert.assertTrue(!statusCounts.isEmpty());
        Assert.assertEquals(20, statusCounts.get("id1").get("COMPLETE").intValue());
        Assert.assertEquals(3, statusCounts.get("id1").get("ERROR").intValue());
        Assert.assertEquals(27, statusCounts.get("id2").get("COMPLETE").intValue());
        Assert.assertEquals(5, statusCounts.get("id2").get("ERROR").intValue());
    }


    private List<ScheduledContextInstanceAuditAggregateRecord> createScheduledContextInstanceAuditAggregateRecords(String contextInstanceId, String contextName
        , int numberToCreate, InstanceStatus instanceStatus) {
        List<ScheduledContextInstanceAuditAggregateRecord> records = new ArrayList<>();

        for (int i=0; i<numberToCreate; i++) {
            ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord
                = new SolrScheduledContextInstanceAuditAggregateRecordImpl();
            scheduledContextInstanceAuditAggregateRecord.setRepeatingJob(true);
            scheduledContextInstanceAuditAggregateRecord.setJobType("");
            scheduledContextInstanceAuditAggregateRecord.setContextInstanceId(contextInstanceId);
            scheduledContextInstanceAuditAggregateRecord.setStatus(instanceStatus.name());
            scheduledContextInstanceAuditAggregateRecord.setContextName(contextName);
            scheduledContextInstanceAuditAggregateRecord
                .setScheduledContextInstanceAuditAggregate(new ScheduledContextInstanceAuditAggregateImpl());
            scheduledContextInstanceAuditAggregateRecord.setScheduledProcessEventName("eventName");

            records.add(scheduledContextInstanceAuditAggregateRecord);
        }

        return records;
    }
}