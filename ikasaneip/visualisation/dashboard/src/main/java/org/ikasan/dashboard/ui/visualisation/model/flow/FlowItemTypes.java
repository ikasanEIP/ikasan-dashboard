package org.ikasan.dashboard.ui.visualisation.model.flow;

/**
 * Constants representing different types of flow items in a visualization.
 * These types are used to categorize and identify various elements and components
 * within a flow diagram, including wiretaps, logging points, and control elements.
 */
public class FlowItemTypes {
    /**
     * Represents a wiretap that is placed before a component in the flow.
     */
    public static final String BEFORE_WIRETAP = "BEFORE_WIRETAP";

    /**
     * Represents a wiretap that is placed after a component in the flow.
     */
    public static final String AFTER_WIRETAP = "AFTER_WIRETAP";

    /**
     * Represents a logging wiretap that is placed before a component in the flow.
     */
    public static final String BEFORE_LOGGING_WIRETAP = "BEFORE_LOGGING_WIRETAP";

    /**
     * Represents a logging wiretap that is placed after a component in the flow.
     */
    public static final String AFTER_LOGGING_WIRETAP = "AFTER_LOGGING_WIRETAP";

    /**
     * Represents a standard flow component.
     */
    public static final String FLOW_COMPONENT = "FLOW_COMPONENT";

    /**
     * Represents the flow startup control element.
     */
    public static final String FLOW_START_UP_CONTROL = "FLOW_START_UP_CONTROL";

    /**
     * Represents a flow recording element.
     */
    public static final String FLOW_RECORDING = "FLOW_RECORDING";
}
