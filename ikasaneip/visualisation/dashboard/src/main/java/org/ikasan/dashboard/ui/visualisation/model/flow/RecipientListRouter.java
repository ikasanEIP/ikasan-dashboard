package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by stewmi on 07/11/2018.
 */
public class RecipientListRouter extends AbstractMultiTransition
{
	public static final String IMAGE = "frontend/images/recipient-list-router.png";

	public RecipientListRouter(DesignerItemIdentifier id, String name)
	{
        super(id, name, IMAGE);
		this.transitions = new HashMap<>();
	}

	public void addTransition(String context, Node node)
	{
		this.transitions.put(context, node);
	}

	@Override
	public Map<String, Node> getTransitions()
	{
		return this.transitions;
	}

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

        public RecipientListRouterBuilder withId(DesignerItemIdentifier id)
        {
            this.id = id;
            return this;
        }

        public RecipientListRouterBuilder withName(String name)
        {
            this.name = name;
            return this;
        }


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
