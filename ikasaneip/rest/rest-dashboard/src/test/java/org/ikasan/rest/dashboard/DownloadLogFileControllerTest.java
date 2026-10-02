package org.ikasan.rest.dashboard;

import org.ikasan.rest.dashboard.util.TestModuleMetaDataService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Comprehensive REST test class for DownloadLogFileController.
 * Tests the controller as a REST service using MockMvc.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DownloadLogFileController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration({"/substitute-components.xml"})
@TestPropertySource(properties = {"solr.install.dir="})
public class DownloadLogFileControllerTest extends AbstractRestMvcTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    TestModuleMetaDataService moduleMetaDataService;

    private String originalUserDir;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        moduleMetaDataService.clear();
        originalUserDir = System.getProperty("user.dir");
    }

    @Test
    public void test_dashboardZip_success() throws Exception {
        // Create temporary log directory
        File tempDir = tempFolder.newFolder();
        File logsDir = new File(tempDir, "logs");
        logsDir.mkdir();

        // Create some log files
        File logFile1 = new File(logsDir, "application.log");
        Files.write(logFile1.toPath(), "Log content 1\nLine 2".getBytes());
        File logFile2 = new File(logsDir, "h2.log");
        Files.write(logFile2.toPath(), "Database log content".getBytes());

        // Set system property to temp directory
        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/dashboard/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be OK", HttpStatus.OK.value(), status);

            // Verify content type
            String contentType = mvcResult.getResponse().getContentType();
            assertEquals("Content type should be octet-stream",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, contentType);

            // Verify Content-Disposition header
            String disposition = mvcResult.getResponse().getHeader("Content-Disposition");
            assertNotNull("Should have Content-Disposition header", disposition);
            assertTrue("Should be attachment", disposition.contains("attachment"));
            assertTrue("Should have filename", disposition.contains("filename=dashboardLogs"));
            assertTrue("Should have .zip extension", disposition.contains(".zip"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    public void test_dashboardZip_directory_does_not_exist() throws Exception {
        // Create temporary directory without logs folder
        File tempDir = tempFolder.newFolder();

        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/dashboard/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), status);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    public void test_dashboardZip_with_empty_logs_directory() throws Exception {
        // Create temporary log directory with no files
        File tempDir = tempFolder.newFolder();
        File logsDir = new File(tempDir, "logs");
        logsDir.mkdir();

        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/dashboard/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be OK", HttpStatus.OK.value(), status);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    public void test_dashboardZip_with_subdirectories() throws Exception {
        // Create temporary log directory with subdirectories
        File tempDir = tempFolder.newFolder();
        File logsDir = new File(tempDir, "logs");
        logsDir.mkdir();

        // Create nested directory structure
        File subDir = new File(logsDir, "archive");
        subDir.mkdir();

        File logFile1 = new File(logsDir, "application.log");
        Files.write(logFile1.toPath(), "Current log".getBytes());
        File logFile2 = new File(subDir, "old-application.log");
        Files.write(logFile2.toPath(), "Archived log".getBytes());

        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/dashboard/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be OK", HttpStatus.OK.value(), status);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    public void test_moduleZip_module_not_found() throws Exception {
        String moduleName = "nonExistentModule";
        String uri = "/rest/logs/module/zip?moduleName=" + moduleName;

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals("Status should be INTERNAL_SERVER_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(), status);
    }

    @Test
    public void test_moduleZip_missing_module_name_parameter() throws Exception {
        String uri = "/rest/logs/module/zip";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals("Status should be BAD_REQUEST",
            HttpStatus.BAD_REQUEST.value(), status);
    }

    @Test
    public void test_solrZip_success_default_directory() throws Exception {
        // Create temporary solr log directory with default structure
        File tempDir = tempFolder.newFolder();
        File solrDir = new File(tempDir, "solr");
        File serverDir = new File(solrDir, "server");
        File logsDir = new File(serverDir, "logs");
        logsDir.mkdirs();

        // Create some log files
        File logFile = new File(logsDir, "solr.log");
        Files.write(logFile.toPath(), "Solr log content".getBytes());

        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/solr/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be OK", HttpStatus.OK.value(), status);

            // Verify content type
            String contentType = mvcResult.getResponse().getContentType();
            assertEquals("Content type should be octet-stream",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, contentType);

            // Verify Content-Disposition header
            String disposition = mvcResult.getResponse().getHeader("Content-Disposition");
            assertNotNull("Should have Content-Disposition header", disposition);
            assertTrue("Should be attachment", disposition.contains("attachment"));
            assertTrue("Should have solrLogs filename", disposition.contains("filename=solrLogs-"));
            assertTrue("Should have .zip extension", disposition.contains(".zip"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    public void test_solrZip_directory_does_not_exist() throws Exception {
        // Create temporary directory without solr folder
        File tempDir = tempFolder.newFolder();

        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/solr/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), status);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    public void test_solrZip_with_empty_logs_directory() throws Exception {
        // Create temporary solr log directory with no files
        File tempDir = tempFolder.newFolder();
        File solrDir = new File(tempDir, "solr");
        File serverDir = new File(solrDir, "server");
        File logsDir = new File(serverDir, "logs");
        logsDir.mkdirs();

        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/solr/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be OK", HttpStatus.OK.value(), status);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    public void test_solrZip_with_multiple_log_files() throws Exception {
        // Create temporary solr log directory with multiple files
        File tempDir = tempFolder.newFolder();
        File solrDir = new File(tempDir, "solr");
        File serverDir = new File(solrDir, "server");
        File logsDir = new File(serverDir, "logs");
        logsDir.mkdirs();

        // Create multiple log files
        File logFile1 = new File(logsDir, "solr.log");
        Files.write(logFile1.toPath(), "Solr current log".getBytes());
        File logFile2 = new File(logsDir, "solr.log.1");
        Files.write(logFile2.toPath(), "Solr archived log 1".getBytes());
        File logFile3 = new File(logsDir, "solr_gc.log");
        Files.write(logFile3.toPath(), "GC log".getBytes());

        System.setProperty("user.dir", tempDir.getAbsolutePath());

        try {
            String uri = "/rest/logs/solr/zip";

            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

            int status = mvcResult.getResponse().getStatus();
            assertEquals("Status should be OK", HttpStatus.OK.value(), status);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }
}
