package org.ikasan.designer.json;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

@Ignore
public class DesignerJsonParserTest {

    public static final String DESIGNER_JSON = "/data/graph/designer.json";

//    @Test
//    public void test_parse_json() throws IOException {
//        DesignerDynamicImageManager parser = new DesignerDynamicImageManager("src/test/resources/images");
//        String jsonObject = parser.parse(this.loadDataFile(DESIGNER_JSON));
//
//        System.out.println(jsonObject);
//    }
//
//    protected String loadDataFile(String fileName) throws IOException
//    {
//        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");
//
//        return contentToSend;
//    }
//
//    protected InputStream loadDataFileStream(String fileName) throws IOException
//    {
//        return getClass().getResourceAsStream(fileName);
//    }
}
