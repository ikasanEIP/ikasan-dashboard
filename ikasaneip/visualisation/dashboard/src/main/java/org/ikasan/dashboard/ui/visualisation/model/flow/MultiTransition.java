package org.ikasan.dashboard.ui.visualisation.model.flow;


import java.util.Map;

/**
 * Represents the ability to manage multiple transitions in a connected system or workflow.
 * Implementations of this interface provide a mechanism for retrieving a collection
 * of transitions, which map string identifiers to corresponding Node objects.
 */
public interface MultiTransition
{
    /**
     * Retrieves the transitions associated with this instance.
     *
     * @return a map where the keys are transition identifiers of type String
     *         and the values are Node objects representing the corresponding transitions.
     */
    Map<String, Node> getTransitions();
}
