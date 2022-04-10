package org.ikasan.designer.builder;

import org.ikasan.designer.model.Port;
import org.ikasan.designer.model.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RectangleBuilder {
    protected String type = "draw2d.shape.basic.Rectangle";
    protected String id = UUID.randomUUID().toString();
    protected int alpha = 1;
    protected boolean selectable;
    protected boolean draggable;
    protected int angle;
    protected String cssClass = "draw2d_shape_basic_Rectangle";
    protected String composite;

    protected double x;
    protected double y;
    protected double width = 100;
    protected double height = 75;

    private List<Port> ports = new ArrayList<>();
    private String bgColor = "rgba(230,145,56,0.132)";
    private String color = "rgba(27,27,27,1)";
    private int stroke = 2;
    private int radius = 5;
    private String dasharray = null;

    public RectangleBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RectangleBuilder withAlpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public RectangleBuilder withSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public RectangleBuilder withDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public RectangleBuilder withAngle(int angle) {
        this.angle = angle;
        return this;
    }

    public RectangleBuilder withComposite(String composite) {
        this.composite = composite;
        return this;
    }

    public RectangleBuilder withX(double x) {
        this.x = x;
        return this;
    }

    public RectangleBuilder withY(double y) {
        this.y = y;
        return this;
    }

    public RectangleBuilder withWidth(double width) {
        this.width = width;
        return this;
    }

    public RectangleBuilder withHeight(double height) {
        this.height = height;
        return this;
    }

    public RectangleBuilder addPort(Port port) {
        if(this.ports == null) {
            this.ports = new ArrayList<>();
        }

        this.ports.add(port);
        return this;
    }

    public RectangleBuilder withBgColor(String bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public RectangleBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public RectangleBuilder withStroke(int stroke) {
        this.stroke = stroke;
        return this;
    }

    public RectangleBuilder withRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public RectangleBuilder withDasharray(String dasharray) {
        this.dasharray = dasharray;
        return this;
    }

    public Rectangle build() {
        PortBuilder bottomPortBuilder = new PortBuilder();
        bottomPortBuilder.witLocator("draw2d.layout.locator.BottomLocator")
        .withName("hybridSource");

        PortBuilder topPortBuilder = new PortBuilder();
        topPortBuilder.witLocator("draw2d.layout.locator.TopLocator")
            .withName("hybridTarget");

        this.addPort(bottomPortBuilder.build())
            .addPort(topPortBuilder.build());

        Rectangle rectangle = new Rectangle();
        rectangle.setId(this.id);
        rectangle.setType(this.type);
        rectangle.setAlpha(this.alpha);
        rectangle.setSelectable(this.selectable);
        rectangle.setDraggable(this.draggable);
        rectangle.setAlpha(this.alpha);
        rectangle.setCssClass(this.cssClass);
        rectangle.setComposite(this.composite);
        rectangle.setX(this.x);
        rectangle.setY(this.y);
        rectangle.setWidth(this.width);
        rectangle.setHeight(this.height);
        rectangle.setPorts(this.ports);
        rectangle.setBgColor(this.bgColor);
        rectangle.setColor(this.color);
        rectangle.setStroke(this.stroke);
        rectangle.setRadius(this.radius);
        rectangle.setDasharray(this.dasharray);

        return rectangle;
    }
}
