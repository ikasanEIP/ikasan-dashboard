package org.ikasan.designer.builder;

import org.ikasan.designer.model.Connection;
import org.ikasan.designer.model.ConnectionDetails;
import org.ikasan.designer.model.Port;
import org.ikasan.designer.model.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConnectionBuilder {
    protected String type = "draw2d.Connection";
    protected String id = UUID.randomUUID().toString();
    protected int alpha = 1;
    protected boolean selectable = false;
    protected boolean draggable = false;
    protected int angle = 0;
    protected String cssClass = "draw2d_Connection";
    protected String composite;

    private int stroke = 2;
    private String color = "rgba(0,0,0,1)";
    private int outlineStroke;
    private String outlineColor = "rgba(255,255,255,1)";
    private String policy = "draw2d.policy.line.LineSelectionFeedbackPolicy";
    private List<Vertex> vertex;
    private String router = "draw2d.layout.connection.ManhattanConnectionRouter";
    private int radius = 5;
    private ConnectionDetails source;
    private ConnectionDetails target;

    protected ConnectionBuilder() {

    }

    public ConnectionBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ConnectionBuilder withAlpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public ConnectionBuilder withSelectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public ConnectionBuilder withDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public ConnectionBuilder withAngle(int angle) {
        this.angle = angle;
        return this;
    }

    public ConnectionBuilder withComposite(String composite) {
        this.composite = composite;
        return this;
    }

    public ConnectionBuilder withColor(String color) {
        this.color = color;
        return this;
    }

    public ConnectionBuilder withStroke(int stroke) {
        this.stroke = stroke;
        return this;
    }

    public ConnectionBuilder withOutlineStroke(int outlineStroke) {
        this.outlineStroke = outlineStroke;
        return this;
    }

    public ConnectionBuilder withOutlineColor(String outlineColor) {
        this.outlineColor = outlineColor;
        return this;
    }

    public ConnectionBuilder withPolicy(String policy) {
        this.policy = policy;
        return this;
    }

    public ConnectionBuilder addVertex(Vertex vertex) {
        if(this.vertex == null) {
            this.vertex = new ArrayList<>();
        }

        this.vertex.add(vertex);
        return this;
    }

    public ConnectionBuilder withRouter(String router) {
        this.router = router;
        return this;
    }

    public ConnectionBuilder withRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public ConnectionBuilder withSource(ConnectionDetails source) {
        this.source = source;
        return this;
    }

    public ConnectionBuilder withTarget(ConnectionDetails target) {
        this.source = target;
        return this;
    }

    public Connection build() {
        Connection connection = new Connection();
        connection.setId(this.id);
        connection.setAlpha(this.alpha);
        connection.setSelectable(this.selectable);
        connection.setDraggable(this.draggable);
        connection.setAlpha(this.alpha);
        connection.setCssClass(this.cssClass);
        connection.setComposite(this.composite);
        connection.setColor(this.color);
        connection.setStroke(this.stroke);

        return connection;
    }
}
