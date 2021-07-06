package org.ikasan.scheduled.dao;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
}
