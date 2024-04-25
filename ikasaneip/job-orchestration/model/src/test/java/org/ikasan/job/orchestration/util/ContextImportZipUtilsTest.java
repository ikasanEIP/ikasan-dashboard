package org.ikasan.job.orchestration.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ContextImportZipUtilsTest {
    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    @Test
    public void should_unzip_file_context_with_subcontext_in_job_import_mode() throws IOException {
        String contextName = "HelloContext_[[env.name]]";   // Doesn't really matter if this is a token or not, it will be generated verbatim.
        String jsonContext = IOUtils.toString(getClass().getResourceAsStream("/data/contextWithNestedSubcontexts.json"), StandardCharsets.UTF_8);
        ContextTemplateImpl expectedContextTemplte = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        expectedContextTemplte.setName(contextName);

        try {
            InputStream inputStream = new ClassPathResource("data/zip/downloadName.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate actualContextTemplate = contextBundle.getContextTemplate();
            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();

            assertNotNull(actualContextTemplate);
            assertEquals(contextName, actualContextTemplate.getName());
            assertEquals(12, jobs.size());

            int fileJobCount = 0;
            int quartzJobCount = 0;
            int internalJobCount = 0;
            int globalJobCount = 0;
            for (SchedulerJob schedulerJob : jobs) {
                if (schedulerJob instanceof FileEventDrivenJob) {
                    fileJobCount++;
                } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                    quartzJobCount++;
                } else if (schedulerJob instanceof InternalEventDrivenJob) {
                    internalJobCount++;
                } else if (schedulerJob instanceof GlobalEventJobImpl) {
                    globalJobCount++;
                }
            }

            assertEquals(3, internalJobCount);
            assertEquals(3, fileJobCount);
            assertEquals(3, quartzJobCount);
            assertEquals(3, globalJobCount);
            assertEquals(expectedContextTemplte.getContexts().size(), actualContextTemplate.getContexts().size());
            Set<String> keyDifferences = expectedContextTemplte.getContextsMap().keySet();
            keyDifferences.removeAll(actualContextTemplate.getContextsMap().keySet());
            assertEquals(0, keyDifferences.size());


        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }


    @Test
    public void should_unzip_file_context_and_a_truck_load_of_jobs() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-1436221681.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();

            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-1436221681", contextTemplate.getName());
            assertEquals(1334, jobs.size());

            int fileJobCount = 0;
            int quartzJobCount = 0;
            int internalJobCount = 0;
            for (SchedulerJob schedulerJob : jobs) {
                if (schedulerJob instanceof FileEventDrivenJob) {
                    fileJobCount++;
                } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                    quartzJobCount++;
                } else if (schedulerJob instanceof InternalEventDrivenJob) {
                    internalJobCount++;
                }
            }

            assertEquals(200, internalJobCount);
            assertEquals(500, fileJobCount);
            assertEquals(634, quartzJobCount);

            assertEquals(1334, fileJobCount + quartzJobCount + internalJobCount);
        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_a_truck_load_of_jobs_and_profile() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-1793100514_WITH-PROFILES.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();

            assertNotNull(contextTemplate);
            assertEquals("-1793100514", contextTemplate.getName());
            assertEquals(967, jobs.size());

            int fileJobCount = 0;
            int quartzJobCount = 0;
            int internalJobCount = 0;
            for (SchedulerJob schedulerJob : jobs) {
                if (schedulerJob instanceof FileEventDrivenJob) {
                    fileJobCount++;
                } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                    quartzJobCount++;
                } else if (schedulerJob instanceof InternalEventDrivenJob) {
                    internalJobCount++;
                }
            }

            assertEquals(239, internalJobCount);
            assertEquals(486, fileJobCount);
            assertEquals(242, quartzJobCount);

            assertEquals(967, fileJobCount + quartzJobCount + internalJobCount);

            assertEquals(1, contextBundle.getContextProfiles().size());
            assertEquals("-1793100514", contextBundle.getContextProfiles().get(0).getContextName());
            assertEquals(6, contextBundle.getContextProfiles().get(0).getContextProfile().getSubContexts().size());

        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_jobs() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX", contextTemplate.getName());

            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();
            assertEquals(3, jobs.size());

            int count = 0;
            for (SchedulerJob schedulerJob : jobs) {
                if (schedulerJob instanceof FileEventDrivenJob) {
                    FileEventDrivenJob job = (FileEventDrivenJob) schedulerJob;
                    assertEquals("jobName2", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                    QuartzScheduleDrivenJob job = (QuartzScheduleDrivenJob) schedulerJob;
                    assertEquals("jobName3", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof InternalEventDrivenJob) {
                    InternalEventDrivenJob job = (InternalEventDrivenJob) schedulerJob;
                    assertEquals("jobName1", job.getJobName());
                    count++;
                }
            }

            assertEquals(3, count);

        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_no_jobs() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX-EMPTY-JOBS.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-EMPTY-JOBS", contextTemplate.getName());

            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();
            assertEquals(0, jobs.size());
        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_missing_jobs() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX-MISSING-JOBS.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-MISSING-JOBS", contextTemplate.getName());

            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();
            assertEquals(0, jobs.size());
        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_no_jobs_no_notification() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX-EMPTY-NOTIFICATION.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-EMPTY-NOTIFICATION", contextTemplate.getName());

            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();
            assertEquals(0, jobs.size());

            List<EmailNotificationDetails> notification = contextBundle.getEmailNotificationDetails();
            assertEquals(0, notification.size());
        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_jobs_and_notification() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", contextTemplate.getName());

            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();
            assertEquals(3, jobs.size());

            int count = 0;
            for (SchedulerJob schedulerJob : jobs) {
                if (schedulerJob instanceof FileEventDrivenJob) {
                    FileEventDrivenJob job = (FileEventDrivenJob) schedulerJob;
                    assertEquals("jobName2", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                    QuartzScheduleDrivenJob job = (QuartzScheduleDrivenJob) schedulerJob;
                    assertEquals("jobName3", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof InternalEventDrivenJob) {
                    InternalEventDrivenJob job = (InternalEventDrivenJob) schedulerJob;
                    assertEquals("jobName1", job.getJobName());
                    count++;
                }
            }

            assertEquals(3, count);

            List<EmailNotificationDetails> notification = contextBundle.getEmailNotificationDetails();
            assertEquals(2, notification.size());

            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(0).getContextName());
            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(0).getChildContextName());
            if (!(notification.get(0).getJobName().equals("jobName1") || notification.get(0).getJobName().equals("jobName3"))) {
                fail(notification.get(0).getJobName() + " is not equal to jobName1 or jobName3");
            }

            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(1).getContextName());
            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(1).getChildContextName());
            if (!(notification.get(1).getJobName().equals("jobName1") || notification.get(1).getJobName().equals("jobName3"))) {
                fail(notification.get(1).getJobName() + " is not equal to jobName1 or jobName3");
            }

            EmailNotificationContext notificationContext = contextBundle.getEmailNotificationContext();
            assertNull(notificationContext);

        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_jobs_and_notification_plus_notifContext() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS-2.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", contextTemplate.getName());

            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();
            assertEquals(3, jobs.size());

            int count = 0;
            for (SchedulerJob schedulerJob : jobs) {
                if (schedulerJob instanceof FileEventDrivenJob) {
                    FileEventDrivenJob job = (FileEventDrivenJob) schedulerJob;
                    assertEquals("jobName2", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                    QuartzScheduleDrivenJob job = (QuartzScheduleDrivenJob) schedulerJob;
                    assertEquals("jobName3", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof InternalEventDrivenJob) {
                    InternalEventDrivenJob job = (InternalEventDrivenJob) schedulerJob;
                    assertEquals("jobName1", job.getJobName());
                    count++;
                }
            }

            assertEquals(3, count);

            List<EmailNotificationDetails> notification = contextBundle.getEmailNotificationDetails();
            assertEquals(2, notification.size());

            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(0).getContextName());
            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(0).getChildContextName());
            if (!(notification.get(0).getJobName().equals("jobName1") || notification.get(0).getJobName().equals("jobName3"))) {
                fail(notification.get(0).getJobName() + " is not equal to jobName1 or jobName3");
            }

            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(1).getContextName());
            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notification.get(1).getChildContextName());
            if (!(notification.get(1).getJobName().equals("jobName1") || notification.get(1).getJobName().equals("jobName3"))) {
                fail(notification.get(1).getJobName() + " is not equal to jobName1 or jobName3");
            }

            EmailNotificationContext notificationContext = contextBundle.getEmailNotificationContext();
            assertNotNull(notificationContext);

            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notificationContext.getContextName());
            assertEquals(5, notificationContext.getMonitorTypes().size());
            assertEquals(5, notificationContext.getEmailBodyNotificationTemplate().size());
            assertEquals(5, notificationContext.getEmailSubjectNotificationTemplate().size());
            assertEquals("some@email.com", notificationContext.getEmailSendTo().get(0));
            assertEquals("some@email.com", notificationContext.getEmailSendToByMonitorType().get("OVERDUE").get(0));
            assertEquals("some@email.com", notificationContext.getEmailSendCcByMonitorType().get("COMPLETE").get(0));

        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_jobs_and_notification_context() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX-WITH-CONTEXT-NOTIFICATIONS-ONLY.zip").getInputStream();
            ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = contextBundle.getContextTemplate();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", contextTemplate.getName());

            List<SchedulerJob> jobs = contextBundle.getSchedulerJobs();
            assertEquals(3, jobs.size());

            int count = 0;
            for (SchedulerJob schedulerJob : jobs) {
                if (schedulerJob instanceof FileEventDrivenJob) {
                    FileEventDrivenJob job = (FileEventDrivenJob) schedulerJob;
                    assertEquals("jobName2", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof QuartzScheduleDrivenJob) {
                    QuartzScheduleDrivenJob job = (QuartzScheduleDrivenJob) schedulerJob;
                    assertEquals("jobName3", job.getJobName());
                    count++;
                } else if (schedulerJob instanceof InternalEventDrivenJob) {
                    InternalEventDrivenJob job = (InternalEventDrivenJob) schedulerJob;
                    assertEquals("jobName1", job.getJobName());
                    count++;
                }
            }

            assertEquals(3, count);

            List<EmailNotificationDetails> notification = contextBundle.getEmailNotificationDetails();
            assertEquals(0, notification.size());

            EmailNotificationContext notificationContext = contextBundle.getEmailNotificationContext();
            assertNotNull(notificationContext);

            assertEquals("CONTEXT-NOT-SO-COMPLEX-WITH-NOTIFICATIONS", notificationContext.getContextName());
            assertEquals(5, notificationContext.getMonitorTypes().size());
            assertEquals(5, notificationContext.getEmailBodyNotificationTemplate().size());
            assertEquals(5, notificationContext.getEmailSubjectNotificationTemplate().size());
            assertEquals("some@email.com", notificationContext.getEmailSendTo().get(0));
            assertEquals("some@email.com", notificationContext.getEmailSendToByMonitorType().get("OVERDUE").get(0));
            assertEquals("some@email.com", notificationContext.getEmailSendCcByMonitorType().get("COMPLETE").get(0));

        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }


}