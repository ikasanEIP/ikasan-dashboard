package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;


/**
 * Represents an Event-Driven Consumer entity that extends the {@code Consumer} class.
 * This class is designed to handle event-driven process flows and is identified by its
 * unique identifier, name, transition label, transition node, and source node.
 */
public class EventDrivenConsumer extends Consumer
{
	public static final String IMAGE = "frontend/images/event-driven-consumer.png";

    /**
     * Constructs an instance of {@code EventDrivenConsumer} with the specified properties.
     *
     * @param id The unique identifier of the consumer. Must not be null.
     * @param name The name of the consumer. Represents its descriptive name. Must not be null.
     * @param transitionLabel The label for the transition associated with this consumer. Can be null.
     * @param transition The transition node that this consumer is connected to. Must not be null.
     * @param source The source node from which this consumer originates. Can be null.
     */
	private EventDrivenConsumer(DesignerItemIdentifier id, String name, String transitionLabel, Node transition, Node source)
    {
        super(id, name, transitionLabel, transition, IMAGE, source);
    }

    /**
     * Creates a new instance of {@code EventDrivenConsumerBuilder}.
     * This builder facilitates the creation of {@code EventDrivenConsumer} instances by
     * providing a fluent API for setting required and optional properties.
     *
     * @return a new {@code EventDrivenConsumerBuilder} instance that can be used to
     *         configure and build {@code EventDrivenConsumer} objects.
     */
    public static EventDrivenConsumerBuilder eventDrivenConsumerBuilder()
    {
        return new EventDrivenConsumerBuilder();
    }

    /**
     * Builder class
     */
    public static class EventDrivenConsumerBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;
        private Node source;

        /**
         * Sets the identifier for the event-driven consumer being built.
         *
         * @param id the {@link DesignerItemIdentifier} to associate with the event-driven consumer
         * @return the current instance of {@link EventDrivenConsumerBuilder} for method chaining
         */
        public EventDrivenConsumerBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the EventDrivenConsumer being built.
         *
         * @param name the name to assign to the consumer
         * @return the current instance of EventDrivenConsumerBuilder for method chaining
         */
        public EventDrivenConsumerBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the builder.
         *
         * @param transitionLabel the label to associate with the transition
         * @return the current instance of EventDrivenConsumerBuilder
         */
        public EventDrivenConsumerBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Assigns the specified transition {@code Node} to this builder and returns the builder instance.
         *
         * @param transition the {@code Node} representing the transition to be associated with the consumer
         * @return the {@code EventDrivenConsumerBuilder} instance for method chaining
         */
        public EventDrivenConsumerBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }

        /**
         * Specifies the source node for the consumer being built.
         *
         * @param source the source node to associate with the consumer
         * @return the current instance of {@code EventDrivenConsumerBuilder} for method chaining
         */
        public EventDrivenConsumerBuilder withSource(Node source)
        {
            this.source = source;
            return this;
        }

        /**
         * Builds and returns a new instance of {@code EventDrivenConsumer} using the properties
         * specified in the {@code EventDrivenConsumerBuilder}. The {@code id}, {@code name}, and
         * {@code transition} properties are required and must not be null.
         *
         * @return a new {@code EventDrivenConsumer} instance initialized with the provided properties.
         * @throws IllegalStateException if any of the required properties ({@code id}, {@code name},
         *         or {@code transition}) are null.
         */
        public EventDrivenConsumer build()
        {
            if (id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create EventDrivenConsumer. id, name and transition cannot be null!");
            }

            return new EventDrivenConsumer(id, name, transitionLabel, transition, source);
        }
    }
}
