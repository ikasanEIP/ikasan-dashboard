package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a filter component in the designer model with a single transition.
 * This class extends the behavior of the {@code AbstractSingleTransition} class.
 * A filter typically connects to another node with a specified transition label.
 */
public class Filter extends AbstractSingleTransition
{
	public static final String IMAGE = "frontend/images/message-filter.png";


    /**
     * Creates a new instance of the Filter class, representing a filter component
     * in the designer model with a specified transition and label.
     *
     * @param id The identifier of the filter, encapsulating type, name, and UUID information.
     * @param name The name of the filter.
     * @param transitionLabel The descriptive label for the transition of the filter.
     * @param transition The node object representing the transition connected to the filter.
     */
	public Filter(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
    {
        super(id, name, transition, transitionLabel, IMAGE);
    }

    /**
     * Creates and returns a new instance of {@code FilterBuilder}.
     * The {@code FilterBuilder} provides a fluent interface for constructing
     * a {@code Filter} object with specified properties such as ID, name,
     * transition label, and transition node.
     *
     * @return a new {@code FilterBuilder} instance for constructing a {@code Filter}.
     */
    public static FilterBuilder filterBuilder()
    {
        return new FilterBuilder();
    }

    /**
     * Builder class
     */
    public static class FilterBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the filter and returns the updated FilterBuilder instance.
         *
         * @param id the {@link DesignerItemIdentifier} that uniquely identifies the filter.
         * @return the updated instance of {@code FilterBuilder} for method chaining.
         */
        public FilterBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the filter.
         *
         * @param name the name to assign to the filter
         * @return the current instance of {@code FilterBuilder} for method chaining
         */
        public FilterBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the filter being built.
         *
         * @param transitionLabel the label associated with the transition
         * @return the current instance of the FilterBuilder to allow method chaining
         */
        public FilterBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the filter being built.
         *
         * @param transition the {@link Node} object to associate as the transition.
         * @return the current {@link FilterBuilder} instance for method chaining.
         */
        public FilterBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }


        /**
         * Builds and returns a {@code Filter} instance using the current state of the builder.
         * The required fields {@code id}, {@code name}, and {@code transition} must
         * be non-null; otherwise, an {@code IllegalStateException} is thrown.
         *
         * @return a newly created {@code Filter} object initialized with the specified properties.
         * @throws IllegalStateException if any of the required fields ({@code id}, {@code name}, {@code transition}) is null.
         */
        public Filter build()
        {
            if (id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create Filter. id, name and transition cannot be null!");
            }

            return new Filter(id, name, transitionLabel, transition);
        }
    }
}
