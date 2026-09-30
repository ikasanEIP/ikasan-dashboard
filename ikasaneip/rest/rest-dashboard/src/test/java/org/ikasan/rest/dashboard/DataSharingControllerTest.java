package org.ikasan.rest.dashboard;

import org.ikasan.rest.dashboard.util.TestESBSearchService;
import org.ikasan.rest.dashboard.util.TestIkasanESBDocument;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

/**
 * Unit tests for DataSharingController.
 * Tests the REST endpoints for querying and counting entity data for data sharing.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DataSharingController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml",
        "/data-sharing-test-components.xml"
    }
)
public class DataSharingControllerTest extends AbstractRestMvcTest {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    TestESBSearchService esbSearchService;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Reset the search service before each test
        esbSearchService.reset();
    }

    // ========== WIRETAP TESTS ==========

    @Test
    public void test_query_wiretap_success_with_results() throws Exception {
        String uri = "/rest/data-sharing/wiretap?fromTimestamp=1000&toTimestamp=2000&limit=10&offset=0";

        // Create test documents - return 10 to match limit
        List<IkasanESBDocument> docs = createTestWiretapDocuments(10);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(100);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":100"));
        assertThat(content, containsString("\"hasMore\":true"));
        assertThat(content, containsString("\"data\""));

        // Verify the data array contains exactly 10 items (matching limit)
        // Converted wiretap data
        // Converted wiretap data

        // Verify search was called with correct parameters
        assertEquals(1000L, esbSearchService.getLastStartTime());
        assertEquals(2000L, esbSearchService.getLastEndTime());
        assertEquals(10, esbSearchService.getLastLimit());
        assertEquals(0, esbSearchService.getLastOffset());
    }

    @Test
    public void test_query_wiretap_with_module_names() throws Exception {
        String uri = "/rest/data-sharing/wiretap?fromTimestamp=1000&toTimestamp=2000&moduleNames=module1&moduleNames=module2&limit=10&offset=0";

        // Create 10 documents with specific module names that match the query (matching limit)
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String moduleName = (i % 2 == 0) ? "module1" : "module2";
            docs.add(createTestDocument("wiretap-" + i, "wiretapEvent", moduleName, "flow" + i));
        }

        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(50);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":50"));
        assertThat(content, containsString("module1"));
        assertThat(content, containsString("module2"));

        // Verify we got 10 documents back (matching limit)
        // Converted wiretap data
        // Converted wiretap data

        // Verify module names were passed
        assertNotNull(esbSearchService.getLastModuleNames());
        assertEquals(2, esbSearchService.getLastModuleNames().size());
        assertTrue(esbSearchService.getLastModuleNames().contains("module1"));
        assertTrue(esbSearchService.getLastModuleNames().contains("module2"));
    }

    @Test
    public void test_query_wiretap_without_module_names() throws Exception {
        String uri = "/rest/data-sharing/wiretap?fromTimestamp=1000&toTimestamp=2000&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestWiretapDocuments(10);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(100);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify empty set was passed for module names
        assertNotNull(esbSearchService.getLastModuleNames());
        assertTrue(esbSearchService.getLastModuleNames().isEmpty());
    }

    @Test
    public void test_query_wiretap_pagination() throws Exception {
        String uri = "/rest/data-sharing/wiretap?fromTimestamp=1000&toTimestamp=2000&limit=25&offset=50";

        List<IkasanESBDocument> docs = createTestWiretapDocuments(25);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(200);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":200"));
        assertThat(content, containsString("\"hasMore\":true"));

        // Verify pagination parameters
        assertEquals(50, esbSearchService.getLastOffset());
        assertEquals(25, esbSearchService.getLastLimit());
    }

    @Test
    public void test_query_wiretap_default_limit_and_offset() throws Exception {
        String uri = "/rest/data-sharing/wiretap?fromTimestamp=1000&toTimestamp=2000";

        List<IkasanESBDocument> docs = createTestWiretapDocuments(100);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(100);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify default parameters
        assertEquals(0, esbSearchService.getLastOffset());
        assertEquals(1000, esbSearchService.getLastLimit());
    }

    @Test
    public void test_query_wiretap_no_more_results() throws Exception {
        String uri = "/rest/data-sharing/wiretap?fromTimestamp=1000&toTimestamp=2000&limit=10&offset=0";

        // Return 5 documents (less than limit of 10) - no more results
        List<IkasanESBDocument> docs = createTestWiretapDocuments(5);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(5);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":5"));
        assertThat(content, containsString("\"hasMore\":false"));

        // Verify we got only 5 documents (all available)
        // Converted wiretap data
        // Converted wiretap data
    }

    @Test
    public void test_count_wiretap_success() throws Exception {
        String uri = "/rest/data-sharing/wiretap/count?fromTimestamp=1000&toTimestamp=2000";

        esbSearchService.setTotalResults(250);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"count\":250"));

        // Verify count used limit=0 and offset=0
        assertEquals(0, esbSearchService.getLastLimit());
        assertEquals(0, esbSearchService.getLastOffset());
    }

    @Test
    public void test_count_wiretap_with_module_names() throws Exception {
        String uri = "/rest/data-sharing/wiretap/count?fromTimestamp=1000&toTimestamp=2000&moduleNames=testModule";

        esbSearchService.setTotalResults(42);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"count\":42"));

        // Verify module name was passed
        assertNotNull(esbSearchService.getLastModuleNames());
        assertTrue(esbSearchService.getLastModuleNames().contains("testModule"));
    }

    // ========== ERROR TESTS ==========

    @Test
    public void test_query_errors_with_results() throws Exception {
        String uri = "/rest/data-sharing/errors?fromTimestamp=1000&toTimestamp=2000&limit=10&offset=0";

        // Return 10 documents to match limit
        List<IkasanESBDocument> docs = createTestErrorDocuments(10);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(75);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":75"));
        // Converted error data

        // Verify we got 10 documents back (matching limit)
        // Converted error data
        // Converted error data

    }

    @Test
    public void test_query_errors_with_module_names() throws Exception {
        String uri = "/rest/data-sharing/errors?fromTimestamp=1000&toTimestamp=2000&moduleNames=errorModule&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestErrorDocuments(10);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(30);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify module name was passed
        assertNotNull(esbSearchService.getLastModuleNames());
        assertTrue(esbSearchService.getLastModuleNames().contains("errorModule"));
    }

    @Test
    public void test_count_errors_success() throws Exception {
        String uri = "/rest/data-sharing/errors/count?fromTimestamp=1000&toTimestamp=2000";

        esbSearchService.setTotalResults(150);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"count\":150"));
    }

    // ========== EXCLUSION TESTS ==========

    @Test
    public void test_query_exclusions_with_results() throws Exception {
        String uri = "/rest/data-sharing/exclusions?fromTimestamp=1000&toTimestamp=2000&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestExclusionDocuments(10);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(30);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":30"));
        // Converted exclusion data
    }

    @Test
    public void test_query_exclusions_with_module_names() throws Exception {
        String uri = "/rest/data-sharing/exclusions?fromTimestamp=1000&toTimestamp=2000&moduleNames=mod1&moduleNames=mod2&limit=50&offset=0";

        // Create 50 documents with specific module names that match the query (matching limit)
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String moduleName = (i % 2 == 0) ? "mod1" : "mod2";
            docs.add(createTestDocument("exclusion-" + i, "exclusionEvent", moduleName, "flow" + i));
        }

        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(123);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("mod1"));
        assertThat(content, containsString("mod2"));

        // Verify all module names were passed
        assertNotNull(esbSearchService.getLastModuleNames());
        assertEquals(2, esbSearchService.getLastModuleNames().size());
        assertTrue(esbSearchService.getLastModuleNames().contains("mod1"));
        assertTrue(esbSearchService.getLastModuleNames().contains("mod2"));
    }

    @Test
    public void test_count_exclusions_success() throws Exception {
        String uri = "/rest/data-sharing/exclusions/count?fromTimestamp=1000&toTimestamp=2000";

        esbSearchService.setTotalResults(45);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"count\":45"));
    }

    // ========== REPLAY TESTS ==========

    @Test
    public void test_query_replays_with_results() throws Exception {
        String uri = "/rest/data-sharing/replays?fromTimestamp=1000&toTimestamp=2000&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestReplayDocuments(10);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(20);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":20"));
        // Converted replay data
    }

    @Test
    public void test_query_replays_with_module_names() throws Exception {
        String uri = "/rest/data-sharing/replays?fromTimestamp=1000&toTimestamp=2000&moduleNames=replayMod&limit=5&offset=0";

        List<IkasanESBDocument> docs = createTestReplayDocuments(5);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(15);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify module name was passed
        assertNotNull(esbSearchService.getLastModuleNames());
        assertTrue(esbSearchService.getLastModuleNames().contains("replayMod"));
    }

    @Test
    public void test_count_replays_success() throws Exception {
        String uri = "/rest/data-sharing/replays/count?fromTimestamp=1000&toTimestamp=2000";

        esbSearchService.setTotalResults(35);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"count\":35"));
    }

    // ========== MODULE METADATA TESTS ==========

    @Test
    public void test_query_module_metadata_with_results() throws Exception {
        String uri = "/rest/data-sharing/module-metadata?moduleNames=module1&moduleNames=module2&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestModuleMetadataDocuments(2);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(2);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":2"));
        // Converted module metadata

        // Verify identifiers (module names) were passed - for module metadata, module name IS the id
        assertNotNull(esbSearchService.getLastIdentifiers());
        assertEquals(2, esbSearchService.getLastIdentifiers().size());
        assertTrue(esbSearchService.getLastIdentifiers().contains("module1"));
        assertTrue(esbSearchService.getLastIdentifiers().contains("module2"));
    }

    @Test
    public void test_query_module_metadata_single_module() throws Exception {
        String uri = "/rest/data-sharing/module-metadata?moduleNames=singleModule&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestModuleMetadataDocuments(1);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(1);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify single identifier was passed
        assertNotNull(esbSearchService.getLastIdentifiers());
        assertEquals(1, esbSearchService.getLastIdentifiers().size());
        assertTrue(esbSearchService.getLastIdentifiers().contains("singleModule"));
    }

    @Test
    public void test_query_module_metadata_with_pagination() throws Exception {
        String uri = "/rest/data-sharing/module-metadata?moduleNames=mod1&moduleNames=mod2&limit=5&offset=10";

        List<IkasanESBDocument> docs = createTestModuleMetadataDocuments(5);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(20);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"hasMore\":true"));

        // Verify pagination parameters
        assertEquals(10, esbSearchService.getLastOffset());
        assertEquals(5, esbSearchService.getLastLimit());
    }

    @Test
    public void test_query_module_metadata_default_limit() throws Exception {
        String uri = "/rest/data-sharing/module-metadata?moduleNames=module1";

        List<IkasanESBDocument> docs = createTestModuleMetadataDocuments(1);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(1);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify default limit and offset
        assertEquals(0, esbSearchService.getLastOffset());
        assertEquals(1000, esbSearchService.getLastLimit());
    }

    // ========== CONFIGURATION TESTS ==========

    @Test
    public void test_query_configuration_with_results() throws Exception {
        String uri = "/rest/data-sharing/configuration?configurationIdentifiers=config1&configurationIdentifiers=config2&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestConfigurationDocuments(2);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(2);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":2"));
        // Converted configuration data

        // Verify identifiers (configuration ids) were passed
        assertNotNull(esbSearchService.getLastIdentifiers());
        assertEquals(2, esbSearchService.getLastIdentifiers().size());
        assertTrue(esbSearchService.getLastIdentifiers().contains("config1"));
        assertTrue(esbSearchService.getLastIdentifiers().contains("config2"));
    }

    @Test
    public void test_query_configuration_single_config() throws Exception {
        String uri = "/rest/data-sharing/configuration?configurationIdentifiers=singleConfig&limit=10&offset=0";

        List<IkasanESBDocument> docs = createTestConfigurationDocuments(1);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(1);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify single identifier was passed
        assertNotNull(esbSearchService.getLastIdentifiers());
        assertEquals(1, esbSearchService.getLastIdentifiers().size());
        assertTrue(esbSearchService.getLastIdentifiers().contains("singleConfig"));
    }

    @Test
    public void test_query_configuration_with_pagination() throws Exception {
        String uri = "/rest/data-sharing/configuration?configurationIdentifiers=cfg1&configurationIdentifiers=cfg2&limit=20&offset=5";

        List<IkasanESBDocument> docs = createTestConfigurationDocuments(20);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(100);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"hasMore\":true"));

        // Verify pagination parameters
        assertEquals(5, esbSearchService.getLastOffset());
        assertEquals(20, esbSearchService.getLastLimit());
    }

    @Test
    public void test_query_configuration_default_limit() throws Exception {
        String uri = "/rest/data-sharing/configuration?configurationIdentifiers=config1";

        List<IkasanESBDocument> docs = createTestConfigurationDocuments(1);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(1);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify default limit and offset
        assertEquals(0, esbSearchService.getLastOffset());
        assertEquals(1000, esbSearchService.getLastLimit());
    }

    @Test
    public void test_query_configuration_multiple_identifiers() throws Exception {
        String uri = "/rest/data-sharing/configuration?configurationIdentifiers=cfg1&configurationIdentifiers=cfg2&configurationIdentifiers=cfg3&limit=50&offset=0";

        List<IkasanESBDocument> docs = createTestConfigurationDocuments(3);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(3);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Verify all three identifiers were passed
        assertNotNull(esbSearchService.getLastIdentifiers());
        assertEquals(3, esbSearchService.getLastIdentifiers().size());
        assertTrue(esbSearchService.getLastIdentifiers().contains("cfg1"));
        assertTrue(esbSearchService.getLastIdentifiers().contains("cfg2"));
        assertTrue(esbSearchService.getLastIdentifiers().contains("cfg3"));
    }

    // ========== PAGINATION AND EDGE CASE TESTS ==========

    @Test
    public void test_query_with_large_offset() throws Exception {
        String uri = "/rest/data-sharing/errors?fromTimestamp=1000&toTimestamp=2000&limit=100&offset=900";

        // Return 100 documents to match limit
        List<IkasanESBDocument> docs = createTestErrorDocuments(100);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(1500);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"totalCount\":1500"));
        assertThat(content, containsString("\"hasMore\":true"));

        // Verify we got 100 documents back (matching limit)
        // Converted error data
        // Converted error data

        assertEquals(900, esbSearchService.getLastOffset());
        assertEquals(100, esbSearchService.getLastLimit());
    }

    @Test
    public void test_count_with_zero_results() throws Exception {
        String uri = "/rest/data-sharing/replays/count?fromTimestamp=1000&toTimestamp=2000";

        esbSearchService.setTotalResults(0);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"count\":0"));
    }

    @Test
    public void test_query_wiretap_at_end_of_results() throws Exception {
        String uri = "/rest/data-sharing/wiretap?fromTimestamp=1000&toTimestamp=2000&limit=10&offset=90";

        List<IkasanESBDocument> docs = createTestWiretapDocuments(10);
        esbSearchService.setResults(docs);
        esbSearchService.setTotalResults(100);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("\"hasMore\":false")); // offset 90 + limit 10 = 100, no more
    }

    // ========== HELPER METHODS ==========

    private List<IkasanESBDocument> createTestWiretapDocuments(int count) {
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestIkasanESBDocument doc = new TestIkasanESBDocument(
                "wiretap-" + i,
                "wiretapEvent",
                "module" + (i % 2 + 1),
                "flow" + i
            );
            doc.setEvent("Test wiretap payload " + i);
            doc.setComponentName("component" + i);
            docs.add(doc);
        }
        return docs;
    }

    private List<IkasanESBDocument> createTestErrorDocuments(int count) {
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestIkasanESBDocument doc = new TestIkasanESBDocument(
                "error-" + i,
                "errorOccurrence",
                "module" + (i % 2 + 1),
                "flow" + i
            );
            doc.setEvent("Error event " + i);
            doc.setErrorMessage("Error message " + i);
            doc.setErrorDetail("Error detail " + i);
            docs.add(doc);
        }
        return docs;
    }

    private List<IkasanESBDocument> createTestExclusionDocuments(int count) {
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestIkasanESBDocument doc = new TestIkasanESBDocument(
                "exclusion-" + i,
                "exclusionEvent",
                "module" + (i % 2 + 1),
                "flow" + i
            );
            doc.setEvent("Exclusion event " + i);
            doc.setErrorUri("error://uri/" + i);
            docs.add(doc);
        }
        return docs;
    }

    private List<IkasanESBDocument> createTestReplayDocuments(int count) {
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestIkasanESBDocument doc = new TestIkasanESBDocument(
                "replay-" + i,
                "replayEvent",
                "module" + (i % 2 + 1),
                "flow" + i
            );
            doc.setEvent("Replay event " + i);
            docs.add(doc);
        }
        return docs;
    }

    private List<IkasanESBDocument> createTestModuleMetadataDocuments(int count) {
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestIkasanESBDocument doc = new TestIkasanESBDocument(
                "module" + i, // For module metadata, the module name is the ID
                "moduleMetaData",
                "module" + i,
                null
            );
            doc.setEvent("{\"moduleName\":\"module" + i + "\",\"flows\":[]}");
            docs.add(doc);
        }
        return docs;
    }

    private List<IkasanESBDocument> createTestConfigurationDocuments(int count) {
        List<IkasanESBDocument> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestIkasanESBDocument doc = new TestIkasanESBDocument(
                "config-" + i, // Configuration ID is the ID
                "configurationMetaData",
                "module" + i,
                null
            );
            doc.setEvent("{\"configurationId\":\"config-" + i + "\"}");
            docs.add(doc);
        }
        return docs;
    }

    /**
     * Helper method to create a single test document with specific properties
     */
    private TestIkasanESBDocument createTestDocument(String id, String type, String moduleName, String flowName) {
        TestIkasanESBDocument doc = new TestIkasanESBDocument(id, type, moduleName, flowName);
        doc.setEvent("Test event for " + type);
        doc.setComponentName("component-" + id);
        doc.setEventId("event-" + id);
        return doc;
    }
}
