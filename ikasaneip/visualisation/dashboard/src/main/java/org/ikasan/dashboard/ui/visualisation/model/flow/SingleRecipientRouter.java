package org.ikasan.dashboard.ui.visualisation.model.flow;

import org.ikasan.designer.pallet.DesignerItemIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a router node in a workflow that supports routing to a single recipient
 * based on a specified transition context. It extends {@link AbstractMultiTransition}
 * and provides the functionality to add and manage transitions to different nodes.
 */
public class SingleRecipientRouter extends AbstractMultiTransition
{
	private static final String IMAGE = "frontend/images/message-router.png";

	/**
     * Constructs a new instance of {@code SingleRecipientRouter}.
     * This router allows for defining transitions to a single recipient node based on a given context.
     * It is initialized with a unique identifier, a name, and a predefined image constant.
     *
     * @param id   the unique identifier for the router represented as a {@link DesignerItemIdentifier}
     * @param name the name of the router
     */
    public SingleRecipientRouter(DesignerItemIdentifier id, String name)
	{
        super(id, name, IMAGE);
		transitions = new HashMap<>();
	}

	/**
     * Adds a transition to the specified node based on the given transition context.
     *
     * @param transitionContext the context or key representing the transition
     *                          that determines the routing logic
     * @param node the destination {@code Node} to which the transition is mapped
     */
    public void addTransition(String transitionContext, Node node)
	{
		this.transitions.put(transitionContext, node);
	}

	@Override
	public Map<String, Node> getTransitions()
	{
		return this.transitions;
	}


    /**
     * Creates and returns a new instance of {@code SingleRecipientRouterBuilder}.
     * The builder allows for constructing a {@code SingleRecipientRouter} instance
     * by setting required properties such as identifier and name.
     *
     * @return a new instance of {@code SingleRecipientRouterBuilder} to facilitate the construction
     *         of {@code SingleRecipientRouter} objects.
     */
    public static SingleRecipientRouterBuilder singleRecipientRouterBuilder()
    {
        return new SingleRecipientRouterBuilder();
    }

    /**
     * Builder class
     */
    public static class SingleRecipientRouterBuilder
    {
        private DesignerItemIdentifier id;
        private String name;

        /**
         * Sets the identifier for the SingleRecipientRouter and returns the builder instance.
         *
         * @param id the identifier to be set for the SingleRecipientRouter
         * @return the current instance of {@code SingleRecipientRouterBuilder}
         */
        public SingleRecipientRouterBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the SingleRecipientRouter being built.
         *
         * @param name the name to be assigned to the router
         * @return the current instance of {@code SingleRecipientRouterBuilder} for method chaining
         */
        public SingleRecipientRouterBuilder withName(String name)
        {
            this.name = name;
            return this;
        }


        /**
         * Builds and returns an instance of {@code SingleRecipientRouter} using the current state
         * of the builder. The method ensures that all required fields are appropriately set
         * before creating the instance.
         *
         * @return a new instance of {@code SingleRecipientRouter} initialized with the builder's state
         * @throws IllegalStateException if either the {@code id} or {@code name} fields are null
         */
        public SingleRecipientRouter build()
        {
            if (id == null || name == null)
            {
                throw new IllegalStateException("Cannot create SingleRecipientRouter. id and name cannot ne null!");
            }

            return new SingleRecipientRouter(id, name);
        }
    }
}
