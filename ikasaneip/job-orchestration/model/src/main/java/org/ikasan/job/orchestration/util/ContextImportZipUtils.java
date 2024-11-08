package org.ikasan.job.orchestration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.ikasan.job.orchestration.util.ContextHelper.ENV_NAME_REPLACEMENT;
import static org.ikasan.job.orchestration.util.ContextImportExportConstants.*;


public final class ContextImportZipUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ContextImportZipUtils.class);

    /**
     * Import a zip containing 1 or more JobPlans that have explicit environment names (not tokens). Note
     * that token replacement is done by the CI at deployment time, the CI uses the scheduler agent provision utilities,
     * to import, not this method. The method will produce the same results whether the subcontexts are split or not.
     * @param inputStream containing the zip
     * @return a populated context bundle extracted from the zip
     */
    public static ContextBundle extractZipFile(InputStream inputStream) {
        List<SchedulerJob> contextJobs = new ArrayList<>();
        List<ContextProfileRecord> contextProfileRecords = new ArrayList<>();
        List<EmailNotificationDetails> emailNotificationDetails = new ArrayList<>();
        AtomicReference<EmailNotificationContext> emailNotificationContexts = new AtomicReference<>();
        AtomicReference<ContextTemplate> atomicContextTemplate = new AtomicReference<>();
        Map<String, List<ContextTemplate>> dirToTemplates = new ConcurrentHashMap<>();
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
                    String internalTemplateJobsDirectory = parentDirectory + JOBS_DIR + "/" + INTERNAL_TEMPLATES_DIR + "/";
                    String quartzJobsDirectory = parentDirectory + JOBS_DIR + "/" + QUARTZ_DIR + "/";
                    String globalJobsDirectory = parentDirectory + JOBS_DIR + "/" + GLOBAL_JOB_DIR + "/"; //TODO WE have to build a test once we know what the Global Job looks like
                    String contextProfileDirectory = parentDirectory + PROFILE_DIR + "/";
                    String emailNotificationDirectory = parentDirectory + NOTIFICATION_DIR + "/";
                    String emailNotificationDetailDirectory = parentDirectory + NOTIFICATION_DETAILS_DIR + "/";

                    if (!entry.isDirectory() && entry.getName().startsWith(contextDirectory)
                        && entry.getName().endsWith(".json")) {
                        String directory = getLeafDirectoryFromPath(entry.getName());
                        atomicContextTemplate.set((ContextTemplate) getContextArtifact(CONTEXT_TEMPLATE, outputStream.toString(), contextService));
                        // Thread safe way to add amp entry for String -> List<String>
                        dirToTemplates.computeIfAbsent(
                                directory ,
                                ( x -> new CopyOnWriteArrayList<>() )
                            )
                            .add( (ContextTemplate) getContextArtifact(CONTEXT_TEMPLATE, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(fileJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(FILE_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(internalJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(INTERNAL_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(internalTemplateJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(INTERNAL_TEMPLATES_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(quartzJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(QUARTZ_DIR, outputStream.toString(), contextService));
                    } else if (!entry.isDirectory() && entry.getName().startsWith(globalJobsDirectory)
                        && entry.getName().endsWith(".json")) {
                        contextJobs.add((SchedulerJob) getContextArtifact(GLOBAL_JOB_DIR, outputStream.toString(), contextService));
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
            e.printStackTrace();
            LOG.warn("Could not read zip file, Error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // The CI can support multiple level 1 contexts, the dashboard import supports only 1 plan (without tokens)
        ContextTemplate contextTemplate = dirToTemplates.get("context").get(0);
        rebuildSubContexts(contextTemplate, dirToTemplates);

        return new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, emailNotificationDetails, emailNotificationContexts.get(), new ArrayList<>());
    }

    /**
     * Given a path of format dir1/../dirn/xx.file return dirn i.e. the final directory
     * @param path to interrogate
     * @return the final directory of the path
     */
    protected static String getLeafDirectoryFromPath(String path) {
        String[] splitPath = path.split("/");
        return splitPath[splitPath.length - 2];
    }

    /**
     * Recursively rebuild the Context Template Graph into the single root
     * @param currentRoot to service
     * @param dirToTemplates directory -> List<ContextTemplates> where directory == name of the context that owns the subcontexts
     */
    protected static void rebuildSubContexts(ContextTemplate currentRoot, Map<String, List<ContextTemplate>> dirToTemplates) {
        List<ContextTemplate> dirContexts = dirToTemplates.get(sanitiseForUseAsFilename(currentRoot.getName()));
        if (dirContexts != null) {
            List<ContextTemplate> sortedDirContexts = dirContexts.stream()
                .sorted(Comparator.comparingInt(ContextTemplate::getOrdinal))
                .collect(Collectors.toList());
            currentRoot.setContexts(sortedDirContexts);
            for (ContextTemplate subContext : dirContexts) {
                rebuildSubContexts(subContext, dirToTemplates);
            }
        }
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
                case INTERNAL_TEMPLATES_DIR:
                    return service.getInternalEventDrivenJob(json);
                case QUARTZ_DIR:
                    return service.getQuartzScheduleDrivenJob(json);
                case GLOBAL_JOB_DIR:
                    return service.getGlobalEventJob(json);
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

    public static final String[] UNSAFE_FILENAME_CHAR = new String[]{"\\", ":", "/", "*", "?", "\"", "<", ">", "|"};
    public static final String[] REPLACE_UNSAFE_FILENAME_CHAR = new String[]{"_", "_" ,"_", "_", "_", "_", "_", "_", "_"};

    /**
     * As below
     * @param toBeSanitised which is typically some form of the context name
     * @return a sanitized version that can be used as a filename
     */
    public static String sanitiseForUseAsFilename(String toBeSanitised) {
        return StringUtils.replaceEach(toBeSanitised, UNSAFE_FILENAME_CHAR, REPLACE_UNSAFE_FILENAME_CHAR).replace("_"+ ENV_NAME_REPLACEMENT, "");
    }
}
