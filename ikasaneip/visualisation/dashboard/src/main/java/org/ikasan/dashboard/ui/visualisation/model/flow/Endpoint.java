package org.ikasan.dashboard.ui.visualisation.model.flow;

/**
 * Represents an endpoint in a system or flow. An endpoint typically marks a specific
 * node in a workflow or process where certain actions, transitions, or processing occur.
 * This interface serves as a marker to group and manage all endpoint-related types
 * within a flow.
 *
 * Classes implementing this interface should represent distinct types of endpoints,
 * such as message endpoints or dead-end points, and may include specific functionalities
 * and attributes suited to their respective use cases.
 *
 * Implementers of this interface may define:
 * - Identification for the endpoint.
 * - Labels or metadata relevant to the endpoint.
 * - Transition logic or connections to other workflow components.
 */
public interface Endpoint {
}
