package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.dashboard.ui.visualisation.scheduler.dag.component.DagNode;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ContextTemplateToDagConverter {
    public List<DagNode> convert(ContextInstance context) {
        List<DagNode> dagNodes = new ArrayList<>();
        AtomicReference<Context> previous = new AtomicReference<>();

        context.getContexts().forEach(child -> {
            DagNode node = this.convert(child, previous.get(), context);
            if(child.getContexts() != null && !child.getContexts().isEmpty()) {
                this.addNodes(node, child, child.getContexts());
            }
            previous.set(child);
            dagNodes.add(node);
        });

        return dagNodes;
    }

    /**
     * Recursively adds child nodes to a DAG node.
     *
     * @param dagNode The DAG node to add child nodes to
     * @param parent The parent context of the child nodes
     * @param children The list of child contexts
     */
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

    /**
     * Converts a given context to a DagNode object.
     *
     * @param context the context to convert
     * @param previous the previous context
     * @param parent the parent context
     * @return the converted DagNode object
     */
    private DagNode convert(Context context, Context previous, Context parent) {
        DagNode dagNode = new DagNode();
        dagNode.setId(context.getName());
        dagNode.setCollapse(true);
        if(previous != null && parent != null) {
            List<Context> transitions = ContextHelper.transitionsFromContext(context, parent.getContexts());
            List<String> transitionNames = transitions.stream()
                .filter(c -> !c.getName().equals(dagNode.getId()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toList());
            String[] dependencies = new String[transitionNames.size()];
            dagNode.setDependencies(transitionNames.toArray(dependencies));
        }
        if(parent != null && !parent.getName().equals(context.getName()))dagNode.setParentId(parent.getName());

        return dagNode;
    }
}
