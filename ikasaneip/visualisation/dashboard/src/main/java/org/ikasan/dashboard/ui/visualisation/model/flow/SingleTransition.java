package org.ikasan.dashboard.ui.visualisation.model.flow;


/**
 * Represents an interface for objects that define a single transition in a process flow.
 * Implementing classes should provide the details of the transition target node
 * and its associated label.
 *
 * The interface is typically used in the context of defining relationships
 * or flows between nodes in a model, particularly when the connection is uni-directional.
 *
 * Classes implementing this interface are expected to provide:
 * - A method to retrieve the target transition node.
 * - A method to retrieve the label associated with the transition.
 */
public interface SingleTransition
{
    /**
     * Retrieves the target transition node associated with this object.
     *
     * The target node represents the endpoint for the transition defined
     * by the implementing class, which typically models a directed
     * connection in a process flow.
     *
     * @return the {@code Node} object representing the target of the transition.
     */
    Node getTransition();

    /**
     * Retrieves the label associated with the transition.
     * The label is typically a descriptive string that provides
     * additional context or meaning to the transition in the process flow.
     *
     * @return the label of the transition as a String
     */
    String getTransitionLabel();
}
