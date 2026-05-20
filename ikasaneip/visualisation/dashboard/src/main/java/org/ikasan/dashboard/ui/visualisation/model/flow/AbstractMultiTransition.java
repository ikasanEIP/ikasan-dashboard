package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

import java.util.HashMap;
import java.util.Map;

public class AbstractMultiTransition extends AbstractWiretapNode implements MultiTransition {
    protected Map<String, Node> transitions;

    public AbstractMultiTransition(DesignerItemIdentifier id, String name, String image) {
        super(id, name, image);
        this.transitions = new HashMap<>();
    }

    @Override
    public Map<String, Node> getTransitions() {
        return this.transitions;
    }
}
