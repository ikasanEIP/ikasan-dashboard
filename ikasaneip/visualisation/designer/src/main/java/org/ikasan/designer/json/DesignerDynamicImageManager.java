package org.ikasan.designer.json;

import com.vaadin.flow.component.html.Image;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class DesignerDynamicImageManager {

    public static final String IMAGE = "draw2d.shape.basic.Image";

    private HashMap<String, Image> imageHashMap;

    public DesignerDynamicImageManager(ArrayList<Image> images) {
        this.initImages(images);
    }

    public String parse(String json) throws IOException {

        JSONArray jsonArray = new JSONArray(json);

        jsonArray.forEach(item -> {
            if(item instanceof JSONObject) {
                String type = ((JSONObject)item).getString("type");

                if(type.equals(IMAGE)){
                    this.manageImage((JSONObject)item);
                }
            }
        });

        return jsonArray.toString();
    }


    private void manageImage(JSONObject image) {
        String path = image.getString("path");

        if(path.startsWith("VAADIN/dynamic/resource/")) {
            String imageName = path.substring(path.lastIndexOf("/") + 1);

            Image image1 = this.imageHashMap.get(imageName);

            image.remove("path");
            image.put("path", image1.getSrc());
            image.put("keepAspectRatio", true);
        }
    }

    private void initImages(ArrayList<Image> images)  {
        this.imageHashMap = new HashMap<>();

        images.forEach(image -> {
            String imageName = image.getSrc().substring(image.getSrc().lastIndexOf("/") + 1);
            this.imageHashMap.put(imageName, image);
        });
    }

}
