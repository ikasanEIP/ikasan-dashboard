package org.ikasan.designer.util;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DynamicImageHelper {

    /**
     * Helper methog to load the dynamic images.
     * @param imagePath
     * @return
     * @throws IOException
     */
    public static ArrayList<Image> loadDynamicImages(String imagePath) throws IOException {
        ArrayList<Image> images = new ArrayList<>();

        Files.list(Paths.get(imagePath)).forEach(file -> {
            Image image = getImage(file);
            if(image != null) {
                images.add(image);
            }
        });

        return images;
    }

    private static Image getImage(Path file) {

        StreamResource res = new StreamResource(file.getFileName().toString(), () -> {
            // eg. load image data from classpath (src/main/resources/images/image.png)
            try {
                return new FileInputStream(file.toFile());
            }
            catch (FileNotFoundException e) {
                return null;
            }
            catch (IOException e) {
                return null;
            }
        });

        return new Image(res, "");
    }
}
