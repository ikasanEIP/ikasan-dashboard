package org.ikasan.designer.builder;

import org.ikasan.designer.model.Image;
import org.ikasan.designer.model.Port;
import org.ikasan.designer.model.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImageBuilder {
    protected String type = "draw2d.shape.basic.Image";
    protected String id = UUID.randomUUID().toString();
    protected double x;
    protected double y;
    protected double width= 95;
    protected double height = 63;
    protected int alpha = 1;
    protected boolean selectable = true;
    protected boolean draggable = true;
    protected int angle = 0;
    protected String cssClass = "draw2d_shape_basic_Image";
    private List<Port> ports = new ArrayList<>();
    private String path = "frontend/images/flow.png";

    protected ImageBuilder() {

    }

    public ImageBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ImageBuilder withAlpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public ImageBuilder withSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public ImageBuilder withDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public ImageBuilder withAngle(int angle) {
        this.angle = angle;
        return this;
    }

    public ImageBuilder withX(double x) {
        this.x = x;
        return this;
    }

    public ImageBuilder withY(double y) {
        this.y = y;
        return this;
    }

    public ImageBuilder withWidth(double width) {
        this.width = width;
        return this;
    }

    public ImageBuilder withHeight(double height) {
        this.height = height;
        return this;
    }

    public ImageBuilder addPort(Port port) {
        if(this.ports == null) {
            this.ports = new ArrayList<>();
        }

        this.ports.add(port);
        return this;
    }

    public ImageBuilder withPath(String path) {
        this.path = path;
        return this;
    }

    public Image build() {
        PortBuilder bottomPortBuilder = new PortBuilder();
        bottomPortBuilder.witLocator("draw2d.layout.locator.BottomLocator")
            .withName("hybridSource");

        PortBuilder topPortBuilder = new PortBuilder();
        topPortBuilder.witLocator("draw2d.layout.locator.TopLocator")
            .withName("hybridTarget");

        this.addPort(bottomPortBuilder.build())
            .addPort(topPortBuilder.build());

        Image image = new Image();
        image.setId(this.id);
        image.setType(this.type);
        image.setAlpha(this.alpha);
        image.setSelectable(this.selectable);
        image.setDraggable(this.draggable);
        image.setAlpha(this.alpha);
        image.setCssClass(this.cssClass);
        image.setX(this.x);
        image.setY(this.y);
        image.setWidth(this.width);
        image.setHeight(this.height);
        image.setPorts(this.ports);
        image.setPath(this.path);

        return image;
    }
}
