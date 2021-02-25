package org.ikasan.designer.model;

public class Figure {
    private String identifier;
    private int x;
    private int y;
    private int width;
    private int height;
    private String type;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Figure{");
        sb.append("identifier='").append(identifier).append('\'');
        sb.append(", x=").append(x);
        sb.append(", y=").append(y);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
