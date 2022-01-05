package org.ikasan.dashboard.ui;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.SpringServlet;
import kotlin.jvm.functions.Function0;
import org.ikasan.dashboard.Application;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SessionAttributeConstants;
import org.ikasan.security.model.User;
import org.ikasan.security.service.UserService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.IntStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class},
    properties = {"spring.config.import=optional:configserver:"},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class UITest
{
    @Autowired
    protected ApplicationContext ctx;

    @MockBean
    protected IkasanAuthentication ikasanAuthentication;

    @MockBean
    protected UserService userService;

    @MockBean
    protected User user;

    @MockBean
    protected SolrGeneralServiceImpl solrSearchService;

    @MockBean
    protected ModuleMetaDataService moduleMetadataService;

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

        IkasanSolrDocumentSearchResults results = new IkasanSolrDocumentSearchResults(new ArrayList<>(), 0, 1L);

        Mockito.when(this.solrSearchService.search(Mockito.anySet(), Mockito.anySet(), Mockito.isNull(), Mockito.anyLong(),
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
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.WARN);

        this.setup_general_expectations();
        this.setup_expectations();

        routes = new Routes().autoDiscoverViews("org.ikasan.dashboard.ui");
        final Function0<UI> uiFactory = UI::new;
        final SpringServlet servlet = new MockSpringServlet(routes, ctx, uiFactory);
        MockVaadin.setup(uiFactory, servlet);

        UI.getCurrent().getSession().setAttribute(SessionAttributeConstants.TIMEZONE_ID,
            "Europe/London");
    }

    @After
    public void tearDown() {
        MockVaadin.tearDown();
    }

    protected IkasanSolrDocumentSearchResults getSolrResults(int size) {

        ArrayList<IkasanSolrDocument> ikasanSolrDocuments = new ArrayList<>();

        IntStream.range(0, size).forEach(i -> {
            IkasanSolrDocument document = new IkasanSolrDocument();
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
