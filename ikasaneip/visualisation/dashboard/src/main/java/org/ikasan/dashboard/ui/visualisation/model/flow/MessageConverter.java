package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a message converter component that performs a single transition within a designer model.
 * This class extends {@code AbstractSingleTransition} to provide additional functionality for
 * handling message conversions in the context of its usage.
 */
public class MessageConverter extends AbstractSingleTransition
{
	public static final String IMAGE = "frontend/images/message-translator.png";

    /**
     * Constructs a new instance of {@code MessageConverter}.
     *
     * @param id the unique identifier for the designer item
     * @param name the name of the message converter
     * @param transitionLabel the label for the transition performed by the message converter
     * @param transition the node representing the transition associated with the message converter
     */
	public MessageConverter(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
    {
        super(id, name, transition, transitionLabel, IMAGE);
    }

    /**
     * Creates a new instance of {@code MessageConverterBuilder}, which facilitates
     * the construction of a {@code MessageConverter} object.
     *
     * @return a new {@code MessageConverterBuilder} instance for building {@code MessageConverter} objects
     */
    public static MessageConverterBuilder messageConverterBuilder()
    {
        return new MessageConverterBuilder();
    }

    /**
     * Builder class
     */
    public static class MessageConverterBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the message converter being built.
         *
         * @param id the {@link DesignerItemIdentifier} to associate with the message converter
         * @return the current instance of {@code MessageConverterBuilder}, allowing method chaining
         */
        public MessageConverterBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the MessageConverter being built.
         *
         * @param name the name to set for the MessageConverter
         * @return the current instance of MessageConverterBuilder for method chaining
         */
        public MessageConverterBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the message converter being built.
         *
         * @param transitionLabel the transition label to set
         * @return the current instance of {@code MessageConverterBuilder} for method chaining
         */
        public MessageConverterBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the message converter being built.
         *
         * @param transition the transition node to be used, represented as an instance of {@code Node}
         * @return the current instance of {@code MessageConverterBuilder} to allow method chaining
         */
        public MessageConverterBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }


        /**
         * Builds and returns a new {@code MessageConverter} instance using the current state of the builder.
         * The {@code id}, {@code name}, and {@code transition} properties must not be null;
         * otherwise, an {@code IllegalStateException} is thrown.
         *
         * @return a new {@code MessageConverter} instance constructed with the specified properties
         * @throws IllegalStateException if the {@code id}, {@code name}, or {@code transition} is null
         */
        public MessageConverter build()
        {
            if (id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create MessageConverter. id, name and transition cannot be null!");
            }

            return new MessageConverter(id, name, transitionLabel, transition);
        }
    }
}
