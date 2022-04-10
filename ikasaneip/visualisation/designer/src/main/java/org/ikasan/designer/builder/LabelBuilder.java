package org.ikasan.designer.builder;

import org.ikasan.designer.model.Label;
import org.ikasan.designer.model.Port;
import org.ikasan.designer.model.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LabelBuilder {
    protected String type = "draw2d.shape.basic.Text";
    protected String id = UUID.randomUUID().toString();
    protected int alpha = 1;
    protected boolean selectable = false;
    protected boolean draggable = false;
    protected int angle = 0;
    protected String cssClass = "draw2d_shape_basic_Text";
    protected String composite;

    protected double x;
    protected double y;
    protected double width = 100;
    protected double height = 15;

    private List<Port> ports = new ArrayList<>();
    private String bgColor = "rgba(255,255,255,0)";
    private String color = "rgba(255,255,255,0)";
    private int stroke = 1;
    private int radius = 0;
    private String text;
    private int outlineStroke = 0;
    private String outlineColor = "rgba(255,255,255,0)";
    private String fontSize = "10pt";
    private String fontColor = "rgba(13,13,13,1)";
    private String fontFamily = "\"Trebuchet MS\", Helvetica, sans-serif";

    public LabelBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public LabelBuilder withAlpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public LabelBuilder withSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public LabelBuilder withDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public LabelBuilder withAngle(int angle) {
        this.angle = angle;
        return this;
    }

    public LabelBuilder withComposite(String composite) {
        this.composite = composite;
        return this;
    }

    public LabelBuilder withX(double x) {
        this.x = x;
        return this;
    }

    public LabelBuilder withY(double y) {
        this.y = y;
        return this;
    }

    public LabelBuilder withWidth(double width) {
        this.width = width;
        return this;
    }

    public LabelBuilder withHeight(double height) {
        this.height = height;
        return this;
    }

    public LabelBuilder addPort(Port port) {
        if(this.ports == null) {
            this.ports = new ArrayList<>();
        }

        this.ports.add(port);
        return this;
    }

    public LabelBuilder withBgColor(String bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public LabelBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public LabelBuilder withStroke(int stroke) {
        this.stroke = stroke;
        return this;
    }

    public LabelBuilder withRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public LabelBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public LabelBuilder withOutlineStroke(int outlineStroke) {
        this.outlineStroke = outlineStroke;
        return this;
    }

    public LabelBuilder withOutlineColor(String outlineColor) {
        this.outlineColor = outlineColor;
        return this;
    }

    public LabelBuilder withFontSize(String fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public LabelBuilder withFontColor(String fontColor) {
        this.fontColor = fontColor;
        return this;
    }

    public LabelBuilder withFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    public Label build() {
        Label rectangle = new Label();
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
        rectangle.setText(this.text);
        rectangle.setOutlineColor(this.outlineColor);

        return rectangle;
    }
}
