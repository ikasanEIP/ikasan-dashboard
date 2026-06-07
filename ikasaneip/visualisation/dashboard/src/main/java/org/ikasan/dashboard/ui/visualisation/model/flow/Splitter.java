package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a splitter within a designer model, defining a single transition to another node
 * along with an associated label. The splitter extends the {@link AbstractSingleTransition} class
 * and sets a predefined image to represent its visual component.
 */
public class Splitter extends AbstractSingleTransition
{
	public static final String IMAGE = "frontend/images/splitter.png";

    /**
     * Constructs a new instance of the Splitter, representing a designer model object
     * with a single transition to another node. The transition is accompanied by a
     * descriptive label, and the visual representation of the splitter is defined by
     * a predefined image.
     *
     * @param id               A unique identifier for the splitter item. It is an instance
     *                         of {@link DesignerItemIdentifier} containing the type, name,
     *                         and UUID.
     * @param name             The name of the splitter, used to represent it in the designer model.
     * @param transitionLabel  The label associated with the transition to another node.
     * @param transition       The target node to which the splitter transitions. It is an
     *                         instance of {@link Node}.
     */
	public Splitter(DesignerItemIdentifier id, String name, String transitionLabel, Node transition)
    {
        super(id, name, transition, transitionLabel, IMAGE);
    }

    /**
     * Creates a new instance of {@code SplitterBuilder}, allowing the construction of a {@code Splitter} object
     * through a fluent API.
     *
     * @return a new instance of {@code SplitterBuilder} to define and build a {@code Splitter}.
     */
    public static SplitterBuilder splitterBuilder()
    {
        return new SplitterBuilder();
    }

    /**
     * Builder class
     */
    public static class SplitterBuilder
    {
        private DesignerItemIdentifier id;
        private String name;
        private String transitionLabel;
        private Node transition;

        /**
         * Sets the identifier for the splitter being built.
         *
         * @param id the identifier of the splitter, represented as a {@link DesignerItemIdentifier}
         * @return the current instance of {@code SplitterBuilder} for method chaining
         */
        public SplitterBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the splitter being built.
         *
         * @param name the name to assign to the splitter
         * @return the current instance of the {@code SplitterBuilder} for method chaining
         */
        public SplitterBuilder withName(String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Sets the transition label for the SplitterBuilder.
         *
         * @param transitionLabel the transition label to set
         * @return the current instance of SplitterBuilder
         */
        public SplitterBuilder withTransitionLabel(String transitionLabel)
        {
            this.transitionLabel = transitionLabel;
            return this;
        }

        /**
         * Sets the transition node for the SplitterBuilder and returns the builder instance.
         *
         * @param transition the transition node to be associated with the builder.
         *                   It represents the Node object being set as the transition point.
         * @return the current instance of {@code SplitterBuilder} for method chaining.
         */
        public SplitterBuilder withTransition(Node transition)
        {
            this.transition = transition;
            return this;
        }


        /**
         * Builds and returns a {@code Splitter} instance with the provided parameters.
         * The {@code id}, {@code name}, and {@code transition} must not be null; otherwise,
         * an {@code IllegalStateException} will be thrown. This ensures that the resulting
         * {@code Splitter} is fully initialized and valid.
         *
         * @return a new instance of {@code Splitter}, constructed with the specified
         *         {@code id}, {@code name}, {@code transitionLabel}, and {@code transition}.
         * @throws IllegalStateException if {@code id}, {@code name}, or {@code transition} is null.
         */
        public Splitter build()
        {
            if (id == null || name == null || transition == null)
            {
                throw new IllegalStateException("Cannot create Splitter. id, name and transition cannot be null!");
            }

            return new Splitter(id, name, transitionLabel, transition);
        }
    }
}
