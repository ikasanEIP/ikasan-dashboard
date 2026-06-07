package org.ikasan.dashboard.ui.visualisation.model.flow;

/**
 * Constants representing the status of a node search or lookup operation.
 * This class provides string constants to indicate whether a node was found,
 * not found, or if the search result is empty.
 */
public class NodeFoundStatus
{
    /**
     * Indicates that the search result is empty or no search was performed.
     */
    public static final String EMPTY = "EMPTY";

    /**
     * Indicates that the node was successfully found.
     */
    public static final String FOUND = "FOUND";

    /**
     * Indicates that the node was not found.
     */
    public static final String NOT_FOUND = "NOT_FOUND";
}
