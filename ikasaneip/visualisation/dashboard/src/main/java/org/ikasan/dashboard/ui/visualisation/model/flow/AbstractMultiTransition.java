package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an abstract base class for nodes that support multiple transitions
 * in a workflow or routing context. Provides common functionality for managing
 * transitions between different nodes.
 *
 * This class extends {@link AbstractWiretapNode} and implements the
 * {@link MultiTransition} interface. It is designed to be inherited by more
 * specific implementations of multi-transition behavior.
 */
public class AbstractMultiTransition extends AbstractWiretapNode implements MultiTransition {
    protected Map<String, Node> transitions;

    /**
     * Constructs an instance of AbstractMultiTransition.
     *
     * @param id    the unique identifier for the designer item
     * @param name  the name of the designer item
     * @param image the image associated with the designer item
     */
    public AbstractMultiTransition(DesignerItemIdentifier id, String name, String image) {
        super(id, name, image);
        this.transitions = new HashMap<>();
    }

    @Override
    public Map<String, Node> getTransitions() {
        return this.transitions;
    }
}
