package org.ikasan.scheduled;

import org.ikasan.scheduled.dao.SolrScheduledProcessEventDao;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
        List<String> agents = this.dao.getAllAgents();
        System.out.println(agents);

        agents.forEach(agent -> {
            dao.getJobGroupsForAgent(agent).forEach(jobGroup -> {
                System.out.println(jobGroup);
                dao.getJobsForAgentAndJobGroup(agent, jobGroup).forEach(job -> System.out.println(job));
            });
        });
    }
}
