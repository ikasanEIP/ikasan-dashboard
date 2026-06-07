package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a specialized type of {@link Consumer} which uses polling mechanisms
 * in a process flow. It extends the general behavior of a {@code Consumer} and
 * provides a builder pattern for flexible and customizable construction.
 */
public class PollingConsumer extends Consumer
{
	public static final String IMAGE = "frontend/images/polling-consumer.png";

    /**
     * Constructs a new instance of PollingConsumer.
     *
     * @param id the unique identifier of the polling consumer.
     * @param name the name of the polling consumer.
     * @param transitionLabel a label associated with the transition.
     * @param transition the node representing the transition.
     * @param source the source node of the polling consumer.
     */
	private PollingConsumer(DesignerItemIdentifier id, String name, String transitionLabel, Node transition, Node source)
    {
        super(id, name, transitionLabel, transition, IMAGE, source);
    }

    /**
     * Creates a new instance of the PollingConsumerBuilder, which is used to construct
     * a {@link PollingConsumer} object with a customizable configuration.
     *
     * @return a new instance of {@code PollingConsumerBuilder} for configuring and creating a {@code PollingConsumer}.
     */
    public static PollingConsumerBuilder pollingConsumerBuilder()
    {
        return new PollingConsumerBuilder();
    }

    /**
     * Builder class
     */
    public static class PollingConsumerBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;
        private Node source;

        /**
         * Sets the identifier for the polling consumer being built.
         *
         * @param id the {@link DesignerItemIdentifier} representing the identifier
         * @return the current instance of {@link PollingConsumerBuilder} for method chaining
         */
        public PollingConsumerBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the PollingConsumer being built and returns the builder instance.
         *
         * @param name the name to set for the PollingConsumer
         * @return the current instance of PollingConsumerBuilder
         */
        public PollingConsumerBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the PollingConsumer.
         *
         * @param transitionLabel the label representing the transition to be set
         * @return the instance of PollingConsumerBuilder for method chaining
         */
        public PollingConsumerBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the polling consumer being built.
         *
         * @param transition the {@code Node} representing the transition to be associated with this polling consumer
         * @return the current {@code PollingConsumerBuilder} instance for method chaining
         */
        public PollingConsumerBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }

        /**
         * Sets the source node for the polling consumer being built.
         *
         * @param source the source node to associate with the polling consumer
         * @return the updated instance of the {@code PollingConsumerBuilder} for method chaining
         */
        public PollingConsumerBuilder withSource(Node source)
        {
            this.source = source;
            return this;
        }

        /**
         * Builds a {@code PollingConsumer} instance using the data provided
         * through the {@code PollingConsumerBuilder}.
         *
         * @return a new instance of {@code PollingConsumer} configured with
         * the builder's current state.
         * @throws IllegalStateException if any of the required parameters
         * {@code id}, {@code name}, or {@code transition} are {@code null}.
         */
        public PollingConsumer build()
        {
            if(id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create PollingConsumer. id, name and transition cannot be null!");
            }

            return new PollingConsumer(id, name, transitionLabel, transition, source);
        }
    }
}
