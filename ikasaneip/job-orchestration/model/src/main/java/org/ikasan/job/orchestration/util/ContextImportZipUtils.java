package org.ikasan.job.orchestration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.job.orchestration.model.context.ContextBundleImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.ikasan.job.orchestration.util.ContextImportExportConstants.*;


public final class ContextImportZipUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ContextImportZipUtils.class);

    public static ContextBundle extractZipFile(InputStream inputStream) {
        List<SchedulerJob> contextJobs = new ArrayList<>();
        List<ContextProfileRecord> contextProfileRecords = new ArrayList<>();
        List<EmailNotificationDetails> emailNotificationDetails = new ArrayList<>();
        AtomicReference<EmailNotificationContext> emailNotificationContexts = new AtomicReference<>();
        AtomicReference<ContextTemplate> contextTemplate = new AtomicReference<>();
        try {
            ContextService contextService = new ContextService();
            AtomicReference<String> parentDirectory = new AtomicReference<>();
            readZipInputStream(inputStream, (entry, outputStream) -> {
                    // 1st entry is the parent Directory
                    if (parentDirectory.get() == null) {
                        parentDirectory.set(entry.getName());
                    }
                    String contextDirectory = parentDirectory + CONTEXT_DIR + "/";
                    String fileJobsDirectory = parentDirectory + JOBS_DIR + "/" + FILE_DIR + "/";
                    String internalJobsDirectory = parentDirectory + JOBS_DIR + "/" + INTERNAL_DIR + "/";
                    String quartzJobsDirectory = parentDirectory + JOBS_DIR + "/" + QUARTZ_DIR + "/";
                    String contextProfileDirectory = parentDirectory + PROFILE_DIR + "/";
                    String emailNotificationDirectory = parentDirectory + NOTIFICATION_DIR + "/";
                    String emailNotificationDetailDirectory = parentDirectory + NOTIFICATION_DETAILS_DIR + "/";

                    if (!entry.isDirectory() && entry.getName().startsWith(contextDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextTemplate.set((ContextTemplate) getContextArtifact(CONTEXT_TEMPLATE, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(fileJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(FILE_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(internalJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(INTERNAL_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(quartzJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(QUARTZ_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(contextProfileDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextProfileRecords.add((ContextProfileRecord) getContextArtifact(PROFILE_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(emailNotificationDirectory)
                        && entry.getName().endsWith(".json")) {
                        emailNotificationContexts.set((EmailNotificationContext) getContextArtifact(NOTIFICATION_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(emailNotificationDetailDirectory)
                        && entry.getName().endsWith(".json")) {
                        emailNotificationDetails.add((EmailNotificationDetails) getContextArtifact(NOTIFICATION_DETAILS_DIR, outputStream.toString(), contextService));
                    }
                }
            );
        } catch (Exception e) {
            LOG.warn("Could not read zip file, Error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return new ContextBundleImpl(contextTemplate.get(), contextJobs, contextProfileRecords, emailNotificationDetails, emailNotificationContexts.get());
    }

    private static void readZipInputStream(InputStream inputStream, BiConsumer<ZipEntry, ByteArrayOutputStream> biConsumer) {
        try {
            ZipInputStream zipInput = new ZipInputStream(inputStream);
            for (ZipEntry entry; (entry = zipInput.getNextEntry()) != null; ) {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zipInput.read(buffer)) != -1) {
                    outStream.write(buffer, 0, length);
                }
                biConsumer.accept(entry, outStream);
            }
        } catch (Exception e) {
            LOG.warn("Could not read zip input stream, Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static Object getContextArtifact(String type, String json, ContextService service) {
        try {
            switch (type) {
                case CONTEXT_TEMPLATE:
                    return service.getContextTemplate(json);
                case FILE_DIR:
                    return service.getFileEventDrivenJob(json);
                case INTERNAL_DIR:
                    return service.getInternalEventDrivenJob(json);
                case QUARTZ_DIR:
                    return service.getQuartzScheduleDrivenJob(json);
                case PROFILE_DIR:
                    return service.getContextProfileRecord(json);
                case NOTIFICATION_DIR:
                    return service.getEmailNotificationContext(json);
                case NOTIFICATION_DETAILS_DIR:
                    return service.getEmailNotificationDetails(json);
                default:
                    throw new RuntimeException("Unknown job type: " + type);
            }
        } catch (JsonProcessingException e) {
            LOG.warn(String.format("Could not read json for job type %s, Error: %s", type, e.getMessage()));
            throw new RuntimeException(e);
        }
    }

}
