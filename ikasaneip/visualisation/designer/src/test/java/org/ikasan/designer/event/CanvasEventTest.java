package org.ikasan.designer.event;

import org.apache.commons.io.IOUtils;
import org.ikasan.designer.json.DesignerDynamicImageManager;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

@Ignore
public class CanvasEventTest {

    public static final String DESIGNER_JSON = "/data/graph/sample-context.json";

    @Test
    public void test_parse_json() throws IOException {
        CanvasEvent canvasEvent = new CanvasEvent(loadDataFile(DESIGNER_JSON));

        System.out.println(canvasEvent);
    }

    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }
}
