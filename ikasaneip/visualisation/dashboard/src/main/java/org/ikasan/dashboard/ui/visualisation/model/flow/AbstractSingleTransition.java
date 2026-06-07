package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents an abstract implementation of a component with a single transition in a designer model.
 * This class provides a foundation for defining nodes that have a single directional relationship
 * or transition to another node, along with an associated label.
 *
 * Classes extending this class should implement specific behavior and characteristics for
 * the transition in the context of their usage.
 *
 * This class extends the behavior of the {@link AbstractWiretapNode} and implements the
 * {@link SingleTransition} interface.
 */
public class AbstractSingleTransition extends AbstractWiretapNode implements SingleTransition
{
    protected Node transition;
    protected String transitionLabel;


    /**
     * Constructs an instance of the AbstractSingleTransition class, representing a transition
     * within a designer model.
     *
     * @param id the unique identifier of the transition, provided as a DesignerItemIdentifier object
     * @param name the name of the transition
     * @param transition the Node object representing the transition
     * @param label the label associated with the transition
     * @param image the image representing the transition
     */
    public AbstractSingleTransition(DesignerItemIdentifier id, String name, Node transition, String label, String image)
    {
        super(id, name, image);
        this.transition = transition;
        this.transitionLabel = label;
    }

    @Override
    public Node getTransition()
    {
        return this.transition;
    }

    @Override
    public String getTransitionLabel()
    {
        return this.transitionLabel;
    }
}
