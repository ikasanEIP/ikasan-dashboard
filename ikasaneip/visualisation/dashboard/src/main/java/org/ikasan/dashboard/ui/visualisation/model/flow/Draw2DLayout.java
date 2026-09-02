package org.ikasan.dashboard.ui.visualisation.model.flow;

import java.util.List;

/**
 * Represents a layout configuration for a Draw2D diagram, containing flow components
 * and JSON configuration data.
 */
public class Draw2DLayout {
    private List<Node> flowComponents;
    private String draw2dJson;

    /**
     * Constructs a Draw2DLayout instance with the specified flow components and draw2d JSON configuration.
     *
     * @param flowComponents a list of AbstractWiretapNode objects representing the flow components in the layout
     * @param draw2dJson a string containing the JSON configuration for the Draw2D layout
     */
    public Draw2DLayout(List<Node> flowComponents, String draw2dJson) {
        this.flowComponents = flowComponents;
        this.draw2dJson = draw2dJson;
    }

    /**
     * Retrieves the list of flow components associated with this layout.
     *
     * @return a list of AbstractWiretapNode objects representing the flow components
     */
    public List<Node> getFlowComponents() {
        return flowComponents;
    }

    /**
     * Retrieves the Draw2D JSON configuration associated with this layout.
     *
     * @return a string containing the Draw2D JSON configuration.
     */
    public String getDraw2dJson() {
        return draw2dJson;
    }

    /**
     * Retrieves a flow component with the specified name from the list of flow components.
     *
     * @param name the name of the flow component to retrieve
     * @return the AbstractWiretapNode corresponding to the specified name
     * @throws IllegalArgumentException if no flow component with the specified name is found
     */
    public Node getFlowComponent(String name) {
        return this.flowComponents.stream()
            .filter(abstractWiretapNode -> abstractWiretapNode.getName() != null
                && abstractWiretapNode.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Flowimpl component not found: " + name));
    }
}
