package org.ikasan.designer.builder;

import org.ikasan.designer.model.Vertex;

public class VertexBuilder {
    private double x = 0;
    private double y = 0;

    public VertexBuilder withX(double x) {
        this.x = x;
        return this;
    }

    public VertexBuilder withY(double y) {
        this.y = y;
        return this;
    }

    public Vertex build() {
        Vertex vertex = new Vertex();
        vertex.setX(this.x);
        vertex.setY(this.y);

        return vertex;
    }
}
