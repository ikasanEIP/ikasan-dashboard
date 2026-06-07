package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a terminal or end point in a designer model that has a single transition
 * with an associated label. The DeadEndPoint serves as a specialized implementation
 * of the {@link AbstractSingleTransition} class and implements both the {@link SingleTransition}
 * and {@link Endpoint} interfaces.
 *
 * This class is designed to define points in a workflow or process where no further
 * transitions are expected, typically indicating the termination of a path.
 */
public class DeadEndPoint extends AbstractSingleTransition implements SingleTransition, Endpoint
{
	public static final String IMAGE = "frontend/images/dead-end-point.png";

    /**
     * Constructs a new `DeadEndPoint` instance.
     *
     * @param id The unique identifier for the designer item.
     * @param name The name of the dead-end point.
     * @param transitionLabel The label associated with the transition.
     * @param transition The transition node associated with this dead-end point.
     */
	private DeadEndPoint(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
    {
        super(id, name, transition, transitionLabel, IMAGE);
    }

    /**
     * Creates and returns a new instance of the DeadEndPointBuilder.
     *
     * @return a new instance of the DeadEndPointBuilder to construct a DeadEndPoint.
     */
    public static DeadEndPointBuilder deadEndPointBuilder()
    {
        return new DeadEndPointBuilder();
    }

    /**
     * Builder class
     */
    public static class DeadEndPointBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the DeadEndPoint being built.
         *
         * @param id the {@code DesignerItemIdentifier} that uniquely identifies the DeadEndPoint
         * @return the current instance of {@code DeadEndPointBuilder} for method chaining
         */
        public DeadEndPointBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the DeadEndPoint being built.
         *
         * @param name the name to set for the DeadEndPoint
         * @return the updated instance of the DeadEndPointBuilder
         */
        public DeadEndPointBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the DeadEndPoint being built.
         *
         * @param transitionLabel the transition label to set
         * @return the current instance of DeadEndPointBuilder for method chaining
         */
        public DeadEndPointBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the builder.
         *
         * @param transition the {@code Node} object representing the transition to be associated with the DeadEndPoint
         * @return the current instance of {@code DeadEndPointBuilder}, enabling method chaining
         */
        public DeadEndPointBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }


        /**
         * Builds and returns a new instance of the DeadEndPoint class using the
         * properties set in the DeadEndPointBuilder. Throws an IllegalStateException
         * if the required properties 'id' or 'name' are not set.
         *
         * @return a fully constructed DeadEndPoint instance.
         * @throws IllegalStateException if either 'id' or 'name' is null.
         */
        public DeadEndPoint build()
        {
            if (id == null || name == null)
            {
                throw new IllegalStateException("Cannot create DeadEndPoint. id and name cannot be null!");
            }

            return new DeadEndPoint(id, name, transitionLabel, transition);
        }
    }
}
