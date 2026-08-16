package org.ikasan.dashboard.ui;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.SpringServlet;
import kotlin.jvm.functions.Function0;
import org.apache.commons.io.FileUtils;
import org.ikasan.dashboard.Application;
import org.ikasan.dashboard.beans.DashboardComponentFactory;
import org.ikasan.dashboard.cache.ModuleMetadataCache;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SessionAttributeConstants;
import org.ikasan.job.orchestration.context.recovery.ContextInstanceRecoveryManager;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.housekeeping.HousekeepService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.ikasan.spec.security.model.User;
import org.ikasan.spec.security.service.UserService;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.BindMode;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.IntStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class},
    properties = {"spring.config.import=optional:configserver:"},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class UITest {

    // Defaults to /tmp so CI (Travis) needs no changes. Docker Desktop only shares /home
    // by default, so override locally: -Ddashboard.uitest.solr.data.dir=$HOME/solr-data (see
    // surefire systemPropertyVariables in dashboard pom.xml, which forwards this property into
    // the test JVM). Deliberately not "solr.data.dir" - that name is the stock Apache Solr
    // <dataDir>${solr.data.dir:}</dataDir> convention used by EmbeddedSolrServer tests elsewhere
    // (e.g. ikasan-solr-client); setting it globally would redirect those unrelated tests' Solr
    // cores into this directory too.
    private static final String SOLR_DATA_DIR = System.getProperty("dashboard.uitest.solr.data.dir", "/tmp/solr-data");

    static SolrContainer solr = new SolrContainer("solr:9.10.1");
    public static MongoDBContainer mongoDBContainer;

    static {
        try {
            File solrDataDir = new File(SOLR_DATA_DIR);
            FileUtils.forceMkdir(solrDataDir);
            FileUtils.cleanDirectory(solrDataDir);

            URL schemaUrl = Thread.currentThread().getContextClassLoader()
                .getResource("./solr/ikasan/conf/managed-schema.xml");
            URL solrConfigUrl = Thread.currentThread().getContextClassLoader()
                .getResource("./solr/ikasan/conf/solrconfig.xml");
            URL solrConfigDir = Thread.currentThread().getContextClassLoader()
                .getResource("./solr/ikasan/conf");

            System.out.println("Solr schema: " + schemaUrl.getPath());
            System.out.println("Solr config: " + solrConfigUrl.getPath());
            System.out.println("Solr configuration directory: " + solrConfigDir.getPath());

            solr.withCommand("solr-precreate ikasan")
                .withCollection("ikasan")
                .withConfiguration("configset", solrConfigUrl)
                .withCopyFileToContainer(
                    MountableFile.forHostPath(solrConfigDir.getPath()),
                    "/var/solr/data/ikasan/conf"
                )
                .withFileSystemBind(SOLR_DATA_DIR,
                    "/var/solr/data/ikasan", BindMode.READ_WRITE)
                .withZookeeper(false)
                .withSchema(schemaUrl);

            solr.start();
            System.out.println("Solr started!");

            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();

        } catch (IOException e) {
            System.out.println("Error starting SOLR! " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("solr.url", () -> "http://" + solr.getHost() + ":" + solr.getSolrPort() + "/solr");
    }

    @Autowired
    protected ApplicationContext ctx;

    @MockitoBean
    protected IkasanAuthentication ikasanAuthentication;

    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected User user;

    @MockitoBean
    protected ESBSearchService esbSearchService;

    @MockitoBean
    protected HousekeepService housekeepService;

    @MockitoBean
    public ModuleMetaDataService moduleMetadataService;

    @MockitoBean
    protected ContextInstanceRecoveryManager contextInstanceRecoveryManager;

    @MockitoBean
    protected ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;

    public abstract void setup_expectations() throws IOException;

    protected void setup_general_expectations() {
        // Setup the mock authentication.
        SecurityContextHolder.getContext().setAuthentication(this.ikasanAuthentication);

        // Mock some of the requisite behaviour.
        Mockito.when(this.ikasanAuthentication.getName())
            .thenReturn("username");
        Mockito.when(this.ikasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(this.userService.loadUserByUsername("username"))
            .thenReturn(user);
        Mockito.when(user.isRequiresPasswordChange())
            .thenReturn(false);

        IkasanDocumentSearchResults results = new IkasanSolrDocumentSearchResults(new ArrayList<>(), 0, 1L);

        Mockito.when(this.esbSearchService.search(Mockito.anySet(), Mockito.anySet(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.anyLong(), Mockito.anyInt(), Mockito.anyList(), Mockito.anyBoolean(), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(results);

        ModuleMetadataSearchResults moduleMetadataSearchResults = new ModuleMetadataSearchResults(new ArrayList<>(), 0, 0);


        Mockito.when(this.moduleMetadataService.find(Mockito.anyList(), Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(moduleMetadataSearchResults);

    }

    private static Routes routes;

    @BeforeAll
    public static void discoverRoutes() {
    }


    @Before
    public void setup() throws IOException {
        DashboardComponentFactory.testContext();

        this.setup_general_expectations();
        this.setup_expectations();

        routes = new Routes().autoDiscoverViews("org.ikasan.dashboard.ui");
        final Function0<UI> uiFactory = UI::new;
        final SpringServlet servlet = new MockSpringServlet(routes, ctx, uiFactory);
        MockVaadin.setup(uiFactory, servlet);

        UI.getCurrent().getSession().setAttribute(SessionAttributeConstants.TIMEZONE_ID,
            "Europe/London");

        ModuleMetadataCache.instance().reset();
    }

    @After
    public void tearDown() {
        MockVaadin.tearDown();
    }

    protected IkasanDocumentSearchResults getSolrResults(int size) {

        ArrayList<IkasanESBDocument> ikasanSolrDocuments = new ArrayList<>();

        IntStream.range(0, size).forEach(i -> {
            IkasanESBDocument document = new IkasanSolrDocument();
            document.setId("id" +i);
            document.setComponentName("component"+i);
            document.setErrorAction("exclusion"+i);
            document.setType("exclusion"+i);
            document.setErrorDetail("error"+i);
            document.setErrorUri("uri"+i);
            document.setErrorMessage("message"+i);
            document.setFlowName("flow"+i);
            document.setModuleName("module"+i);
            document.setPayloadRaw("payload".getBytes());
            document.setExceptionClass("exception.class");
            document.setEvent("event"+i);
            document.setEventId("eventId"+i);

            ikasanSolrDocuments.add(document);
        });

        return new IkasanSolrDocumentSearchResults(ikasanSolrDocuments
            , ikasanSolrDocuments.size(), 1);
    }

}
