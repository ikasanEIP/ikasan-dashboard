package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a specific type of endpoint within a system or workflow that supports a single transition.
 * The MessageEndPoint class acts as a concrete implementation of the {@link Endpoint} interface and
 * extends the {@link AbstractSingleTransition} class.
 *
 * This class models a message-based endpoint within a larger system, complete with properties
 * such as an identifier, name, and associated transition. It also provides a builder class
 * for constructing instances.
 */
public class MessageEndPoint extends AbstractSingleTransition implements Endpoint
{
	public static final String IMAGE = "frontend/images/message-endpoint.png";

    /**
     * Constructs a new {@code MessageEndPoint} instance with the specified parameters.
     *
     * @param id the unique identifier for the designer item that represents this endpoint
     * @param name the name of this endpoint
     * @param transitionLabel the label associated with the transition for this endpoint
     * @param transition the node representing the transition connected to this endpoint
     */
	public MessageEndPoint(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
    {
        super(id, name, transition, transitionLabel, IMAGE);
    }

    /**
     * Creates and returns a new instance of {@code MessageEndPointBuilder}.
     * This builder can be used to configure and construct instances of {@code MessageEndPoint}.
     *
     * @return a new {@code MessageEndPointBuilder} instance for constructing {@code MessageEndPoint} objects.
     */
    public static MessageEndPointBuilder messageEndPointBuilder()
    {
        return new MessageEndPointBuilder();
    }

    /**
     * Builder class
     */
    public static class MessageEndPointBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the message endpoint being built.
         *
         * @param id the {@link DesignerItemIdentifier} representing the unique identifier of the message endpoint
         * @return the current instance of {@code MessageEndPointBuilder} for method chaining
         */
        public MessageEndPointBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name of the MessageEndPoint being built.
         *
         * @param name the name to set for the MessageEndPoint
         * @return the current instance of MessageEndPointBuilder
         */
        public MessageEndPointBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for this MessageEndPointBuilder and returns the builder instance.
         *
         * @param transitionLabel the label to associate with the transition
         * @return the updated MessageEndPointBuilder instance
         */
        public MessageEndPointBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the message endpoint being built.
         *
         * @param transition the {@code Node} representing the transition to be set
         * @return the current instance of {@code MessageEndPointBuilder} for method chaining
         */
        public MessageEndPointBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }


        /**
         * Constructs and returns a new {@code MessageEndPoint} instance based on the current state
         * of the {@code MessageEndPointBuilder}. The builder must have non-null values for the
         * mandatory properties {@code id} and {@code name}, or an {@code IllegalStateException}
         * will be thrown.
         *
         * @return a new {@code MessageEndPoint} instance initialized with the properties set in the builder
         * @throws IllegalStateException if either {@code id} or {@code name} is {@code null}
         */
        public MessageEndPoint build()
        {
            if (id == null || name == null)
            {
                throw new IllegalStateException("Cannot create DeadEndPoint. id and name cannot be null!");
            }

            return new MessageEndPoint(id, name, transitionLabel, transition);
        }
    }
}
