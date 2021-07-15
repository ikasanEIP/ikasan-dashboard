package org.ikasan.scheduled.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.scheduled.model.Outcome;
import org.ikasan.scheduled.model.SolrScheduledProcessEvent;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Ignore
public class ScheduleProcessEventDaoTest {

    private SolrScheduledProcessEventDao dao;

    @Before
    public void setup() {
        dao = new SolrScheduledProcessEventDao();
        dao.initStandalone("http://localhost:8983/solr", 30);
        dao.setSolrUsername("ikasan");
        dao.setSolrPassword("1ka5an");
    }

    @Test
    public void test() {
        List<String> agents = this.dao.getAllAgentNames();
        System.out.println(agents);

        agents.forEach(agent -> {
            dao.getJobGroupsForAgent(agent).forEach(jobGroup -> {
                System.out.println(jobGroup);
                dao.getJobsForAgentAndJobGroup(agent, jobGroup).forEach(job -> System.out.println(job));
            });
        });
    }

    @Test
    public void testHashcode() throws IOException {
        String event1 = loadDataFile("/data/scheduledEvent1.json");
        String event2 = loadDataFile("/data/scheduledEvent2.json");
        ObjectMapper mapper = new ObjectMapper();

        ScheduledProcessEvent<Outcome> scheduledProcessEvent1 = mapper.readValue(event1, SolrScheduledProcessEvent.class);
        ScheduledProcessEvent<Outcome> scheduledProcessEvent2 = mapper.readValue(event2, SolrScheduledProcessEvent.class);

        Assert.assertEquals(scheduledProcessEvent1.hashCode(), scheduledProcessEvent2.hashCode());
    }

    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}
