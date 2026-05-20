package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

public class AbstractSingleTransition extends AbstractWiretapNode implements SingleTransition
{
    protected Node transition;
    protected String transitionLabel;

    /**
     * Constructor
     *
     * @param id
     * @param name
     * @param transition
     * @param label
     * @param image
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
