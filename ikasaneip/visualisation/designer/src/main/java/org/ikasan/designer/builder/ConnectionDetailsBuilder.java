package org.ikasan.designer.builder;

import org.ikasan.designer.model.ConnectionDetails;

public class ConnectionDetailsBuilder {
    private String node;
    private String port;
    private String decoration;

    public ConnectionDetailsBuilder withNode(String node) {
        this.node = node;
        return this;
    }

    public ConnectionDetailsBuilder withPort(String port) {
        this.port = port;
        return this;
    }

    public ConnectionDetailsBuilder withDecoration(String decoration) {
        this.decoration = decoration;
        return this;
    }

    public ConnectionDetails build() {
        ConnectionDetails connectionDetails = new ConnectionDetails();
        connectionDetails.setNode(this.node);
        connectionDetails.setPort(this.port);
        connectionDetails.setDecoration(this.decoration);

        return connectionDetails;
    }
}
