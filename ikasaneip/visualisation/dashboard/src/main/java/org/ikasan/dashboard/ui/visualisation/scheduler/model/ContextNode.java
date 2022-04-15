package org.ikasan.dashboard.ui.visualisation.scheduler.model;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.stream.IntStream;

public class ContextNode {
    private ContextInstance contextInstance;
    private int spacing = 20;

    public ContextNode(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
    }

    public ContextNode(ContextInstance contextInstance, int spacing) {
        this.contextInstance = contextInstance;
        this.spacing = spacing;
    }

    public ContextInstance getContextInstance() {
        return contextInstance;
    }

    @Override
    public String toString() {
        StringBuffer spacingString = new StringBuffer();
        IntStream.range(0, spacing).forEach(i -> spacingString.append("*"));

        return spacingString.toString();
    }
}
