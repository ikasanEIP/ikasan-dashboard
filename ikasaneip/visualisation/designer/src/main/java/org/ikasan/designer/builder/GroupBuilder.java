package org.ikasan.designer.builder;

import org.ikasan.designer.model.Circle;
import org.ikasan.designer.model.Group;
import org.ikasan.designer.model.Port;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupBuilder {
    protected String type = "draw2d.shape.composite.Group";
    protected String id = UUID.randomUUID().toString();
    protected String cssClass = "draw2d_shape_composite_Group";
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected int alpha = 1;
    protected boolean selectable = true;
    protected boolean draggable = true;

    protected String composite;


    private List<Port> ports = new ArrayList<>();
    private String bgColor = "rgba(0,0,0,0)";
    private String color = "rgba(0,0,0,0)";
    private int stroke = 1;
    private String dasharray = null;

    public GroupBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public GroupBuilder withAlpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public GroupBuilder withSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public GroupBuilder withDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public GroupBuilder withComposite(String composite) {
        this.composite = composite;
        return this;
    }

    public GroupBuilder withX(double x) {
        this.x = x;
        return this;
    }

    public GroupBuilder withY(double y) {
        this.y = y;
        return this;
    }

    public GroupBuilder withWidth(double width) {
        this.width = width;
        return this;
    }

    public GroupBuilder withHeight(double height) {
        this.height = height;
        return this;
    }

    public GroupBuilder addPort(Port port) {
        if(this.ports == null) {
            this.ports = new ArrayList<>();
        }

        this.ports.add(port);
        return this;
    }

    public GroupBuilder withBgColor(String bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public GroupBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public GroupBuilder withStroke(int stroke) {
        this.stroke = stroke;
        return this;
    }


    public GroupBuilder withDasharray(String dasharray) {
        this.dasharray = dasharray;
        return this;
    }

    public Group build() {

        Group group = new Group();
        group.setId(this.id);
        group.setType(this.type);
        group.setAlpha(this.alpha);
        group.setSelectable(this.selectable);
        group.setDraggable(this.draggable);
        group.setAlpha(this.alpha);
        group.setCssClass(this.cssClass);
        group.setComposite(this.composite);
        group.setX(this.x);
        group.setY(this.y);
        group.setWidth(this.width);
        group.setHeight(this.height);
        group.setPorts(this.ports);
        group.setBgColor(this.bgColor);
        group.setColor(this.color);
        group.setStroke(this.stroke);
        group.setDasharray(this.dasharray);

        return group;
    }
}
