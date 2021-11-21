package org.ikasan.scheduler.core.component.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.status.ContextInstanceStatus;
import org.ikasan.scheduler.core.service.ContextService;
import org.junit.Test;

import java.io.IOException;

public class ContextInstanceToContextInstanceStatusConverterTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextInstanceToContextInstanceStatusConverter converter = new ContextInstanceToContextInstanceStatusConverter();
        ContextInstanceStatus contextInstanceStatus = converter.convert(context);

        ObjectMapper objectMapper = new ObjectMapper();

        // todo some assertions
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextInstanceStatus));
    }
}
