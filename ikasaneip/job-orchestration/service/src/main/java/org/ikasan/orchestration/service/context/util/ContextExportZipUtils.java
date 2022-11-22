package org.ikasan.orchestration.service.context.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ikasan.job.orchestration.model.profile.ContextProfileSearchFilterImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.ikasan.job.orchestration.util.ContextImportExportConstants.*;


public final class ContextExportZipUtils {

    public static final String[] UNSAFE_FILENAME_CHAR = new String[]{"\\", ":", "/", "*", "?", "\"", "<", ">", "|"};
    public static final String[] REPLACE_UNSAFE_FILENAME_CHAR = new String[]{"_",  "_" ,"_", "_", "_", "_",  "_", "_", "_"};

    private static final Logger LOG = LoggerFactory.getLogger(ContextExportZipUtils.class);

    public static String getExportZipFileName(String contextName) {
        return contextName + ".zip";
    }

    public static ByteArrayOutputStream createZipFile(ContextTemplate context,
                                                      String workingDirectory,
                                                      SchedulerJobService schedulerJobService,
                                                      EmailNotificationDetailsService emailNotificationDetailsService,
                                                      EmailNotificationContextService emailNotificationContextService,
                                                      ContextProfileService contextProfileService,
                                                      int searchLimit) {
        try {
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Export with pretty lines
            String template = objectMapper.writeValueAsString(context);

            String contextName = context.getName();

            // sanitise the contextName as this will be used for the filename and that windows do not allow for certain characters
            String contextFileName = StringUtils.replaceEach(contextName, UNSAFE_FILENAME_CHAR, REPLACE_UNSAFE_FILENAME_CHAR);

            // clean up the working directory if it exists
            deleteWorkingDirectory(getWorkingDirectory(workingDirectory) + contextFileName);

            // create the paths and directories on disk
            Path contextDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + CONTEXT_DIR);
            Path jobsDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + JOBS_DIR);
            Path jobsFileDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + JOBS_DIR + File.separator + FILE_DIR);
            Path jobsInternalDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + JOBS_DIR + File.separator + INTERNAL_DIR);
            Path jobsQuartzDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + JOBS_DIR + File.separator + QUARTZ_DIR);
            Path notificationDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + NOTIFICATION_DIR);
            Path notificationDetailDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + NOTIFICATION_DETAILS_DIR);
            Path profilesDir = Paths.get(getWorkingDirectory(workingDirectory) + contextFileName + File.separator + PROFILE_DIR);

            Files.createDirectories(contextDir);
            Files.createDirectories(jobsDir);
            Files.createDirectories(jobsFileDir);
            Files.createDirectories(jobsInternalDir);
            Files.createDirectories(jobsQuartzDir);
            Files.createDirectories(notificationDir);
            Files.createDirectories(notificationDetailDir);
            Files.createDirectories(profilesDir);

            // create the context template as json
            Path contextFilePath = Paths.get(contextDir + File.separator + contextFileName + ".json");
            Files.createFile(contextFilePath);
            Files.write(contextFilePath, template.getBytes());

            // get all the jobs
            int offset = 0;
            SearchResults<SchedulerJobRecord> results = schedulerJobService.findByContext(contextName, searchLimit, offset);
            addJobFilesToZip(objectMapper, jobsFileDir, jobsInternalDir, jobsQuartzDir, results);

            int retrievedNumber = results.getResultList().size();
            long totalNumberOfResults = results.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                results = schedulerJobService.findByContext(contextName, searchLimit, offset);
                addJobFilesToZip(objectMapper, jobsFileDir, jobsInternalDir, jobsQuartzDir, results);
            }

            // get the overall notifications settings for the context
            offset = 0;
            SearchResults<EmailNotificationContextRecord> notificationResults = emailNotificationContextService.findByContextName(contextName, searchLimit, offset);
            addNotificationFilesToZip(objectMapper, notificationDir, notificationResults);

            retrievedNumber = notificationResults.getResultList().size();
            totalNumberOfResults = notificationResults.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                notificationResults = emailNotificationContextService.findByContextName(contextName, searchLimit, offset);
                addNotificationFilesToZip(objectMapper, notificationDir, notificationResults);
            }

            // get the notification details for the context - these are the individual notification defined per context, child context and job.
            offset = 0;
            SearchResults<EmailNotificationDetailsRecord> notificationDetailResults = emailNotificationDetailsService.findByContextName(contextName, searchLimit, offset);
            addNotificationDetailFilesToZip(objectMapper, notificationDetailDir, notificationDetailResults);

            retrievedNumber = notificationDetailResults.getResultList().size();
            totalNumberOfResults = notificationDetailResults.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                notificationDetailResults = emailNotificationDetailsService.findByContextName(contextName, searchLimit, offset);
                addNotificationDetailFilesToZip(objectMapper, notificationDetailDir, notificationDetailResults);
            }

            // get the profiles for the context
            offset = 0;
            ContextProfileSearchFilter profileFilter = new ContextProfileSearchFilterImpl();
            profileFilter.setContextName(contextName); //search by contextName

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

    private static void addJobFilesToZip(ObjectMapper objectMapper, Path p3, Path p4, Path p5, SearchResults<SchedulerJobRecord> results) throws IOException {
        for (SchedulerJobRecord schedulerJobRecord : results.getResultList()) {
            String jobAsString = objectMapper.writeValueAsString(schedulerJobRecord.getJob());
            Path jobPath = null;

            // sanitise the jobName as this will be used for the filename and that windows do not allow for certain characters
            String jobName = StringUtils.replaceEach(schedulerJobRecord.getJob().getJobName(),
                UNSAFE_FILENAME_CHAR, REPLACE_UNSAFE_FILENAME_CHAR);
            switch (schedulerJobRecord.getType()) {
                case JobConstants.FILE_EVENT_DRIVEN_JOB:
                    jobPath = Paths.get(p3 + File.separator + jobName + ".json");
                    break;
                case JobConstants.INTERNAL_EVENT_DRIVEN_JOB:
                    jobPath = Paths.get(p4 + File.separator + jobName + ".json");
                    break;
                case JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB:
                    jobPath = Paths.get(p5 + File.separator + jobName + ".json");
                    break;
                default:
                    LOG.warn("Unknown job type: " + schedulerJobRecord.getType());
                    break;
            }
            if (jobPath != null) {
                Files.createFile(jobPath);
                Files.write(jobPath, jobAsString.getBytes());
            }
        }
    }

    private static void addNotificationFilesToZip(ObjectMapper objectMapper, Path notificationPath, SearchResults<EmailNotificationContextRecord> results) throws IOException {
        for (EmailNotificationContextRecord record : results.getResultList()) {
            String jsonString = objectMapper.writeValueAsString(record.getEmailNotificationContext());

            // Use the EmailNotificationContextRecord.id as the filename
            String fileName = StringUtils.replaceEach(record.getId(),
                UNSAFE_FILENAME_CHAR, REPLACE_UNSAFE_FILENAME_CHAR);
            Path path = Paths.get(notificationPath + File.separator + fileName + "-notification.json");

            // Create the file
            Files.createFile(path);
            Files.write(path, jsonString.getBytes());
        }
    }

    private static void addNotificationDetailFilesToZip(ObjectMapper objectMapper, Path notificationDetailPath, SearchResults<EmailNotificationDetailsRecord> results) throws IOException {
        for (EmailNotificationDetailsRecord record : results.getResultList()) {
            String jsonString = objectMapper.writeValueAsString(record.getEmailNotificationDetails());

            // Use the EmailNotificationDetailsRecord.id as the filename
            String fileName = StringUtils.replaceEach(record.getId(),
                UNSAFE_FILENAME_CHAR, REPLACE_UNSAFE_FILENAME_CHAR);
            Path path = Paths.get(notificationDetailPath + File.separator + fileName + ".json");

            // Create the file
            Files.createFile(path);
            Files.write(path, jsonString.getBytes());
        }
    }

    private static void addProfilesFilesToZip(ObjectMapper objectMapper, Path profilesDir, SearchResults<ContextProfileRecord> results) throws IOException {
        for (ContextProfileRecord record : results.getResultList()) {
            String jsonString = objectMapper.writeValueAsString(record);

            // Use the contextName and profileName as the filename
            String fileName = StringUtils.replaceEach(record.getContextName()+"-"+record.getProfileName(),
                UNSAFE_FILENAME_CHAR, REPLACE_UNSAFE_FILENAME_CHAR);
            Path path = Paths.get(profilesDir + File.separator + fileName + ".json");

            // Create the file
            Files.createFile(path);
            Files.write(path, jsonString.getBytes());
        }
    }

    private static void deleteWorkingDirectory(String name) throws IOException {
        if (Files.exists(Paths.get(name))) {
            FileUtils.deleteDirectory(new File(name));
        }
    }

    private static String getWorkingDirectory(String workingDirectory) {
        if (workingDirectory.equals(".")) {
            return "";
        }
        return workingDirectory;
    }
}
