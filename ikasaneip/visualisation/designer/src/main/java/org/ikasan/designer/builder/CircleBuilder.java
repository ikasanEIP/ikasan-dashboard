package org.ikasan.designer.builder;

import org.ikasan.designer.model.Circle;
import org.ikasan.designer.model.Port;
import org.ikasan.designer.model.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CircleBuilder {
    protected String type = "draw2d.shape.basic.Circle";
    protected String id = UUID.randomUUID().toString();
    protected int alpha = 1;
    protected boolean selectable;
    protected boolean draggable;
    protected int angle;
    protected String cssClass = "draw2d_shape_basic_Circle";
    protected String composite;

    protected double x;
    protected double y;
    protected double width = 100;
    protected double height = 75;

    private List<Port> ports = new ArrayList<>();
    private String bgColor = "rgba(230,145,56,0.132)";
    private String color = "rgba(27,27,27,1)";
    private int stroke = 2;
    private String dasharray = null;

    public CircleBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CircleBuilder withAlpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public CircleBuilder withSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public CircleBuilder withDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public CircleBuilder withAngle(int angle) {
        this.angle = angle;
        return this;
    }

    public CircleBuilder withComposite(String composite) {
        this.composite = composite;
        return this;
    }

    public CircleBuilder withX(double x) {
        this.x = x;
        return this;
    }

    public CircleBuilder withY(double y) {
        this.y = y;
        return this;
    }

    public CircleBuilder withWidth(double width) {
        this.width = width;
        return this;
    }

    public CircleBuilder withHeight(double height) {
        this.height = height;
        return this;
    }

    public CircleBuilder addPort(Port port) {
        if(this.ports == null) {
            this.ports = new ArrayList<>();
        }

        this.ports.add(port);
        return this;
    }

    public CircleBuilder withBgColor(String bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public CircleBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public CircleBuilder withStroke(int stroke) {
        this.stroke = stroke;
        return this;
    }


    public CircleBuilder withDasharray(String dasharray) {
        this.dasharray = dasharray;
        return this;
    }

    public CircleBuilder withTopAndBottomPorts() {
        PortBuilder bottomPortBuilder = new PortBuilder();
        bottomPortBuilder.witLocator("draw2d.layout.locator.BottomLocator")
            .withName("bottomHybridSource");

        PortBuilder topPortBuilder = new PortBuilder();
        topPortBuilder.witLocator("draw2d.layout.locator.TopLocator")
            .withName("topHybridTarget");

        this.addPort(bottomPortBuilder.build())
            .addPort(topPortBuilder.build());

        return this;
    }

    public CircleBuilder withLeftAndRightPorts() {
        PortBuilder bottomPortBuilder = new PortBuilder();
        bottomPortBuilder.witLocator("draw2d.layout.locator.RightLocator")
            .withName("rightHybridSource");

        PortBuilder topPortBuilder = new PortBuilder();
        topPortBuilder.witLocator("draw2d.layout.locator.LeftLocator")
            .withName("leftHybridTarget");

        this.addPort(bottomPortBuilder.build())
            .addPort(topPortBuilder.build());

        return this;
    }



    public Circle build() {

        Circle circle = new Circle();
        circle.setId(this.id);
        circle.setType(this.type);
        circle.setAlpha(this.alpha);
        circle.setSelectable(this.selectable);
        circle.setDraggable(this.draggable);
        circle.setAlpha(this.alpha);
        circle.setCssClass(this.cssClass);
        circle.setComposite(this.composite);
        circle.setX(this.x);
        circle.setY(this.y);
        circle.setWidth(this.width);
        circle.setHeight(this.height);
        circle.setPorts(this.ports);
        circle.setBgColor(this.bgColor);
        circle.setColor(this.color);
        circle.setStroke(this.stroke);
        circle.setDasharray(this.dasharray);

        return circle;
    }
}
