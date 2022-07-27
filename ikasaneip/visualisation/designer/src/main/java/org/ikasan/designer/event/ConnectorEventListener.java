package org.ikasan.designer.event;

public interface ConnectorEventListener {

    /**
     * Called when a connectorEvent occurs such as the addition or removal of a connection.
     *
     * @param connectorEvent
     */
    void connectorEvent(ConnectorEvent connectorEvent);
}
