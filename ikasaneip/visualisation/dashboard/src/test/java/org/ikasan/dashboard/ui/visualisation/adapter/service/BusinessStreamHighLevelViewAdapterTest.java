package org.ikasan.dashboard.ui.visualisation.adapter.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class BusinessStreamHighLevelViewAdapterTest {

    @Test
    public void test() throws IOException {
        String json = this.loadDataFile("/data/businessStream/business-stream-high-level.json");

        BusinessStreamHighLevelViewAdapter adapter = new BusinessStreamHighLevelViewAdapter();
        String diagramJson = adapter.adaptView(json);

        System.out.println(diagramJson);
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
