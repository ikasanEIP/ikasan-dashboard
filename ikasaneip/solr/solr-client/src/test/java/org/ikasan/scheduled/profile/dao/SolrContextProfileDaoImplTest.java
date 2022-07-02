package org.ikasan.scheduled.profile.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.profile.model.SolrContextProfileImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileRecordImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

public class SolrContextProfileDaoImplTest extends SolrTestCaseJ4 {

    private SolrContextProfileDaoImpl solrContextProfileDao;

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

        this.solrContextProfileDao = new SolrContextProfileDaoImpl();
        this.solrContextProfileDao.setSolrClient(server);

    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }


    @Test
    public void test_save_null_context_profile_null_access_users_null_access_roles_and_find_success() {
        SolrContextProfileRecordImpl solrContextProfileRecord = new SolrContextProfileRecordImpl();
        solrContextProfileRecord.setProfileName("profileName");
        solrContextProfileRecord.setContextName("contextName");
        solrContextProfileRecord.setOwner("owner");
        solrContextProfileRecord.setModifiedBy("modifiedBy");

        this.solrContextProfileDao.save(solrContextProfileRecord);

        ContextProfileRecord found = this.solrContextProfileDao.findById("profileName-contextName-contextProfile");

        Assert.assertNotNull(found);

        Assert.assertEquals("profileName", found.getProfileName());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("owner", found.getOwner());
        Assert.assertEquals(0, found.getContextProfile().getSubContexts().size());
        Assert.assertEquals(0, found.getAccessRoles().size());
        Assert.assertEquals(0, found.getAccessUsers().size());
        Assert.assertEquals("modifiedBy", found.getModifiedBy());

        Assert.assertNull(this.solrContextProfileDao.findById("bad_id"));
    }

    @Test
    public void test_save_and_find_success() {
        SolrContextProfileRecordImpl solrContextProfileRecord = new SolrContextProfileRecordImpl();
        solrContextProfileRecord.setProfileName("profileName");
        solrContextProfileRecord.setContextName("contextName");
        solrContextProfileRecord.setOwner("owner");
        solrContextProfileRecord.setModifiedBy("modifiedBy");
        solrContextProfileRecord.setAccessRoles(List.of("role1"));
        solrContextProfileRecord.setAccessUsers(List.of("user1"));

        SolrContextProfileImpl solrContextProfile = new SolrContextProfileImpl();
        solrContextProfile.setSubContexts(List.of("context1", "context2"));

        solrContextProfileRecord.setContextProfile(solrContextProfile);

        this.solrContextProfileDao.save(solrContextProfileRecord);

        ContextProfileRecord found = this.solrContextProfileDao.findById("profileName-contextName-contextProfile");

        Assert.assertNotNull(found);

        Assert.assertEquals("profileName", found.getProfileName());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("owner", found.getOwner());
        Assert.assertEquals(2, found.getContextProfile().getSubContexts().size());
        Assert.assertEquals(1, found.getAccessRoles().size());
        Assert.assertEquals(1, found.getAccessUsers().size());
        Assert.assertEquals("modifiedBy", found.getModifiedBy());

        Assert.assertNull(this.solrContextProfileDao.findById("bad_id"));
    }

    @Test
    public void test_find_by_filter() {
        this.addRecords(100);

        SearchResults<ContextProfileRecord> results = this.solrContextProfileDao.findByFilter(new SolrContextProfileSearchFilterImpl(), -1, -1, null, null);

        Assert.assertEquals(100, results.getTotalNumberOfResults());
        Assert.assertEquals(100, results.getResultList().size());

        SolrContextProfileSearchFilterImpl filter = new SolrContextProfileSearchFilterImpl();
        filter.setUser("user1");

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(11, results.getTotalNumberOfResults());
        Assert.assertEquals(11, results.getResultList().size());

        filter = new SolrContextProfileSearchFilterImpl();
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        filter = new SolrContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals(1, results.getResultList().size());

        filter = new SolrContextProfileSearchFilterImpl();
        filter.setProfileName("bad profile name");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());

        filter = new SolrContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("contextName18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals(1, results.getResultList().size());

        filter = new SolrContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("bad context name");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());

        filter = new SolrContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("contextName18");
        filter.setOwner("owner18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals(1, results.getResultList().size());

        filter = new SolrContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("contextName18");
        filter.setOwner("bad owner");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = this.solrContextProfileDao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());

    }

    @Test
    @Ignore
    public void test() {
        SolrContextProfileDaoImpl dao = new SolrContextProfileDaoImpl();
        dao.initStandalone("http://localhost:8983/solr", 30);
        dao.setSolrUsername("ikasan");
        dao.setSolrPassword("1ka5an");

        SolrContextProfileRecordImpl solrContextProfileRecord = new SolrContextProfileRecordImpl();
        solrContextProfileRecord.setProfileName("AC_SYSTEM_PROFILE");
        solrContextProfileRecord.setContextName("-1793100514");
        solrContextProfileRecord.setOwner(ContextProfileRecord.SYSTEM_OWNER);
        solrContextProfileRecord.setModifiedBy("admin");
        solrContextProfileRecord.setAccessRoles(List.of());
        solrContextProfileRecord.setAccessUsers(List.of());

        SolrContextProfileImpl solrContextProfile = new SolrContextProfileImpl();
        solrContextProfile.setSubContexts(List.of("CONTEXT-1436221681", "CONTEXT-1447508514", "CONTEXT-369160711", "CONTEXT-1677625082", "CONTEXT--2014137964", "CONTEXT-1500699512"));

        solrContextProfileRecord.setContextProfile(solrContextProfile);

        dao.save(solrContextProfileRecord);

    }

    private void addRecords(int num) {
        IntStream.range(0, num).forEach(i -> {
            SolrContextProfileRecordImpl solrContextProfileRecord = new SolrContextProfileRecordImpl();
            solrContextProfileRecord.setProfileName("profileName"+i);
            solrContextProfileRecord.setContextName("contextName"+i);
            solrContextProfileRecord.setOwner("owner"+i);
            solrContextProfileRecord.setModifiedBy("modifiedBy");
            solrContextProfileRecord.setAccessRoles(List.of("role"+i));
            solrContextProfileRecord.setAccessUsers(List.of("user"+i));

            SolrContextProfileImpl solrContextProfile = new SolrContextProfileImpl();
            solrContextProfile.setSubContexts(List.of("context1", "context2"));

            solrContextProfileRecord.setContextProfile(solrContextProfile);

            this.solrContextProfileDao.save(solrContextProfileRecord);
        });
    }


//    protected String loadDataFile(String fileName) throws IOException
//    {
//        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");
//
//        return contentToSend;
//    }
//
//    protected InputStream loadDataFileStream(String fileName) throws IOException
//    {
//        return getClass().getResourceAsStream(fileName);
//    }
}