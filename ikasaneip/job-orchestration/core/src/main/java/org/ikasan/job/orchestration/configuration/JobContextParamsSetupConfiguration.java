package org.ikasan.job.orchestration.configuration;

import org.apache.commons.lang3.StringUtils;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class JobContextParamsSetupConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JobContextParamsSetupConfiguration.class);

    private Map<String, String> location;
    private Map<String, Map<String, String>> paramsToReplace;
    private SpringCloudConfigRefreshService springCloudConfigRefreshService;
    private List<String> jobContextConfigRepoEnvironment;
    private String configServerUrl;

    public JobContextParamsSetupConfiguration(SpringCloudConfigRefreshService springCloudConfigRefreshService,
                                              List<String> jobContextConfigRepoEnvironment,
                                              String configServerUrl) {
        // Set empty map on creation
        this.paramsToReplace = new HashMap<>();
        
        this.springCloudConfigRefreshService = springCloudConfigRefreshService;
        this.jobContextConfigRepoEnvironment = jobContextConfigRepoEnvironment;
        this.configServerUrl = configServerUrl;
    }

    /**
     * Gets the parameters that is required for
     * @return a Map of Map parameters, the below is an example of two entries in the map
     *  {
     *    ContextName1 : {
     *      param1=value2,
     *      param2=value2
     *    }
     *  },
     *  {
     *    ContextName2 : {
     *      param1=value2,
     *      param2=value2,
     *      param3=value3
     *    }
     *  }
     */
    public Map<String, Map<String, String>> getParamsToReplace() {
        return paramsToReplace;
    }

    public void setParamsToReplace(Map<String, Map<String, String>> paramsToReplace) {
        this.paramsToReplace = paramsToReplace;
    }

    public Map<String, String> getLocation() {
        return location;
    }

    /**
     * Setter to create the bean from JobContextParamsSetupFactory
     * Key will be the name of the context
     * Value will be the location of the file that contains the parameters for the given key.
     * @param location key = context name, value = location of the file.
     */
    public void setLocation(Map<String, String> location) {
        logger.info("Location for ikasan scheduler configuration files: {}", location);
        this.location = location;

        // Call springCloudConfigRefreshService to refresh all application properties found in jobContextConfigRepoEnvironment
        if (springCloudConfigRefreshService != null && jobContextConfigRepoEnvironment != null 
            && jobContextConfigRepoEnvironment.size() != 0 && StringUtils.isNotBlank(configServerUrl)) {
            jobContextConfigRepoEnvironment.forEach(applicationPattern -> {
                springCloudConfigRefreshService.refreshConfigRepo(configServerUrl, applicationPattern);
            });
        }
        
        Map<String, Map<String, String>> contextParamsToReplace = new HashMap<>();
        if(null != this.getLocation()) {
            this.getLocation().forEach((key, value) -> {
                try {
                    logger.info("Loading configuration for context [{}] from configuration file [{}]", key, value);
                    Map<String, String> loadMapFromFileValues = loadMapFromFile(value);
                    if (!loadMapFromFileValues.isEmpty()) {
                        contextParamsToReplace.put(key, loadMapFromFileValues);
                    } else {
                        logger.warn("ATTENTION: Loading configuration for context [{}] is empty and therefore will not be added", key);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Unexpected error occurred while loading configuration file for context [" + key + "] and [file " + value +"]", e);
                }
            });
            this.setParamsToReplace(contextParamsToReplace);
        } else {
            logger.warn("No configuration defined for the property job.context.mapping.configuration.");
        }
    }

    /**
     * Creates a Map of key value pairs from a file. If also handles empty map key if it is required.
     * You can ignore lines in the file by adding an # at the beginning of the line.
     *
     * Expected format of the file could look like:
     *
     * param1=value1
     * param2=value2
     * =value4
     * #param3=value3
     *
     * @param fileName location of where the file resides
     * @return Map of key value pairs of parameters and value
     * @throws IOException file issues
     */
    private Map<String, String> loadMapFromFile(String fileName) throws IOException {
        Map<String, String> mappings = new HashMap<>();
        JobContextParamsSetupConfiguration.getLines(fileName).forEach(line -> {
            if(line.trim().isEmpty()) {
                logger.warn("Empty line ignored from the configuration file [{}]", fileName);
            }
            else if(line.trim().startsWith("#")) {
                logger.info("Line [{}] is ignored from configuration file [{}]", line, fileName);
            }
            else if (!line.contains("=")) {
                logger.warn("ATTENTION: Malformed line: Line [{}] from the file [{}] is malformed and will be ignored!", line, fileName);
            } else {
                String key=line.substring(0, line.indexOf('='));
                String value=line.substring(line.indexOf('=') + 1);
                if(null != mappings.get(key)) {
                    logger.warn("ATTENTION: DUPLICATE MAP EXISTS: Mapping for key [{}] already exists with value [{}] and would be replaced with new value [{}] from the file [{}]", key, mappings.get(key), value, fileName);
                }
                if(key.isEmpty()) {
                    logger.warn("ATTENTION: Added Mapping with empty key and with value [{}] from the file [{}]", value, fileName);
                }
                mappings.put(key, value);
            }
        });
        return mappings;
    }

    /**
     * Helper method to read lines from the configuration file
     * @param mappingFilePath path of the configuration file
     * @return Stream of String that represent the lines of the configuration file.
     * @throws IOException file issues
     */
    public static Stream<String> getLines(String mappingFilePath) throws IOException {
        // assume on classpath first time
        InputStream inputStream = JobContextParamsSetupConfiguration.class.getResourceAsStream(mappingFilePath);
        if (inputStream != null) {
            logger.info("Mapping file path [{}] found on classpath", mappingFilePath);
            return new BufferedReader(new InputStreamReader(inputStream)).lines();
        }
        logger.info("File path [{}] is not a classpath resource will use filesystem", mappingFilePath);
        try {
            return Files.lines(Paths.get(mappingFilePath));
        } catch (NoSuchFileException e) {
            logger.error("ATTENTION: File path [{}] does not exist. Skipping!", mappingFilePath);
        }
        return Stream.empty();
    }
}
