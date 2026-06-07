package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a MessageTranslator component in a designer model. This class provides a
 * specialization of AbstractSingleTransition for handling transitions that involve
 * message translation. Each MessageTranslator instance defines an ID, name, transition label,
 * and a transition node, along with its associated image.
 */
public class MessageTranslator extends AbstractSingleTransition
{
	public static final String IMAGE = "frontend/images/message-translator.png";


    /**
     * Constructs a new instance of MessageTranslator, representing a component that facilitates
     * message translation in a designer model. This constructor initializes the MessageTranslator
     * with the given identifier, name, transition label, and transition node, while associating
     * it with a predefined image.
     *
     * @param id the unique identifier of the MessageTranslator; defines its type, name, and UUID
     * @param name the name of the MessageTranslator instance
     * @param transitionLabel the label for the transition associated with the MessageTranslator
     * @param transition the transition node that this MessageTranslator facilitates
     */
	public MessageTranslator(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
    {
        super(id, name, transition, transitionLabel, IMAGE);
    }

    /**
     * Provides a builder instance for creating and configuring a {@code MessageTranslator}.
     * The builder allows setting various properties such as ID, name, transition label,
     * and transition node before constructing the {@code MessageTranslator} instance.
     *
     * @return a new instance of {@code MessageTranslatorBuilder} to build a {@code MessageTranslator}.
     */
    public static MessageTranslatorBuilder messageConverterBuilder()
    {
        return new MessageTranslatorBuilder();
    }

    /**
     * Builder class
     */
    public static class MessageTranslatorBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the message translator being built.
         *
         * @param id the identifier of type {@code DesignerItemIdentifier} to associate with the message translator
         * @return the current instance of {@code MessageTranslatorBuilder} for method chaining
         */
        public MessageTranslatorBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the MessageTranslator being built. This method is part of the
         * builder pattern used to configure and construct a MessageTranslator instance.
         *
         * @param name the name to assign to the MessageTranslator
         * @return the current instance of MessageTranslatorBuilder for method chaining
         */
        public MessageTranslatorBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the message translator being built.
         *
         * @param transitionLabel the label to associate with the transition
         * @return the current instance of {@code MessageTranslatorBuilder} for method chaining
         */
        public MessageTranslatorBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the message translator being built.
         *
         * @param transition the {@code Node} object representing the transition to associate with the message translator
         * @return the current instance of {@code MessageTranslatorBuilder}, allowing for method chaining
         */
        public MessageTranslatorBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }


        /**
         * Builds and returns an instance of {@code MessageTranslator} based on the properties
         * set in the builder. This method validates that all required fields
         * ({@code id}, {@code name}, and {@code transition}) are non-null before creating the instance.
         *
         * @return a fully initialized {@code MessageTranslator} instance
         * @throws IllegalStateException if {@code id}, {@code name}, or {@code transition} is null
         */
        public MessageTranslator build()
        {
            if (id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create MessageConverter. id, name and transition cannot be null!");
            }

            return new MessageTranslator(id, name, transitionLabel, transition);
        }
    }
}
