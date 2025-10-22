package org.ikasan.orchestration.service.context.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.ikasan.job.orchestration.builder.util.ContextTemplateUtils;
import org.ikasan.job.orchestration.model.profile.ContextProfileSearchFilterImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.ikasan.job.orchestration.util.ContextImportExportConstants.*;
import static org.ikasan.job.orchestration.util.ContextImportZipUtils.*;


public final class ContextExportZipUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ContextExportZipUtils.class);

    // This property can be used to switch on subcontext splitting, it may be obsoleted if it makes more sense
    // to call the required export method directly.
    @Value("${is.ikasan.enterprise.scheduler.context.export.split.subcontext:false}")
    private static boolean splitSubcontextToSeparateFiles;

    public static String getExportZipFileName(String contextName) {
        return contextName + ".zip";
    }

    /**
     * Return a zip file stream, given the parameters below.
     * This interface provides reverse compatibility with the existing production version and will
     * no doubt need to be maintained for all live users that have their bitbucket job plans based on the
     * unsplit sub-context approach.
     * @param context containing all the information to be persisted out to the zip file.
     * @param unmodifiedContextName used in queries.
     * @param downloadName i.e. the name of the zip file.
     * @param workingDirectory to use for zipping.
     * @param schedulerJobService used to serialize the parts of the context into JSON strings.
     * @param emailNotificationDetailsService to source details for email notifications.
     * @param emailNotificationContextService to source details for email notifications.
     * @param contextProfileService to source details for profiles.
     * @param searchLimit for the number of Scheduler jobs returned by the backend.
     * @param addReplacementTokens so that some actual details e.g. agent name, are replaced by a token e.g. [[agent.name]].
     * @return A stream that can be used to push the 'zip' file to its final destination.
     */
    public static ByteArrayOutputStream createZipFile(ContextTemplate context,
                                                      String unmodifiedContextName,
                                                      String downloadName,
                                                      String workingDirectory,
                                                      SchedulerJobService schedulerJobService,
                                                      EmailNotificationDetailsService emailNotificationDetailsService,
                                                      EmailNotificationContextService emailNotificationContextService,
                                                      ContextProfileService contextProfileService,
                                                      int searchLimit,
                                                      boolean addReplacementTokens) {
        return createZipFile(context,
            unmodifiedContextName,
            downloadName,
            workingDirectory,
            schedulerJobService,
            emailNotificationDetailsService,
            emailNotificationContextService,
            contextProfileService,
            searchLimit,
            addReplacementTokens,
            splitSubcontextToSeparateFiles);
    }

    /**
     * Creates a zip file containing various components related to the given context.
     *
     * @param context the ContextTemplate object representing the context to be included in the zip file
     * @param unmodifiedContextName the unmodified name of the context for file naming purposes
     * @param downloadName the name under which the zip file will be downloaded
     * @param workingDirectory the root working directory for storing temporary files
     * @param schedulerJobService the service for managing scheduler jobs
     * @param emailNotificationDetailsService the service for managing email notification details
     * @param emailNotificationContextService the service for managing email notification contexts
     * @param contextProfileService the service for managing context profiles
     * @param searchLimit the limit for fetching search results
     * @param addReplacementTokens flag indicating whether replacement tokens should be added
     * @param separateSubContextsWhenPersisting flag indicating whether to separate subcontexts when persisting
     * @return a ByteArrayOutputStream containing the zip file data
     */
    public static ByteArrayOutputStream createZipFile(ContextTemplate context,
                                                      String unmodifiedContextName,
                                                      String downloadName,
                                                      String workingDirectory,
                                                      SchedulerJobService schedulerJobService,
                                                      EmailNotificationDetailsService emailNotificationDetailsService,
                                                      EmailNotificationContextService emailNotificationContextService,
                                                      ContextProfileService contextProfileService,
                                                      int searchLimit,
                                                      boolean addReplacementTokens,
                                                      boolean separateSubContextsWhenPersisting) {
        try {
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            ContextTemplate unmodifiedContextTemplate = SerializationUtils.clone(context);

            // Export with pretty lines
            if(addReplacementTokens) {
                ContextHelper.addContextTemplateReplacementTokens(context);
            }

            // sanitise the contextName as this will be used for the filename and that windows do not allow for certain characters
            String rootContextFileName = sanitiseForUseAsFilename(downloadName);

            String topLevelDirectory = getWorkingDirectory(workingDirectory) + rootContextFileName;
            // clean up the working directory if it exists
            deleteWorkingDirectory(topLevelDirectory);
            String contextDir = topLevelDirectory + File.separator + CONTEXT_DIR;
            Path contextDirPath = mkdir(contextDir);

            if (separateSubContextsWhenPersisting) {
                context.setOrdinal(0);
                writeContextRoots(objectMapper, contextDir, context);
            } else {
                // create the whole context template as json
                String template = objectMapper.writeValueAsString(context);
                Path contextFilePath = Paths.get(contextDirPath + File.separator + rootContextFileName + ".json");
                Files.createFile(contextFilePath);
                Files.write(contextFilePath, template.getBytes());
            }

            Path jobsDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR);
            Path jobsFileDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + FILE_DIR);
            Path jobsInternalDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + INTERNAL_DIR);
            Path jobsInternalTemplateDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + INTERNAL_TEMPLATES_DIR);
            Path jobsQuartzDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + QUARTZ_DIR);
            Path jobsGlobalDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + GLOBAL_JOB_DIR);
            Path notificationDir = Paths.get(topLevelDirectory + File.separator + NOTIFICATION_DIR);
            Path notificationDetailDir = Paths.get(topLevelDirectory + File.separator + NOTIFICATION_DETAILS_DIR);
            Path profilesDir = Paths.get(topLevelDirectory+ File.separator + PROFILE_DIR);

            Files.createDirectories(jobsDir);
            Files.createDirectories(jobsFileDir);
            Files.createDirectories(jobsInternalDir);
            Files.createDirectories(jobsInternalTemplateDir);
            Files.createDirectories(jobsQuartzDir);
            Files.createDirectories(jobsGlobalDir);
            Files.createDirectories(notificationDir);
            Files.createDirectories(notificationDetailDir);
            Files.createDirectories(profilesDir);

            int offset = 0;
            SearchResults<SchedulerJobRecord> results = schedulerJobService.findByContext(unmodifiedContextTemplate.getName(), searchLimit, offset);

            List<SchedulerJob> jobsInPlan = getJobsInPlanWithIncludedGlobalEventJobs(unmodifiedContextTemplate, results.getResultList().stream()
                .map(schedulerJobRecord -> schedulerJobRecord.getJob())
                .collect(Collectors.toList()));

            // get all the jobs
            addJobFilesToZip(objectMapper, jobsFileDir, jobsInternalDir, jobsQuartzDir, jobsGlobalDir, jobsInternalTemplateDir
                , jobsInPlan
                , addReplacementTokens);

            int retrievedNumber = results.getResultList().size();
            long totalNumberOfResults = results.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                results = schedulerJobService.findByContext(unmodifiedContextTemplate.getName(), searchLimit, offset);
                jobsInPlan = getJobsInPlanWithIncludedGlobalEventJobs(unmodifiedContextTemplate, results.getResultList().stream()
                    .map(schedulerJobRecord -> schedulerJobRecord.getJob())
                    .collect(Collectors.toList()));

                addJobFilesToZip(objectMapper, jobsFileDir, jobsInternalDir, jobsQuartzDir, jobsGlobalDir, jobsInternalTemplateDir
                    , jobsInPlan
                    , addReplacementTokens);
            }

            // get the overall notifications settings for the context
            offset = 0;
            SearchResults<EmailNotificationContextRecord> notificationResults = emailNotificationContextService.findByContextName(unmodifiedContextName, searchLimit, offset);
            addNotificationFilesToZip(objectMapper, notificationDir, notificationResults);

            retrievedNumber = notificationResults.getResultList().size();
            totalNumberOfResults = notificationResults.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                notificationResults = emailNotificationContextService.findByContextName(unmodifiedContextName, searchLimit, offset);
                addNotificationFilesToZip(objectMapper, notificationDir, notificationResults);
            }

            // get the notification details for the context - these are the individual notification defined per context, child context and job.
            offset = 0;
            SearchResults<EmailNotificationDetailsRecord> notificationDetailResults = emailNotificationDetailsService.findByContextName(unmodifiedContextName, searchLimit, offset);
            addNotificationDetailFilesToZip(objectMapper, notificationDetailDir, notificationDetailResults);

            retrievedNumber = notificationDetailResults.getResultList().size();
            totalNumberOfResults = notificationDetailResults.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                notificationDetailResults = emailNotificationDetailsService.findByContextName(unmodifiedContextName, searchLimit, offset);
                addNotificationDetailFilesToZip(objectMapper, notificationDetailDir, notificationDetailResults);
            }

            // get the profiles for the context
            offset = 0;
            ContextProfileSearchFilter profileFilter = new ContextProfileSearchFilterImpl();
            profileFilter.setContextName(unmodifiedContextName); //search by contextName

            SearchResults<ContextProfileRecord> profileResults = contextProfileService.findByFilter(profileFilter, searchLimit, offset, null, null);
            addProfilesFilesToZip(objectMapper, profilesDir, profileResults);

            retrievedNumber = profileResults.getResultList().size();
            totalNumberOfResults = profileResults.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                profileResults = contextProfileService.findByFilter(profileFilter, searchLimit, offset, null, null);
                addProfilesFilesToZip(objectMapper, profilesDir, profileResults);
            }

            // create the outputstream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            File fileToZip = new File(getWorkingDirectory(workingDirectory) + rootContextFileName);
            zipDirectory(fileToZip, fileToZip.getName(), zipOut);

            // close the streams
            zipOut.close();
            baos.close();

            // clean up everything in case there is anything there
            deleteWorkingDirectory(getWorkingDirectory(workingDirectory) + rootContextFileName);

            return baos;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn(String.format("Got exception creating zip file. Error [%s]", e.getMessage()));
            // clean up everything in case there is anything there
            try {
                deleteWorkingDirectory(getWorkingDirectory(workingDirectory) +
                    sanitiseForUseAsFilename(context.getName()));
            } catch (IOException ex) {}

            return null;
        }
    }


    /**
     * Creates a simple zip file stream from a job list with no notifications or profiles.
     * This service is used by the maven archetype in the ikasan service job plan builder demo
     * @param context containing most of the information to persist
     * @param workingDirectory to used to perform the local zip in
     * @param schedulerJobList to include
     * @param addReplacementTokens so that some actual details e.f. agent name are replaced by a token e.g. [[agent.name]]
     * @return A stream that can be used to push the 'zip' file to its final destination.
     */
    public static ByteArrayOutputStream createZipFile(ContextTemplate context,
                                                      String workingDirectory,
                                                      List<SchedulerJob> schedulerJobList,
                                                      boolean addReplacementTokens) {
        return createZipFile(context,
                            workingDirectory,
                            schedulerJobList,
                            addReplacementTokens,
                            splitSubcontextToSeparateFiles);
    }

    /**
     * Creates a zip file containing the context template and related job files.
     *
     * @param context                          The ContextTemplate object to be included in the zip file
     * @param workingDirectory                 The base directory where the zip file will be created
     * @param schedulerJobList                 List of SchedulerJob objects to be included in the zip file
     * @param addReplacementTokens             Flag to indicate whether to add replacement tokens to the context
     * @param seperateSubcontextsWhenPersisting Flag to indicate whether to separate subcontexts when persisting
     * @return ByteArrayOutputStream containing the zip file data, or null if an error occurs
     */
    public static ByteArrayOutputStream createZipFile(ContextTemplate context,
                                                      String workingDirectory,
                                                      List<SchedulerJob> schedulerJobList,
                                                      boolean addReplacementTokens,
                                                      boolean seperateSubcontextsWhenPersisting) {
        try {
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Export with pretty lines

            List<SchedulerJob> jobsInPlan = getJobsInPlanWithIncludedGlobalEventJobs(context, schedulerJobList);

            if(addReplacementTokens) {
                ContextHelper.addContextTemplateReplacementTokens(context);
            }

            String contextFileName = sanitiseForUseAsFilename(context.getName());
            // sanitise the contextName as this will be used for the filename and that windows do not allow for certain character
            String topLevelDirectory = getWorkingDirectory(workingDirectory) + contextFileName;
            // clean up the working directory if it exists
            deleteWorkingDirectory(topLevelDirectory);

            String contextDir = topLevelDirectory + File.separator + CONTEXT_DIR;
            Path contextDirPath = mkdir(contextDir);

            if (seperateSubcontextsWhenPersisting) {
                writeContextRoots(objectMapper, contextDir, context);
            } else {
                // create the whole context template as json
                String template = objectMapper.writeValueAsString(context);
                Path contextFilePath = Paths.get(contextDirPath + File.separator + contextFileName + ".json");
                Files.createFile(contextFilePath);
                Files.write(contextFilePath, template.getBytes());
            }

            Path jobsDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR);
            Path jobsFileDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + FILE_DIR);
            Path jobsInternalDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + INTERNAL_DIR);
            Path jobsInternalTemplateDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + INTERNAL_TEMPLATES_DIR);
            Path jobsQuartzDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + QUARTZ_DIR);
            Path jobsGlobalDir = Paths.get(topLevelDirectory + File.separator + JOBS_DIR + File.separator + GLOBAL_JOB_DIR);
            Path notificationDir = Paths.get(topLevelDirectory + File.separator + NOTIFICATION_DIR);
            Path notificationDetailDir = Paths.get(topLevelDirectory + File.separator + NOTIFICATION_DETAILS_DIR);
            Path profilesDir = Paths.get(topLevelDirectory + File.separator + PROFILE_DIR);

            Files.createDirectories(jobsDir);
            Files.createDirectories(jobsFileDir);
            Files.createDirectories(jobsInternalDir);
            Files.createDirectories(jobsInternalTemplateDir);
            Files.createDirectories(jobsQuartzDir);
            Files.createDirectories(jobsGlobalDir);
            Files.createDirectories(notificationDir);
            Files.createDirectories(notificationDetailDir);
            Files.createDirectories(profilesDir);

            addJobFilesToZip(objectMapper, jobsFileDir, jobsInternalDir, jobsQuartzDir, jobsGlobalDir, jobsInternalTemplateDir
                , jobsInPlan, addReplacementTokens);

            // create the outputstream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            File fileToZip = new File(getWorkingDirectory(workingDirectory) + contextFileName);
            zipDirectory(fileToZip, fileToZip.getName(), zipOut);

            // close the streams
            zipOut.close();
            baos.close();

            // clean up everything in case there is anything there
            deleteWorkingDirectory(getWorkingDirectory(workingDirectory) + contextFileName);

            return baos;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn(String.format("Got exception creating zip file. Error [%s]", e.getMessage()));
            // clean up everything in case there is anything there
            try {
                deleteWorkingDirectory(getWorkingDirectory(workingDirectory) +
                    StringUtils.replaceEach(context.getName(), UNSAFE_FILENAME_CHAR, REPLACE_UNSAFE_FILENAME_CHAR));
            } catch (IOException ex) {}

            return null;
        }
    }

    private static List<SchedulerJob> getJobsInPlanWithIncludedGlobalEventJobs(ContextTemplate context, List<SchedulerJob> results) {
        List<String> jobPlanSchedulerJobIdentifiers = ContextHelper.getAllJobs(context).stream()
            .map(job -> job.getIdentifier())
            .collect(Collectors.toList());

        List<SchedulerJob> filteredScheduledJobs = results.stream()
            .filter(job -> {
                if(!job.getAgentName().equals(JobConstants.GLOBAL_EVENT))
                    return true;
                else
                    return jobPlanSchedulerJobIdentifiers.contains(job.getIdentifier());
            })
            .collect(Collectors.toList());

        return filteredScheduledJobs;
    }

    /**
     * Given the currentContext, write it out to currentContextDirectory/currentContext.getName()+".json"
     * For any subcontext that currentContext, recursively invoked this method.
     * The subcontext will be places in a subdirectory whose name is currentContext.getName()
     * @param objectMapper used to persist
     * @param currentContextDirectory that will hold this context
     * @param currentContext to be persisted
     * @throws IOException if we fail to create the directory
     */
    private static void writeContextRoots(ObjectMapper objectMapper, String currentContextDirectory, ContextTemplate currentContext) throws IOException {
        if (currentContext.getContexts() != null && !currentContext.getContexts().isEmpty()) {
            String newContextDirectory = createContextDirectory(currentContextDirectory, currentContext);
            for (ContextTemplate subContext : ContextTemplateUtils.setOrdinalsInContextTemplates(currentContext.getContexts())) {
                writeContextRoots(objectMapper, newContextDirectory, subContext);
            }
        }
        writeCurrentRoot(objectMapper, currentContextDirectory, currentContext);
    }

    /**
     * Creates a new context directory under the parent directory using the given ContextTemplate.
     *
     * @param parentDirectory The parent directory path where the new context directory will be created.
     * @param context The ContextTemplate containing information for the new directory.
     * @return The path of the newly created context directory.
     * @throws IOException If an I/O error occurs during directory creation.
     */
    private static String createContextDirectory(String parentDirectory, ContextTemplate context) throws IOException {
        String newDirectory = parentDirectory + File.separator + sanitiseForUseAsFilename(context.getName());
        mkdir(newDirectory);
        return newDirectory;
    }

    /**
     * Creates a new directory at the specified path if it doesn't already exist.
     *
     * @param newDirectory The path of the directory to be created.
     * @return The Path object representing the newly created directory.
     * @throws IOException If an I/O error occurs during directory creation.
     */
    private static Path mkdir(String newDirectory) throws IOException {
        Path contextDir = Paths.get(newDirectory);
        Files.createDirectories(contextDir);
        return contextDir;
    }

    /**
     * Persist the individual contextTemplate to CONTEXT-currentContext.getName().json
     * @param objectMapper used to convert the Java objects to json strings
     * @param containingDirectory for this json
     * @param context to be persisted
     * @throws IOException if persistence fails
     */
    protected static void writeCurrentRoot(ObjectMapper objectMapper, String containingDirectory, ContextTemplate context) throws IOException {
        // Leaves for this node already written, remove context to avoid serializing whole graph
        context.setContexts(Collections.EMPTY_LIST);
        String template = objectMapper.writeValueAsString(context);
        Path contextFilePath = Paths.get(containingDirectory + File.separator + sanitiseForUseAsFilename(context.getName()) + ".json");
        Files.write(contextFilePath, template.getBytes());
    }

    /**
     * Zips the contents of a directory into a ZipOutputStream.
     *
     * @param fileToZip the File representing the directory to be zipped
     * @param fileName the name of the entry in the zip file
     * @param zipOutputStream the ZipOutputStream to write the zipped data to
     * @throws IOException if an I/O error occurs during zipping
     */
    private static void zipDirectory(File fileToZip, String fileName, ZipOutputStream zipOutputStream) throws IOException {
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOutputStream.putNextEntry(new ZipEntry(fileName));
                zipOutputStream.closeEntry();
            } else {
                zipOutputStream.putNextEntry(new ZipEntry(fileName + "/"));
                zipOutputStream.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipDirectory(childFile, fileName + "/" + childFile.getName(), zipOutputStream);
                }
            }
            return;
        }

        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOutputStream.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOutputStream.write(bytes, 0, length);
        }

        fis.close();
    }

    /**
     * Adds job files to a zip archive.
     *
     * @param objectMapper the ObjectMapper to use for object serialization
     * @param filePath the directory path where the FileEventDrivenJob files will be stored in the zip archive
     * @param internalPath the directory path where the InternalEventDrivenJob files will be stored in the zip archive
     * @param quartzPath the directory path where the QuartzScheduleDrivenJob files will be stored in the zip archive
     * @param globalPath the directory path where the GlobalEventJob files will be stored in the zip archive
     * @param internalTemplatePath the directory path where the InternalTemplatePath files will be stored in the zip archive
     * @param results the list of SchedulerJob objects to add to the zip archive
     * @param addReplacementTokens a boolean indicating whether replacement tokens should be added to the job files
     * @throws IOException if an I/O error occurs during file creation or writing
     */
    private static void addJobFilesToZip(ObjectMapper objectMapper, Path filePath, Path internalPath, Path quartzPath, Path globalPath, Path internalTemplatePath,
                                         List<SchedulerJob> results, boolean addReplacementTokens) throws IOException {
        for (SchedulerJob schedulerJob : results) {

            if(addReplacementTokens) {
                ContextHelper.addSchedulerJobReplacementTokens(schedulerJob);
            }

            // We do not want to export the child context names, as these are automatically populated
            // when a job plan is imported.
            schedulerJob.setChildContextNames(null);

            String jobAsString = objectMapper.writeValueAsString(schedulerJob);
            Path jobPath = null;

            // sanitise the jobName as this will be used for the filename and that windows do not allow for certain characters
            String jobName = sanitiseForUseAsFilename(schedulerJob.getJobName());
            if (schedulerJob instanceof FileEventDrivenJob) {
                jobPath = Paths.get(filePath + File.separator + jobName + ".json");
            }
            else if (schedulerJob instanceof InternalEventDrivenJob) {
                if(schedulerJob.isTemplateJob() != null && schedulerJob.isTemplateJob()) {
                    jobPath = Paths.get(internalTemplatePath + File.separator + jobName + ".json");
                }
                else {
                    jobPath = Paths.get(internalPath + File.separator + jobName + ".json");
                }

            }
            else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                jobPath = Paths.get(quartzPath + File.separator + jobName + ".json");
            }
            else if (schedulerJob instanceof GlobalEventJob) {
                jobPath = Paths.get(globalPath + File.separator + jobName + ".json");
            }
            else {
                LOG.warn("Unknown job type: " + schedulerJob.getClass().getName());
            }

            if (jobPath != null) {
                Files.createFile(jobPath);
                Files.write(jobPath, jobAsString.getBytes());
            }
        }
    }

    /**
     * Adds notification files to a zip archive.
     *
     * @param objectMapper the ObjectMapper to use for object serialization
     * @param notificationPath the directory where notification files will be stored in the zip archive
     * @param results the search results containing EmailNotificationContextRecord objects to add to the zip archive
     * @throws IOException if an I/O error occurs during file creation or writing
     */
    private static void addNotificationFilesToZip(ObjectMapper objectMapper, Path notificationPath, SearchResults<EmailNotificationContextRecord> results) throws IOException {
        for (EmailNotificationContextRecord record : results.getResultList()) {
            String jsonString = objectMapper.writeValueAsString(record.getEmailNotificationContext());

            // Use the EmailNotificationContextRecord.id as the filename
            String fileName = sanitiseForUseAsFilename(record.getId());
            Path path = Paths.get(notificationPath + File.separator + fileName + "-notification.json");

            // Create the file
            Files.createFile(path);
            Files.write(path, jsonString.getBytes());
        }
    }

    /**
     * Adds notification detail files to a zip archive.
     *
     * @param objectMapper the ObjectMapper to use for object serialization
     * @param notificationDetailPath the directory where notification detail files will be stored in the zip archive
     * @param results the search results containing EmailNotificationDetailsRecord objects to add to the zip archive
     * @throws IOException if an I/O error occurs during file creation or writing
     */
    private static void addNotificationDetailFilesToZip(ObjectMapper objectMapper, Path notificationDetailPath, SearchResults<EmailNotificationDetailsRecord> results) throws IOException {
        for (EmailNotificationDetailsRecord record : results.getResultList()) {
            String jsonString = objectMapper.writeValueAsString(record.getEmailNotificationDetails());

            // Use the EmailNotificationDetailsRecord.id as the filename
            String fileName = sanitiseForUseAsFilename(record.getId());
            Path path = Paths.get(notificationDetailPath + File.separator + fileName + ".json");

            // Create the file
            Files.createFile(path);
            Files.write(path, jsonString.getBytes());
        }
    }

    /**
     * Adds profile files to a zip archive.
     *
     * @param objectMapper the ObjectMapper to use for object serialization
     * @param profilesDir the directory where profile files will be stored in the zip archive
     * @param results the search results containing ContextProfileRecord objects to add to the zip archive
     * @throws IOException if an I/O error occurs during file creation or writing
     */
    private static void addProfilesFilesToZip(ObjectMapper objectMapper, Path profilesDir, SearchResults<ContextProfileRecord> results) throws IOException {
        for (ContextProfileRecord record : results.getResultList()) {
            String jsonString = objectMapper.writeValueAsString(record);

            // Use the contextName and profileName as the filename
            String fileName = sanitiseForUseAsFilename(record.getContextName()+"-"+record.getProfileName());
            Path path = Paths.get(profilesDir + File.separator + fileName + ".json");

            // Create the file
            Files.createFile(path);
            Files.write(path, jsonString.getBytes());
        }
    }

    /**
     * Deletes the specified working directory if it exists.
     *
     * @param name the name of the directory to delete
     * @throws IOException if an I/O error occurs
     */
    private static void deleteWorkingDirectory(String name) throws IOException {
        if (Files.exists(Paths.get(name))) {
            FileUtils.deleteDirectory(new File(name));
        }
    }

    /**
     * Retrieves the working directory to be used for zipping.
     *
     * @param workingDirectory the directory path to check
     * @return an empty string if the working directory is ".", otherwise the provided working directory
     */
    private static String getWorkingDirectory(String workingDirectory) {
        if (workingDirectory.equals(".")) {
            return "";
        }
        return workingDirectory;
    }
}
