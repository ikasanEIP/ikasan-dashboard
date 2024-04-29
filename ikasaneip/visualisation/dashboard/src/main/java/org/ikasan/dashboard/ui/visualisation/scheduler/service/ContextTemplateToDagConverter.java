package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.dashboard.ui.visualisation.scheduler.dag.component.DagNode;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ContextTemplateToDagConverter {
    public List<DagNode> convert(ContextInstance context) {
        List<DagNode> dagNodes = new ArrayList<>();
        AtomicReference<Context> previous = new AtomicReference<>();

        context.getContexts().forEach(child -> {
            DagNode node = this.convert(child, previous.get(), null);
            if(child.getContexts() != null && !child.getContexts().isEmpty()) {
                this.addNodes(node, child, child.getContexts());
            }
            previous.set(child);
            dagNodes.add(node);
        });

        return dagNodes;
    }

    private void addNodes(DagNode dagNode, Context parent, List<ContextInstance> children) {
        List<DagNode> childNodes = new ArrayList<>();
        AtomicReference<Context> previous = new AtomicReference<>();
        children.forEach(contextInstance -> {
            DagNode node = this.convert(contextInstance, previous.get(), parent);
            childNodes.add(node);
            if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
                this.addNodes(node, contextInstance, contextInstance.getContexts());
            }
            previous.set(contextInstance);
        });
        DagNode[] nodes = new DagNode[childNodes.size()];
        dagNode.setChildren(childNodes.toArray(nodes));
    }

    private DagNode convert(Context context, Context previous, Context parent) {
        DagNode dagNode = new DagNode();
        dagNode.setId(context.getName());
        dagNode.setCollapse(true);
        if(previous != null) {
            String[] dependencies = new String[1];
            dagNode.setDependencies(List.of(previous.getName()).toArray(dependencies));
        }
        if(parent != null)dagNode.setParentId(parent.getName());

        return dagNode;
    }
}
