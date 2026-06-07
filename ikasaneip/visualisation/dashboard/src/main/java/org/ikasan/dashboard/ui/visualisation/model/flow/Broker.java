package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a Broker node in a designer model, extending the functionality
 * of the {@link AbstractSingleTransition} class. A Broker serves as a specific
 * component in the model with a defined single directional transition and associated metadata.
 *
 * This class uses a builder pattern to construct instances and enforces mandatory fields
 * such as the unique identifier, name, and transition.
 */
public class Broker extends AbstractSingleTransition
{
	public static final String IMAGE = "frontend/images/broker.png";


	/**
     * Constructs a new Broker instance with the specified parameters.
     *
     * @param id The unique identifier for the Broker instance, encapsulated in a {@link DesignerItemIdentifier}.
     * @param name The name of the Broker instance.
     * @param transitionLabel A label associated with the transition of the Broker instance.
     * @param transition The transition node linked to this Broker instance.
     */
    public Broker(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
	{
		super(id, name, transition, transitionLabel, IMAGE);
	}

    /**
     * Creates and returns a new instance of {@code BrokerBuilder}.
     * The {@code BrokerBuilder} is used to construct instances of {@code Broker}
     * by specifying the necessary attributes such as identifier, name, transition
     * label, and transition node.
     *
     * @return a new instance of {@code BrokerBuilder} for building {@code Broker} objects
     */
    public static BrokerBuilder brokerBuilder()
    {
        return new BrokerBuilder();
    }

    /**
     * Builder class
     */
    public static class BrokerBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the Broker being built.
         *
         * @param id the identifier of type DesignerItemIdentifier to be set for the Broker
         * @return the current instance of BrokerBuilder with the updated identifier
         */
        public BrokerBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the Broker being built.
         *
         * @param name the name to assign to the Broker
         * @return the current instance of BrokerBuilder for method chaining
         */
        public BrokerBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the broker being built.
         *
         * @param transitionLabel the transition label to be assigned
         * @return the current instance of {@code BrokerBuilder} with the updated transition label
         */
        public BrokerBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the broker being built.
         *
         * @param transition the {@code Node} instance representing the transition
         *                   to be associated with the broker
         * @return the current {@code BrokerBuilder} instance for method chaining
         */
        public BrokerBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }


        /**
         * Builds and returns a new {@link Broker} instance using the attributes set in the builder.
         * This method validates that the mandatory fields {@code id}, {@code name}, and {@code transition}
         * are not null before creating the {@link Broker} object.
         *
         * @return a new {@link Broker} instance based on the provided attributes
         * @throws IllegalStateException if {@code id}, {@code name}, or {@code transition} are null
         */
        public Broker build()
        {
            if (id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create Broker. id, name and transition cannot be null!");
            }

            return new Broker(id, name, transitionLabel, transition);
        }
    }
}
