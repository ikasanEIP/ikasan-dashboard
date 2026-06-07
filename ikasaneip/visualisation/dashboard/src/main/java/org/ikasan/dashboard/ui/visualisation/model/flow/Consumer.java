package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;


/**
 * Represents an abstract Consumer entity that defines a single transition in a process flow.
 * This class extends the AbstractSingleTransition and implements the SingleTransition interface,
 * providing a foundation for various specific types of consumer classes.
 */
public abstract class Consumer extends AbstractSingleTransition implements SingleTransition
{
    private Node source;

    /**
     * Constructs a new Consumer instance with the specified identifier, name, transition details, image,
     * and source node. Inherits behavior from the AbstractSingleTransition class.
     *
     * @param id               the unique identifier for the Consumer.
     * @param name             the name of the Consumer.
     * @param transitionLabel  the label for the transition associated with the Consumer.
     * @param transition       the target transition node for the Consumer.
     * @param image            the image representation of the Consumer.
     * @param source           the source node associated with this Consumer.
     */
	public Consumer(DesignerItemIdentifier id, String name, String transitionLabel, Node transition, String image, Node source)
    {
        super(id, name, transition, transitionLabel, image);

        this.source = source;
    }

    /**
     * Retrieves the source node associated with this consumer.
     *
     * @return the source node as an instance of {@code Node}.
     */
    public Node getSource()
    {
        return source;
    }
}
