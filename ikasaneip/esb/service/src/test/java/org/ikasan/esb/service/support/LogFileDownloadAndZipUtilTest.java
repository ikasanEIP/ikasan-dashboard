package org.ikasan.esb.service.support;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for LogFileDownloadAndZipUtil.
 */
public class LogFileDownloadAndZipUtilTest {

    private RestTemplate mockRestTemplate;
    private LogFileDownloadAndZipUtil util;
    private Path tempDir;
    private List<Path> createdFiles;

    @Before
    public void setUp() throws IOException {
        mockRestTemplate = mock(RestTemplate.class);
        util = new LogFileDownloadAndZipUtil(mockRestTemplate);
        tempDir = Files.createTempDirectory("log-download-test-");
        createdFiles = new ArrayList<>();
    }

    @After
    public void tearDown() throws IOException {
        // Clean up created files
        for (Path file : createdFiles) {
            if (Files.exists(file)) {
                Files.delete(file);
            }
        }

        // Clean up temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }

    @Test
    public void test_listLogFiles_success() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760; // 10MB

        // Mock response
        String jsonResponse = "{\"application.log\":\"/path/to/application.log\",\"h2.log\":\"/path/to/h2.log\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(responseEntity);

        // Execute
        Map<String, String> result = util.listLogFiles(baseUrl, maxFileSize, null, null);

        // Verify
        assertNotNull("Result should not be null", result);
        assertEquals("Should have 2 files", 2, result.size());
        assertTrue("Should contain application.log", result.containsKey("application.log"));
        assertTrue("Should contain h2.log", result.containsKey("h2.log"));
        assertEquals("Path should match", "/path/to/application.log", result.get("application.log"));
    }

    @Test
    public void test_listLogFiles_with_authentication() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        String username = "testuser";
        String password = "testpass";

        String jsonResponse = "{\"application.log\":\"/path/to/application.log\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(String.class)
        )).thenReturn(responseEntity);

        // Execute
        util.listLogFiles(baseUrl, maxFileSize, username, password);

        // Verify authentication header
        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        assertNotNull("Entity should not be null", capturedEntity);
        assertTrue("Should have Authorization header", capturedEntity.getHeaders().containsHeader("Authorization"));
        String authHeader = capturedEntity.getHeaders().getFirst("Authorization");
        assertTrue("Should be Basic auth", authHeader.startsWith("Basic "));
    }

    @Test
    public void test_listLogFiles_no_content() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;

        ResponseEntity<String> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(responseEntity);

        // Execute
        Map<String, String> result = util.listLogFiles(baseUrl, maxFileSize, null, null);

        // Verify
        assertNull("Result should be null for NO_CONTENT", result);
    }

    @Test(expected = IOException.class)
    public void test_listLogFiles_rest_client_exception() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenThrow(new RestClientException("Connection failed"));

        // Execute - should throw IOException
        util.listLogFiles(baseUrl, maxFileSize, null, null);
    }

    @Test
    public void test_downloadLogFile_success() throws IOException {
        String baseUrl = "http://localhost:8080";
        String fullFilePath = "/path/to/application.log";
        long maxFileSize = 10485760;
        Path outputFile = tempDir.resolve("downloaded.log");
        createdFiles.add(outputFile);

        // Mock response
        byte[] fileContent = "Log file content\nLine 2\nLine 3".getBytes();
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(fileContent, HttpStatus.OK);

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(byte[].class)
        )).thenReturn(responseEntity);

        // Execute
        util.downloadLogFile(baseUrl, fullFilePath, maxFileSize, outputFile.toString(), null, null);

        // Verify
        assertTrue("File should exist", Files.exists(outputFile));
        byte[] downloadedContent = Files.readAllBytes(outputFile);
        assertArrayEquals("Content should match", fileContent, downloadedContent);
    }

    @Test(expected = IOException.class)
    public void test_downloadLogFile_failed_status() throws IOException {
        String baseUrl = "http://localhost:8080";
        String fullFilePath = "/path/to/application.log";
        long maxFileSize = 10485760;
        Path outputFile = tempDir.resolve("downloaded.log");

        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(byte[].class)
        )).thenReturn(responseEntity);

        // Execute - should throw IOException
        util.downloadLogFile(baseUrl, fullFilePath, maxFileSize, outputFile.toString(), null, null);
    }

    @Test(expected = IOException.class)
    public void test_downloadLogFile_rest_client_exception() throws IOException {
        String baseUrl = "http://localhost:8080";
        String fullFilePath = "/path/to/application.log";
        long maxFileSize = 10485760;
        Path outputFile = tempDir.resolve("downloaded.log");

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(byte[].class)
        )).thenThrow(new RestClientException("Download failed"));

        // Execute - should throw IOException
        util.downloadLogFile(baseUrl, fullFilePath, maxFileSize, outputFile.toString(), null, null);
    }

    @Test
    public void test_downloadAndZipLogFiles_success() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        Path zipFile = tempDir.resolve("logs.zip");
        createdFiles.add(zipFile);

        // Mock list response
        String jsonResponse = "{\"application.log\":\"/path/to/application.log\",\"h2.log\":\"/path/to/h2.log\"}";
        ResponseEntity<String> listResponse = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        // Mock download responses
        byte[] appLogContent = "Application log content".getBytes();
        byte[] h2LogContent = "H2 log content".getBytes();
        ResponseEntity<byte[]> appLogResponse = new ResponseEntity<>(appLogContent, HttpStatus.OK);
        ResponseEntity<byte[]> h2LogResponse = new ResponseEntity<>(h2LogContent, HttpStatus.OK);

        when(mockRestTemplate.exchange(
            contains("listLogFiles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(listResponse);

        when(mockRestTemplate.exchange(
            contains("downloadLogFile"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(byte[].class)
        )).thenReturn(appLogResponse, h2LogResponse);

        // Execute
        boolean result = util.downloadAndZipLogFiles(baseUrl, maxFileSize, new FileOutputStream(zipFile.toString()));

        // Verify
        assertTrue("Operation should succeed", result);
        assertTrue("Zip file should exist", Files.exists(zipFile));
        assertTrue("Zip file should have content", Files.size(zipFile) > 0);

        // Verify zip contents
        List<String> zipEntries = getZipEntries(zipFile);
        assertTrue("Should contain application.log", zipEntries.stream().anyMatch(e -> e.contains("application.log")));
        assertTrue("Should contain h2.log", zipEntries.stream().anyMatch(e -> e.contains("h2.log")));
    }

    @Test
    public void test_downloadAndZipLogFiles_no_files() throws FileNotFoundException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        Path zipFile = tempDir.resolve("logs.zip");

        // Mock empty response
        ResponseEntity<String> listResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        when(mockRestTemplate.exchange(
            contains("listLogFiles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(listResponse);

        // Execute
        boolean result = util.downloadAndZipLogFiles(baseUrl, maxFileSize, new FileOutputStream(zipFile.toString()));

        // Verify
        assertFalse("Operation should fail when no files found", result);
//        assertFalse("Zip file should not exist", Files.exists(zipFile));
    }

    @Test
    public void test_downloadAndZipLogFiles_partial_download_failure() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        Path zipFile = tempDir.resolve("logs.zip");
        createdFiles.add(zipFile);

        // Mock list response with 2 files
        String jsonResponse = "{\"application.log\":\"/path/to/application.log\",\"h2.log\":\"/path/to/h2.log\"}";
        ResponseEntity<String> listResponse = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        // Mock first download success, second download failure
        byte[] appLogContent = "Application log content".getBytes();
        ResponseEntity<byte[]> appLogResponse = new ResponseEntity<>(appLogContent, HttpStatus.OK);

        when(mockRestTemplate.exchange(
            contains("listLogFiles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(listResponse);

        when(mockRestTemplate.exchange(
            contains("downloadLogFile"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(byte[].class)
        )).thenReturn(appLogResponse)
          .thenThrow(new RestClientException("Download failed for second file"));

        // Execute
        boolean result = util.downloadAndZipLogFiles(baseUrl, maxFileSize, new FileOutputStream(zipFile.toString()));

        // Verify - should succeed with at least 1 file
        assertTrue("Operation should succeed with partial downloads", result);
        assertTrue("Zip file should exist", Files.exists(zipFile));

        // Verify zip contains only the successful download
        List<String> zipEntries = getZipEntries(zipFile);
        assertTrue("Should contain application.log", zipEntries.stream().anyMatch(e -> e.contains("application.log")));
    }

    @Test
    public void test_downloadAndZipLogFiles_all_downloads_fail() throws FileNotFoundException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        Path zipFile = tempDir.resolve("logs.zip");

        // Mock list response
        String jsonResponse = "{\"application.log\":\"/path/to/application.log\"}";
        ResponseEntity<String> listResponse = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(mockRestTemplate.exchange(
            contains("listLogFiles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(listResponse);

        when(mockRestTemplate.exchange(
            contains("downloadLogFile"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(byte[].class)
        )).thenThrow(new RestClientException("Download failed"));

        // Execute
        boolean result = util.downloadAndZipLogFiles(baseUrl, maxFileSize, new FileOutputStream(zipFile.toString()));

        // Verify
        assertFalse("Operation should fail when all downloads fail", result);
    }

    @Test
    public void test_downloadAndZipLogFiles_with_authentication() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        Path zipFile = tempDir.resolve("logs.zip");
        createdFiles.add(zipFile);
        String username = "testuser";
        String password = "testpass";

        // Mock responses
        String jsonResponse = "{\"application.log\":\"/path/to/application.log\"}";
        ResponseEntity<String> listResponse = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        byte[] logContent = "Log content".getBytes();
        ResponseEntity<byte[]> downloadResponse = new ResponseEntity<>(logContent, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            any(Class.class)
        )).thenReturn(listResponse, downloadResponse);

        // Execute
        boolean result = util.downloadAndZipLogFiles(baseUrl, maxFileSize, new FileOutputStream(zipFile.toString()), username, password);

        // Verify
        assertTrue("Operation should succeed", result);

        // Verify all requests had authentication
        List<HttpEntity> capturedEntities = entityCaptor.getAllValues();
        for (HttpEntity<?> entity : capturedEntities) {
            assertTrue("Should have Authorization header", entity.getHeaders().containsHeader("Authorization"));
        }
    }

    @Test
    public void test_downloadAndZipLogFiles_list_exception() throws FileNotFoundException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        Path zipFile = tempDir.resolve("logs.zip");

        when(mockRestTemplate.exchange(
            contains("listLogFiles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenThrow(new RestClientException("Connection failed"));

        // Execute
        boolean result = util.downloadAndZipLogFiles(baseUrl, maxFileSize, new FileOutputStream(zipFile.toString()));

        // Verify
        assertFalse("Operation should fail on list exception", result);
//        assertFalse("Zip file should not exist", Files.exists(zipFile));
    }

    @Test
    public void test_constructor_without_rest_template() {
        LogFileDownloadAndZipUtil utilWithDefaultTemplate = new LogFileDownloadAndZipUtil();
        assertNotNull("Util should be created", utilWithDefaultTemplate);
    }

    @Test
    public void test_downloadAndZipLogFiles_convenience_method() throws IOException {
        String baseUrl = "http://localhost:8080";
        long maxFileSize = 10485760;
        Path zipFile = tempDir.resolve("logs.zip");
        createdFiles.add(zipFile);

        // Mock responses
        String jsonResponse = "{\"application.log\":\"/path/to/application.log\"}";
        ResponseEntity<String> listResponse = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        byte[] logContent = "Log content".getBytes();
        ResponseEntity<byte[]> downloadResponse = new ResponseEntity<>(logContent, HttpStatus.OK);

        when(mockRestTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(Class.class)
        )).thenReturn(listResponse, downloadResponse);

        // Execute using convenience method (without username/password)
        boolean result = util.downloadAndZipLogFiles(baseUrl, maxFileSize, new FileOutputStream(zipFile.toString()));

        // Verify
        assertTrue("Operation should succeed", result);
        assertTrue("Zip file should exist", Files.exists(zipFile));
    }

    // Helper methods

    private List<String> getZipEntries(Path zipPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
                zis.closeEntry();
            }
        }
        return entries;
    }
}
