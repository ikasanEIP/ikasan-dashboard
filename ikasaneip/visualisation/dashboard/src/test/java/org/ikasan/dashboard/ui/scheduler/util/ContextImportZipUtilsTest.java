package org.ikasan.dashboard.ui.scheduler.util;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class ContextImportZipUtilsTest {

    @Test
    public void should_unzip_file_context_and_a_truck_load_of_jobs() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-1436221681.zip").getInputStream();
            ImmutablePair<ContextTemplate, List<SchedulerJob>> pair = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = pair.getLeft();
            List<SchedulerJob> jobs = pair.getRight();

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
    public void should_unzip_file_context_and_jobs() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX.zip").getInputStream();
            ImmutablePair<ContextTemplate, List<SchedulerJob>> pair = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = pair.getLeft();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX", contextTemplate.getName());

            List<SchedulerJob> jobs = pair.getRight();
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
            ImmutablePair<ContextTemplate, List<SchedulerJob>> pair = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = pair.getLeft();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-EMPTY-JOBS", contextTemplate.getName());

            List<SchedulerJob> jobs = pair.getRight();
            assertEquals(0, jobs.size());
        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

    @Test
    public void should_unzip_file_context_and_missing_jobs() {
        try {
            InputStream inputStream = new ClassPathResource("data/zip/CONTEXT-NOT-SO-COMPLEX-MISSING-JOBS.zip").getInputStream();
            ImmutablePair<ContextTemplate, List<SchedulerJob>> pair = ContextImportZipUtils.extractZipFile(inputStream);

            ContextTemplate contextTemplate = pair.getLeft();
            assertNotNull(contextTemplate);
            assertEquals("CONTEXT-NOT-SO-COMPLEX-MISSING-JOBS", contextTemplate.getName());

            List<SchedulerJob> jobs = pair.getRight();
            assertEquals(0, jobs.size());
        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
        }
    }

}