package org.ikasan.esb.service.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Utility class for downloading log files from a remote REST service and creating a zip file.
 * This class provides functionality to:
 * 1. Call a REST service to list available log files
 * 2. Download each log file
 * 3. Create a zip file containing all downloaded log files
 */
public class LogFileDownloadAndZipUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogFileDownloadAndZipUtil.class);

    private static final String LIST_LOG_FILES_PATH = "/rest/logs/listLogFiles";
    private static final String DOWNLOAD_LOG_FILE_PATH = "/rest/logs/downloadLogFile";

    private final RestTemplate restTemplate;
    private final JsonMapper jsonMapper;

    /**
     * Constructor with RestTemplate dependency injection.
     *
     * @param restTemplate the RestTemplate to use for HTTP requests
     */
    public LogFileDownloadAndZipUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.jsonMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }

    /**
     * Default constructor that creates a new RestTemplate.
     */
    public LogFileDownloadAndZipUtil() {
        this(new RestTemplate());
    }

    /**
     * Downloads all log files from the specified base URL and creates a zip file.
     * This is a convenience method that uses default settings.
     *
     * @param baseUrl the base URL of the REST service (e.g., "http://localhost:8080")
     * @param maxFileSizeInBytes the maximum file size in bytes for files to download
     * @param outputZipPath the path where the output zip file will be created
     * @return true if the operation was successful, false otherwise
     */
    public boolean downloadAndZipLogFiles(String baseUrl, long maxFileSizeInBytes, OutputStream outputZipPath) {
        return downloadAndZipLogFiles(baseUrl, maxFileSizeInBytes, outputZipPath, null, null);
    }

    /**
     * Downloads all log files from the specified base URL and creates a zip file.
     * This method performs the following steps:
     * 1. Calls the REST service to list available log files
     * 2. Creates a temporary directory for downloaded files
     * 3. Downloads each file to the temporary directory
     * 4. Creates a zip file from the downloaded files
     * 5. Cleans up the temporary directory
     *
     * @param baseUrl the base URL of the REST service (e.g., "http://localhost:8080")
     * @param maxFileSizeInBytes the maximum file size in bytes for files to download
     * @param outputZipPath the path where the output zip file will be created
     * @param username the username for basic authentication (optional, can be null)
     * @param password the password for basic authentication (optional, can be null)
     * @return true if the operation was successful, false otherwise
     */
    public boolean downloadAndZipLogFiles(String baseUrl, long maxFileSizeInBytes, OutputStream outputZipPath,
                                          String username, String password) {
        Path tempDir = null;
        try {
            // Step 1: List log files
            logger.info("Listing log files from: {}", baseUrl);
            Map<String, String> logFiles = listLogFiles(baseUrl, maxFileSizeInBytes, username, password);

            if (logFiles == null || logFiles.isEmpty()) {
                logger.warn("No log files found or service returned no content");
                return false;
            }

            logger.info("Found {} log files to download", logFiles.size());

            // Step 2: Create temporary directory for downloads
            tempDir = Files.createTempDirectory("log-download-");
            logger.debug("Created temporary directory: {}", tempDir);

            // Step 3: Download each file
            int downloadedCount = 0;
            for (Map.Entry<String, String> entry : logFiles.entrySet()) {
                String fileName = entry.getKey();
                String fullFilePath = entry.getValue();

                try {
                    downloadLogFile(baseUrl, fullFilePath, maxFileSizeInBytes,
                        tempDir.resolve(fileName).toString(), username, password);
                    downloadedCount++;
                    logger.debug("Downloaded file {}/{}: {}", downloadedCount, logFiles.size(), fileName);
                } catch (Exception e) {
                    logger.error("Failed to download file: {}", fileName, e);
                    // Continue with other files
                }
            }

            if (downloadedCount == 0) {
                logger.error("Failed to download any log files");
                return false;
            }

            logger.info("Successfully downloaded {}/{} files", downloadedCount, logFiles.size());

            // Step 4: Create zip file from downloaded files
            logger.info("Creating zip file: {}", outputZipPath);
            DirectoryZipUtil.zipDirectory(tempDir.toString(), outputZipPath);

            logger.info("Successfully created zip file with {} log files", downloadedCount);
            return true;

        } catch (Exception e) {
            logger.error("Error during log file download and zip process", e);
            return false;
        } finally {
            // Step 5: Clean up temporary directory
            if (tempDir != null) {
                cleanupTempDirectory(tempDir);
            }
        }
    }

    /**
     * Lists available log files from the REST service.
     *
     * @param baseUrl the base URL of the REST service
     * @param maxFileSizeInBytes the maximum file size in bytes
     * @param username the username for basic authentication (optional)
     * @param password the password for basic authentication (optional)
     * @return a map of file names to their full file paths, or null if the request fails
     * @throws IOException if there's an error parsing the response
     */
    public Map<String, String> listLogFiles(String baseUrl, long maxFileSizeInBytes,
                                            String username, String password) throws IOException {
        String url = UriComponentsBuilder.fromUriString(baseUrl + LIST_LOG_FILES_PATH)
            .queryParam("maxFileSize", maxFileSizeInBytes)
            .toUriString();

        logger.debug("Calling listLogFiles endpoint: {}", url);

        try {
            HttpHeaders headers = createHeaders(username, password);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse JSON response to Map<String, String>
                return jsonMapper.readValue(response.getBody(), new TypeReference<>() {});
            } else if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                logger.info("No log files available (NO_CONTENT response)");
                return null;
            } else {
                logger.warn("Unexpected response status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            throw new IOException(String.format("Failed to list log files from: [%s]", url), e);
        }
    }

    /**
     * Downloads a single log file from the REST service.
     *
     * @param baseUrl the base URL of the REST service
     * @param fullFilePath the full file path of the log file to download
     * @param maxFileSizeInBytes the maximum file size in bytes
     * @param outputFilePath the local path where the file will be saved
     * @param username the username for basic authentication (optional)
     * @param password the password for basic authentication (optional)
     * @throws IOException if there's an error downloading or saving the file
     */
    public void downloadLogFile(String baseUrl, String fullFilePath, long maxFileSizeInBytes,
                                String outputFilePath, String username, String password) throws IOException {
        String url = UriComponentsBuilder.fromUriString(baseUrl + DOWNLOAD_LOG_FILE_PATH)
            .queryParam("fullFilePath", fullFilePath)
            .queryParam("maxFileSize", maxFileSizeInBytes)
            .toUriString();

        logger.debug("Downloading log file from: {}", url);

        try {
            HttpHeaders headers = createHeaders(username, password);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Save file to disk
                Path outputPath = Paths.get(outputFilePath);
                Files.write(outputPath, response.getBody());
                logger.debug("Saved file to: {}", outputFilePath);
            } else {
                throw new IOException("Failed to download file, status: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            logger.error("Failed to download log file from: {}", url, e);
            throw new IOException("Failed to download log file: " + fullFilePath, e);
        }
    }

    /**
     * Creates HTTP headers with optional basic authentication.
     *
     * @param username the username for basic authentication (optional)
     * @param password the password for basic authentication (optional)
     * @return HttpHeaders with authentication if credentials are provided
     */
    private HttpHeaders createHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (username != null && password != null) {
            String auth = username + ":" + password;
            byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        }

        return headers;
    }

    /**
     * Cleans up the temporary directory by deleting all files and the directory itself.
     *
     * @param tempDir the temporary directory to clean up
     */
    private void cleanupTempDirectory(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete temporary file: {}", path, e);
                        }
                    });
                logger.debug("Cleaned up temporary directory: {}", tempDir);
            }
        } catch (IOException e) {
            logger.warn("Error cleaning up temporary directory: {}", tempDir, e);
        }
    }
}
