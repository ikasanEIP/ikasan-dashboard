package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.designer.model.Image;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Draw2dCanvasJsonHelper {

    private static ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Retrieves scheduler job images from the provided canvas JSON.
     *
     * @param canvasJson JSON representing the canvas containing scheduler job images
     * @return A map containing scheduler job images with image id as key and Image object as value
     */
    public static Map<String, Image> getSchedulerJobImagesFromCanvasJson(String canvasJson) {
        Map<String, Image> schedulerJobs = new HashMap<>();

        try {
            List<LinkedHashMap> values = OBJECT_MAPPER.readValue(canvasJson, List.class);

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsBytes(value), Image.class);
                    if (!image.getPath().contains("repeating.png")) schedulerJobs.put(image.getId(), image);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return schedulerJobs;
    }
}
