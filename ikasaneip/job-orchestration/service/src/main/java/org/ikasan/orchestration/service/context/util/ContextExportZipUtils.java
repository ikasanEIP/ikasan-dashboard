package org.ikasan.orchestration.service.context.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
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
    private static final Logger LOG = LoggerFactory.getLogger(ContextExportZipUtils.class);

    public static String getExportZipFileName(String contextName) {
        return contextName + ".zip";
    }

    public static ByteArrayOutputStream createZipFile(ContextTemplate context,
                                                      String workingDirectory,
                                                      SchedulerJobService schedulerJobService,
                                                      int searchLimit) {
        try {
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            String template = objectMapper.writeValueAsString(context);

            String contextName = context.getName();

            // clean up the working directory if it exists
            deleteWorkingDirectory(getWorkingDirectory(workingDirectory) + contextName);

            // create the paths and directories on disk
            Path contextDir = Paths.get(getWorkingDirectory(workingDirectory) + contextName + File.separator + CONTEXT_DIR);
            Path jobsDir = Paths.get(getWorkingDirectory(workingDirectory) + contextName + File.separator + JOBS_DIR);
            Path jobsFileDir = Paths.get(getWorkingDirectory(workingDirectory) + contextName + File.separator + JOBS_DIR + File.separator + FILE_DIR);
            Path jobsInternalDir = Paths.get(getWorkingDirectory(workingDirectory) + contextName + File.separator + JOBS_DIR + File.separator + INTERNAL_DIR);
            Path jobsQuartzDir = Paths.get(getWorkingDirectory(workingDirectory) + contextName + File.separator + JOBS_DIR + File.separator + QUARTZ_DIR);

            Files.createDirectories(contextDir);
            Files.createDirectories(jobsDir);
            Files.createDirectories(jobsFileDir);
            Files.createDirectories(jobsInternalDir);
            Files.createDirectories(jobsQuartzDir);

            // create the context template as json
            Path contextFilePath = Paths.get(contextDir + File.separator + contextName + ".json");
            Files.createFile(contextFilePath);
            Files.write(contextFilePath, template.getBytes());

            // get all the jobs
            int offset = 0;
            SearchResults<SchedulerJobRecord> results = schedulerJobService.findByContext(contextName, searchLimit, offset);
            addFilesToZip(objectMapper, jobsFileDir, jobsInternalDir, jobsQuartzDir, results);

            int retrievedNumber = results.getResultList().size();
            long totalNumberOfResults = results.getTotalNumberOfResults();
            while (offset < totalNumberOfResults) {
                offset += retrievedNumber;
                results = schedulerJobService.findByContext(contextName, searchLimit, offset);
                addFilesToZip(objectMapper, jobsFileDir, jobsInternalDir, jobsQuartzDir, results);
            }

            // create the outputstream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            File fileToZip = new File(getWorkingDirectory(workingDirectory) + contextName);
            zipDirectory(fileToZip, fileToZip.getName(), zipOut);

            // close the streams
            zipOut.close();
            baos.close();

            // clean up everything in case there is anything there
            deleteWorkingDirectory(getWorkingDirectory(workingDirectory) + contextName);

            return baos;
        } catch (Exception e) {
            LOG.warn(String.format("Got exception creating zip file. Error [%s]", e.getMessage()));
            // clean up everything in case there is anything there
            try {
                deleteWorkingDirectory(getWorkingDirectory(workingDirectory) + context.getName());
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

    private static void addFilesToZip(ObjectMapper objectMapper, Path p3, Path p4, Path p5, SearchResults<SchedulerJobRecord> results) throws IOException {
        for (SchedulerJobRecord schedulerJobRecord : results.getResultList()) {
            String jobAsString = objectMapper.writeValueAsString(schedulerJobRecord.getJob());
            Path jobPath = null;
            switch (schedulerJobRecord.getType()) {
                case JobConstants.FILE_EVENT_DRIVEN_JOB:
                    jobPath = Paths.get(p3 + File.separator + schedulerJobRecord.getJob().getJobName() + ".json");
                    break;
                case JobConstants.INTERNAL_EVENT_DRIVEN_JOB:
                    jobPath = Paths.get(p4 + File.separator + schedulerJobRecord.getJob().getJobName() + ".json");
                    break;
                case JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB:
                    jobPath = Paths.get(p5 + File.separator + schedulerJobRecord.getJob().getJobName() + ".json");
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
