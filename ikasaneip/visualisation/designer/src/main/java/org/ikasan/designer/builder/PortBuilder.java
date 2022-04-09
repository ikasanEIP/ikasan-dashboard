package org.ikasan.designer.builder;

import org.ikasan.designer.model.Port;
import org.ikasan.designer.model.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PortBuilder {
    protected String type = "draw2d.HybridPort";
    protected String id = UUID.randomUUID().toString();
    protected int alpha;
    protected boolean selectable;
    protected boolean draggable;
    protected int angle;
    protected String cssClass = "draw2d_HybridPort";
    protected String composite;

    private int width;
    private int height;
    private String bgColor;
    private String color;
    private int stroke;
    private long maxFanOut;
    private String name;
    private String semanticGroup = "global";
    private String port = "draw2d.HybridPort";
    private String locator = "draw2d.layout.locator.InputPortLocator";

    protected PortBuilder() {

    }

    public PortBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public PortBuilder withAlpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public PortBuilder withSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public PortBuilder withDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public PortBuilder withAngle(int angle) {
        this.angle = angle;
        return this;
    }

    public PortBuilder withComposite(String composite) {
        this.composite = composite;
        return this;
    }

    public PortBuilder withWidth(int width) {
        this.width = width;
        return this;
    }

    public PortBuilder withHeight(int height) {
        this.height = height;
        return this;
    }


    public PortBuilder withBgColor(String bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public PortBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public PortBuilder withStroke(int stroke) {
        this.stroke = stroke;
        return this;
    }

    public PortBuilder withMaxFanOut(long maxFanOut) {
        this.maxFanOut = maxFanOut;
        return this;
    }

    public PortBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public Port build() {
        Port port = new Port();
        port.setId(this.id);
        port.setAlpha(this.alpha);
        port.setSelectable(this.selectable);
        port.setDraggable(this.draggable);
        port.setAlpha(this.alpha);
        port.setCssClass(this.cssClass);
        port.setComposite(this.composite);
        port.setWidth(this.width);
        port.setHeight(this.height);
        port.setBgColor(this.bgColor);
        port.setColor(this.color);
        port.setStroke(this.stroke);
        port.setMaxFanOut(this.maxFanOut);
        port.setName(this.name);
        port.setSemanticGroup(this.semanticGroup);
        port.setPort(this.port);
        port.setLocator(this.locator);

        return port;
    }
}
