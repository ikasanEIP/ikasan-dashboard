package org.ikasan.scheduled.instance.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.instance.model.SolrContextInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SolrScheduledContextInstanceDaoTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceDaoImpl dao;

    private NodeConfig config;

    private Path tmppath;

    @Before
    public void setup()
    {
        tmppath = createTempDir();

        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();

    }

    @After
    public void teardown() throws IOException
    {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException
    {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrScheduledContextInstanceDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus("RUNNING");
            this.dao.save(scheduledContextRecord);

            ScheduledContextInstanceRecord found = this.dao.findById(contextInstance.getId()+"_scheduledContextInstance");

            Assert.assertEquals(contextInstance.getId()+"_scheduledContextInstance", found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextInstance", found.getContextInstance().getName());
            Assert.assertEquals("RUNNING", found.getStatus());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_status_update_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus("WAITING");
            this.dao.save(scheduledContextRecord);

            ScheduledContextInstanceRecord found = this.dao.findById(contextInstance.getId()+"_scheduledContextInstance");

            Assert.assertEquals(contextInstance.getId()+"_scheduledContextInstance", found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextInstance", found.getContextInstance().getName());
            Assert.assertEquals("WAITING", found.getStatus());
            Assert.assertEquals(1000000L, found.getTimestamp());

            found.setStatus("RUNNING");

            this.dao.save(found);

            found = this.dao.findById(contextInstance.getId()+"_scheduledContextInstance");

            Assert.assertEquals(contextInstance.getId()+"_scheduledContextInstance", found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextInstance", found.getContextInstance().getName());
            Assert.assertEquals("RUNNING", found.getStatus());
            Assert.assertEquals(1000000L, found.getTimestamp());
        }
    }

    @Test
    public void test_find_by_status_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
            this.dao.save(scheduledContextRecord);

            contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
            this.dao.save(scheduledContextRecord);

            contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
            this.dao.save(scheduledContextRecord);

            contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
            this.dao.save(scheduledContextRecord);

            contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
            this.dao.save(scheduledContextRecord);

            contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
            this.dao.save(scheduledContextRecord);

            contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
            this.dao.save(scheduledContextRecord);

            contextInstance = new SolrContextInstanceImpl();
            contextInstance.setName("contextInstance");
            scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
            this.dao.save(scheduledContextRecord);

            Assert.assertEquals(1, this.dao.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ERROR)).getResultList().size());
            Assert.assertEquals(1, this.dao.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RELEASED)).getResultList().size());
            Assert.assertEquals(2, this.dao.getScheduledContextInstancesByStatus(List.of(InstanceStatus.COMPLETE)).getResultList().size());
            Assert.assertEquals(1, this.dao.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ON_HOLD)).getResultList().size());
            Assert.assertEquals(1, this.dao.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RUNNING)).getResultList().size());
            Assert.assertEquals(2, this.dao.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING)).getResultList().size());

            Assert.assertEquals(8, this.dao.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
                , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED)).getResultList().size());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
