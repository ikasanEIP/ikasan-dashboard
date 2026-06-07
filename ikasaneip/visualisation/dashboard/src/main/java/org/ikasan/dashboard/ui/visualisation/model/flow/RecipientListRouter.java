package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a routing node in a workflow or routing context which routes
 * messages to multiple recipients based on specified contexts. Each recipient
 * is identified by a context string and a corresponding target node.
 *
 * This class extends {@link AbstractMultiTransition}, allowing management of
 * multiple transitions between workflow nodes.
 *
 * It also provides a builder class {@link RecipientListRouterBuilder} for
 * constructing instances of {@code RecipientListRouter} with a fluent API.
 */
public class RecipientListRouter extends AbstractMultiTransition
{
	public static final String IMAGE = "frontend/images/recipient-list-router.png";

	/**
     * Constructs an instance of {@code RecipientListRouter}.
     *
     * @param id   the unique identifier for the designer item, containing information
     *             about type, name, and UUID
     * @param name the name of the recipient list router
     */
    public RecipientListRouter(DesignerItemIdentifier id, String name)
	{
        super(id, name, IMAGE);
		this.transitions = new HashMap<>();
	}

	/**
     * Adds a transition to the current recipient list router with the given context
     * and target node.
     *
     * @param context the context string used as the key for the transition.
     *                This typically identifies the condition or category for this routing.
     * @param node    the target {@link Node} associated with the specified context.
     *                This node represents the destination the routing points to.
     */
    public void addTransition(String context, Node node)
	{
		this.transitions.put(context, node);
	}

	@Override
	public Map<String, Node> getTransitions()
	{
		return this.transitions;
	}

    /**
     * Creates and returns a new instance of {@link RecipientListRouterBuilder},
     * enabling fluent construction of {@code RecipientListRouter} objects.
     *
     * @return an instance of {@link RecipientListRouterBuilder} to initialize
     *         and configure a {@code RecipientListRouter}.
     */
    public static RecipientListRouterBuilder recipientRouterBuilder()
    {
        return new RecipientListRouterBuilder();
    }

    /**
     * Builder class
     */
    public static class RecipientListRouterBuilder
    {
        private DesignerItemIdentifier id;
        private String name;

        /**
         * Sets the identifier for the RecipientListRouterBuilder.
         *
         * @param id the {@link DesignerItemIdentifier} instance representing the unique identifier for the router.
         * @return the current instance of {@link RecipientListRouterBuilder} with the updated identifier.
         */
        public RecipientListRouterBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the recipient list router being built.
         *
         * @param name the name to be assigned to the recipient list router
         * @return the current instance of {@code RecipientListRouterBuilder} for method chaining
         */
        public RecipientListRouterBuilder withName(String name)
        {
            this.name = name;
            return this;
        }


        /**
         * Builds and returns a new {@link RecipientListRouter} instance based on the
         * provided id and name configured in the builder.
         *
         * @return a constructed {@link RecipientListRouter} instance
         * @throws IllegalStateException if the id or name is null, indicating that the
         *         builder is in an invalid state.
         */
        public RecipientListRouter build()
        {
            if (id == null || name == null)
            {
                throw new IllegalStateException("Cannot create RecipientListRouter. id and name cannot ne null!");
            }

            return new RecipientListRouter(id, name);
        }
    }
}
