package org.ikasan.dashboard.ui.visualisation.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.ui.visualisation.model.flow.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.Draw2dAdapterBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract class used to assist in the layout of ikasan flow and module diagrams.
 */
public abstract class LayoutManagerBase extends Draw2dAdapterBase
{
    private Logger logger = LoggerFactory.getLogger(LayoutManagerBase.class);
    protected ObjectMapper mapper = new ObjectMapper();
    protected List<Node> nodeList;
    protected List<Destination> destinations;
    protected Logo logo;

    // The below set of values are used internally
    // in order to calculate the placement of the
    // various components.
    protected int xExtent = 0;
    protected int yExtent = 0;
    protected int xExtentFinal = 0;
    protected int xStart = 300;
    protected int yStart = 300;

    // These values are constant and used to determine the relative location
    // of the various components. Defaults are provided however these can
    // overwritten by setter methods.
    protected int xSpacing = 300;
    protected int ySpacing = 200;


    /**
     * Default constructor for the LayoutManagerBase class.
     * Initializes the nodeList and destinations as empty ArrayLists.
     */
    public LayoutManagerBase()
    {
        this.nodeList = new ArrayList<>();
        this.destinations = new ArrayList<>();
    }


    /**
     * Manages the transition layout by positioning nodes and handling their relationships.
     * Updates the coordinates and maintains layout configuration for various types of nodes.
     *
     * @param transition the current node to be positioned and processed, represented as a {@code Node}
     * @param x the x-coordinate where the node is to be positioned, an integer value
     * @param y the y-coordinate where the node is to be positioned, an integer value
     */
    protected void manageTransition(Node transition, int x, int y)
    {
        nodeList.add(transition);
        logger.debug("Adding component [{}] at [x={}] and [y={}]. ", transition.getId(), x, y);


        if (transition instanceof SingleTransition && ((SingleTransition) transition).getTransition() != null)
        {
            transition.setX(x + xSpacing);
            transition.setY(y);

            manageTransition(((SingleTransition) transition).getTransition(), x + xSpacing, y);
        }
        else if (transition instanceof MultiTransition)
        {

            transition.setX(x + xSpacing);
            transition.setY(y);

            int i=0;

            for (String key: ((MultiTransition) transition).getTransitions().keySet())
            {
                if(key.equals(((MultiTransition) transition).getTransitions().get(key)))
                {
                    key = "";
                }

                if(i > 0 && yExtent >= y)
                {
                    y = yExtent + ySpacing;
                }

                manageTransition(((MultiTransition) transition).getTransitions().get(key), x + xSpacing, y);

                i++;
            }
        }
        else if(transition instanceof Node)
        {
            transition.setX(x + xSpacing);
            transition.setY(y);

            if(transition instanceof Destination)
            {
                this.destinations.add((Destination)transition);
            }
        }

        if(x > xExtent)
        {
            xExtent = x;
        }

        if(x > xExtentFinal)
        {
            xExtentFinal = x;
        }

        if(y > yExtent)
        {
            yExtent = y;
        }
    }

    /**
     * Manages the edges between nodes by recursively processing transitions and updating the edge data.
     * Depending on the type of the given transition, this method adds edges to the layout
     * and processes their respective relationships.
     *
     * @param transition the current node to process and determine its edges, represented as a {@code Node}
     */
    protected void manageEdges(Node transition)
    {
        if (transition instanceof SingleTransition && ((SingleTransition) transition).getTransition() != null)
        {
            addEdge(transition.getId().getUuid(), ((SingleTransition) transition).getTransition().getId().getUuid(), ((SingleTransition) transition).getTransitionLabel(),
                ((SingleTransition) transition).getTransition().getX(), ((SingleTransition) transition).getTransition().getY());

            manageEdges(((SingleTransition) transition).getTransition());
        }
        else if (transition instanceof MultiTransition)
        {
            int i=0;

            for (String key: ((MultiTransition) transition).getTransitions().keySet())
            {
                if(key.equals(((MultiTransition) transition).getTransitions().get(key)))
                {
                    key = "";
                }

                addEdge(transition.getId().getUuid(), ((MultiTransition) transition).getTransitions().get(key).getId().getUuid(), key,
                    ((MultiTransition) transition).getTransitions().get(key).getX(), ((MultiTransition) transition).getTransitions().get(key).getY());

                manageEdges(((MultiTransition) transition).getTransitions().get(key));

                i++;
            }
        }
        else if(transition instanceof Node)
        {
            if(transition instanceof Destination)
            {
                this.destinations.add((Destination)transition);
            }
        }
    }

    /**
     * Adds a connecting edge between two nodes identified by their unique IDs.
     * The edge can include an optional label with specified coordinates.
     *
     * @param fromId the unique identifier of the source node from which the edge originates
     * @param toId the unique identifier of the target node to which the edge connects
     * @param label the text label for the edge, providing additional context or information
     * @param labelX the x-coordinate for positioning the label associated with the edge
     * @param labelY the y-coordinate for positioning the label associated with the edge
     */
    protected void addEdge(String fromId, String toId, String label, double labelX, double labelY)
    {
        this.addConnectionWithLabel(fromId, CONNECTOR_RIGHT_HYBRID_SOURCE, toId
            , CONNECTOR_LEFT_HYBRID_TARGET, diagramBuilder, label, labelX, labelY);

    }
}
