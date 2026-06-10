package org.ikasan.dashboard.ui.visualisation.layout;

import org.ikasan.dashboard.ui.visualisation.model.flow.Draw2DLayout;

public interface LayoutManager
{

    /**
     * Provides the layout configuration for managing the arrangement and properties
     * of flow components within a Draw2D diagram.
     *
     * @return an instance of Draw2DLayout representing the layout configuration
     *         of the Draw2D diagram, including flow components and JSON configuration.
     */
    Draw2DLayout layout();
}
