package org.ikasan.dashboard.ui.scheduler.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.job.model.SolrSchedulerJobRecordImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.ikasan.scheduled.job.model.JobConstants.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextExportZipUtilsTest {

    public static final String TEMP_ZIP = "temp.zip";
    @Mock
    private SchedulerJobService schedulerJobService;

    @Test
    public void should_zip_context_and_jobs() {
        try {
            ContextService service = new ContextService();
            String jsonContext = new String(new ClassPathResource("data/contexts/context-simple.json").getInputStream().readAllBytes());

            ContextTemplate contextTemplate = service.getContextTemplate(jsonContext);

            String exportZipFileName = ContextExportZipUtils.getExportZipFileName(contextTemplate.getName());
            assertEquals("CONTEXT-NOT-SO-COMPLEX.zip", exportZipFileName);

            Mockito.when(schedulerJobService.findByContext("CONTEXT-NOT-SO-COMPLEX", 1, 0))
                .thenReturn(new SchedulerJobServiceTestSearchResults(1));
            Mockito.when(schedulerJobService.findByContext("CONTEXT-NOT-SO-COMPLEX", 1, 1))
                .thenReturn(new SchedulerJobServiceTestSearchResults(2));
            Mockito.when(schedulerJobService.findByContext("CONTEXT-NOT-SO-COMPLEX", 1, 2))
                .thenReturn(new SchedulerJobServiceTestSearchResults(3));
            Mockito.when(schedulerJobService.findByContext("CONTEXT-NOT-SO-COMPLEX", 1, 3))
                .thenReturn(new SchedulerJobServiceTestSearchResults(0));

            ByteArrayOutputStream zipFile
                = ContextExportZipUtils.createZipFile(contextTemplate, ".", schedulerJobService, 1);

            assertNotNull(zipFile);
            validateZipFile(zipFile);

        } catch (Exception e) {
            fail("Got exception " + e.getMessage());
            if (Files.exists(Paths.get(TEMP_ZIP))) {
                try {
                    FileUtils.forceDelete(new File(TEMP_ZIP));
                } catch (IOException ex) {
                }
            }
        }
    }

    private void validateZipFile(ByteArrayOutputStream zipFile) throws IOException {
        ContextTemplate contextTemplate;
        FileUtils.writeByteArrayToFile(new File(TEMP_ZIP), zipFile.toByteArray());
        byte[] bytes = FileUtils.readFileToByteArray(new File(TEMP_ZIP));

        ImmutablePair<ContextTemplate, List<SchedulerJob>> pair = ContextImportZipUtils.extractZipFile(new ByteArrayInputStream(bytes));

        contextTemplate = pair.getLeft();
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

        zipFile.close();
        FileUtils.forceDelete(new File(TEMP_ZIP));
    }

    private class SchedulerJobServiceTestSearchResults implements SearchResults<SchedulerJobRecord> {

        private final int number;

        public SchedulerJobServiceTestSearchResults(int number) {
            this.number = number;
        }

        @Override
        public List<SchedulerJobRecord> getResultList() {
            ArrayList<SchedulerJobRecord> schedulerJobRecords = new ArrayList<>();
            switch (number) {
                case 1: {
                    schedulerJobRecords.add(createFileJobRecord());
                    break;
                }
                case 2: {
                    schedulerJobRecords.add(createQuartzJobRecord());
                    break;
                }
                case 3: {
                    schedulerJobRecords.add(createInternalJobRecord());
                    break;
                }
                default:
                    break;
            }

            return schedulerJobRecords;
        }

        @NotNull
        private SolrSchedulerJobRecordImpl createInternalJobRecord() {
            SolrSchedulerJobRecordImpl internalRecord = new SolrSchedulerJobRecordImpl();
            ReflectionTestUtils.setField(internalRecord, "job", getInternalJob());
            ReflectionTestUtils.setField(internalRecord, "type", INTERNAL_EVENT_DRIVEN_JOB);
            return internalRecord;
        }

        @NotNull
        private SolrSchedulerJobRecordImpl createQuartzJobRecord() {
            SolrSchedulerJobRecordImpl quartzRecord = new SolrSchedulerJobRecordImpl();
            ReflectionTestUtils.setField(quartzRecord, "job", getQuartsJob());
            ReflectionTestUtils.setField(quartzRecord, "type", QUARTZ_SCHEDULE_DRIVEN_JOB);
            return quartzRecord;
        }

        @NotNull
        private SolrSchedulerJobRecordImpl createFileJobRecord() {
            SolrSchedulerJobRecordImpl fileJobRecord = new SolrSchedulerJobRecordImpl();
            ReflectionTestUtils.setField(fileJobRecord, "job", getFileEventJob());
            ReflectionTestUtils.setField(fileJobRecord, "type", FILE_EVENT_DRIVEN_JOB);
            return fileJobRecord;
        }

        @Override
        public long getTotalNumberOfResults() {
            // total number is 3 for the test
            return 3;
        }

        @Override
        public long getQueryResponseTime() {
            return 0;
        }

        private String getInternalJob() {
            return "{\"agentName\":\"scheduler-agent\",\"jobName\":\"jobName1\",\"contextId\":\"CONTEXT-NOT-SO-COMPLEX\",\"childContextIds\":[\"childId1\"],\"startupControlType\":\"AUTOMATIC\",\"minExecutionTime\":10,\"maxExecutionTime\":10000,\"contextParameters\":[],\"identifier\":\"scheduler-agent-jobName1\"}";
        }

        private String getQuartsJob() {
            return "{\"agentName\":\"scheduler-agent\",\"jobName\":\"jobName3\",\"contextId\":\"CONTEXT-NOT-SO-COMPLEX\",\"childContextIds\":[\"childId3\"],\"startupControlType\":\"AUTOMATIC\",\"cronExpression\":\"0 0 * ? * * *\",\"ignoreMisfire\":true,\"eager\":false,\"maxEagerCallbacks\":0,\"passthroughProperties\":{},\"persistentRecovery\":true,\"recoveryTolerance\":1800000,\"identifier\":\"scheduler-agent-jobName3\"}";
        }

        private String getFileEventJob() {
            return "{\"agentName\":\"scheduler-agent\",\"jobName\":\"jobName2\",\"contextId\":\"CONTEXT-NOT-SO-COMPLEX\",\"childContextIds\":[\"childId2\"],\"startupControlType\":\"AUTOMATIC\",\"cronExpression\":\"0 0 * ? * * *\",\"ignoreMisfire\":true,\"eager\":false,\"maxEagerCallbacks\":0,\"passthroughProperties\":{},\"persistentRecovery\":true,\"recoveryTolerance\":1800000,\"filenames\":[\"/some/file/path/name.txt\"],\"includeHeader\":false,\"includeTrailer\":false,\"sortByModifiedDateTime\":false,\"sortAscending\":true,\"directoryDepth\":1,\"logMatchedFilenames\":false,\"ignoreFileRenameWhilstScanning\":true,\"minFileAgeSeconds\":0,\"identifier\":\"scheduler-agent-jobName2\"}";
        }
    }

}