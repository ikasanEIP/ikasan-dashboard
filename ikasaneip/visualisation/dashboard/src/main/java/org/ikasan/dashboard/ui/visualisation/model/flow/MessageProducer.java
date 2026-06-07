package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a MessageProducer in a designer model. This class extends the functionality
 * of AbstractSingleTransition and is designed to model a component with a single transition
 * to another node within the system.
 *
 * The MessageProducer provides a static builder class, {@link MessageProducerBuilder},
 * for constructing instances in a flexible and readable way using a fluent API.
 */
public class MessageProducer extends AbstractSingleTransition
{
	public static final String IMAGE = "frontend/images/channel-adapter.png";

    /**
     * Constructs a MessageProducer instance with the specified parameters.
     *
     * @param id the unique identifier associated with this MessageProducer
     * @param name the name of the MessageProducer
     * @param transitionLabel the label describing the transition to the target node
     * @param transition the target node to which this MessageProducer transitions
     */
	public MessageProducer(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
    {
        super(id, name, transition, transitionLabel, IMAGE);
    }

    /**
     * Creates a new instance of {@link MessageProducerBuilder}, which provides a fluent API
     * for configuring and building a {@link MessageProducer}.
     *
     * @return a new instance of {@link MessageProducerBuilder} to construct a {@link MessageProducer}.
     */
    public static MessageProducerBuilder messageProducerBuilder()
    {
        return new MessageProducerBuilder();
    }

    /**
     * Builder class
     */
    public static class MessageProducerBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the MessageProducer being built.
         *
         * @param id the identifier of type {@code DesignerItemIdentifier} to associate with the MessageProducer
         * @return the current {@code MessageProducerBuilder} instance for method chaining
         */
        public MessageProducerBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the message producer being built.
         *
         * @param name the name to assign to the message producer
         * @return the current instance of {@code MessageProducerBuilder} for method chaining
         */
        public MessageProducerBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the message producer being built.
         *
         * @param transitionLabel the label to identify the transition for the message producer
         * @return the updated instance of the MessageProducerBuilder
         */
        public MessageProducerBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the message producer.
         *
         * @param transition the Node instance representing the transition to assign
         * @return the updated instance of {@code MessageProducerBuilder} for method chaining
         */
        public MessageProducerBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }

        /**
         * Constructs and returns a new instance of {@link MessageProducer} using the parameters
         * provided to the builder. The method validates that all mandatory fields (id, name, and
         * transition) are non-null before creating the object.
         *
         * @return a fully constructed {@link MessageProducer} instance
         * @throws IllegalStateException if any of the required fields (id, name, or transition) is null
         */
        public MessageProducer build()
        {
            if(id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create FtpConsumer. id, name and transition cannot be null!");
            }

            return new MessageProducer(id, name, transitionLabel, transition);
        }
    }
}
