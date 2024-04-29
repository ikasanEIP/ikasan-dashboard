package org.ikasan.dashboard.ui.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Assert;
import org.ikasan.dashboard.AbstractTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.dag.component.DagNode;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateToDagConverter;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ContextTemplateToDagConverterTest extends AbstractTest {

    ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    @Test
    public void test_sample_context_with_multiple_and_roots() throws IOException {
        String context = loadDataFile("/data/contexts/-1793100514.json");
        ContextInstance contextInstance = objectMapper.readValue(context, ContextInstance.class);
        ContextTemplateToDagConverter contextTemplateToDagConverter = new ContextTemplateToDagConverter();
        List<DagNode> nodes = contextTemplateToDagConverter.convert(contextInstance);

        Assert.notNull(nodes);
    }
}
