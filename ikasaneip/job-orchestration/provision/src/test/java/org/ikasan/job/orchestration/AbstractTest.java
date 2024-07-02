package org.ikasan.job.orchestration;

import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class AbstractTest
{
    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }

    protected void loadFileJobs(List<SchedulerJob> schedulerJobs, String filePath) throws IOException {
        List<Path> files =  Files.walk(Paths.get(filePath))
            .filter(Files::isRegularFile)
            .collect(Collectors.toList());

        for (Path f : files) {
            FileInputStream inputStream = new FileInputStream(f.toFile());

            FileEventDrivenJob job = ObjectMapperFactory.newInstance().readValue(inputStream.readAllBytes(), FileEventDrivenJobImpl.class);

            schedulerJobs.add(job);

            inputStream.close();
        }

    }

    protected void loadCommandJobs(List<SchedulerJob> schedulerJobs, String filePath) throws IOException {
        List<Path> files =  Files.walk(Paths.get(filePath))
            .filter(Files::isRegularFile)
            .collect(Collectors.toList());

        for (Path f : files) {
            FileInputStream inputStream = new FileInputStream(f.toFile());

            InternalEventDrivenJob job = ObjectMapperFactory.newInstance().readValue(inputStream.readAllBytes(), InternalEventDrivenJobImpl.class);

            schedulerJobs.add(job);

            inputStream.close();
        }

    }

    protected void loadQuartzJobs(List<SchedulerJob> schedulerJobs, String filePath) throws IOException {
        List<Path> files =  Files.walk(Paths.get(filePath))
            .filter(Files::isRegularFile)
            .collect(Collectors.toList());

        for (Path f : files) {
            FileInputStream inputStream = new FileInputStream(f.toFile());

            QuartzScheduleDrivenJob job = ObjectMapperFactory.newInstance().readValue(inputStream.readAllBytes(), QuartzScheduleDrivenJobImpl.class);

            schedulerJobs.add(job);

            inputStream.close();
        }

    }
}
