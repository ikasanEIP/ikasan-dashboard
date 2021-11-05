package org.ikasan.dashboard.visualisation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.visualisation.model.Image;
import org.junit.Test;

import java.util.ArrayList;

public class VisualisationServiceTest {

    @Test
    public void test() throws JsonProcessingException {

        ArrayList<Image> images = new ArrayList<>();
        int x=0;
        int y=0;

        for(int i=0; i<3000; i++) {
            Image image = new Image();

            image.setPath("frontend/images/flow.png");
            image.setType("draw2d.shape.basic.Image");
            image.setId("image-"+i);
            image.setPorts(new ArrayList<>());
            image.setX(x);
            image.setY(y);
            image.setWidth(95);
            image.setHeight(63);
            image.setAlpha(1);
            image.setDraggable(true);
            image.setSelectable(true);
            image.setAngle(0);
            image.setCssClass("draw2d_shape_basic_Image");

            images.add(image);

            x+=200;

            if(i%50 == 0) {
                y+=100;
                x=0;
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(images));
    }
}
