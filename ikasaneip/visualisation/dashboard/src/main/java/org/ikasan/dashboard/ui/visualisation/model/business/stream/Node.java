package org.ikasan.dashboard.ui.visualisation.model.business.stream;

import org.ikasan.designer.pallet.DesignerItemIdentifier;

public class Node
{
    private DesignerItemIdentifier id;
    private String edgeColour = "rgba(0, 255, 0, 0.8)";
    private String fillColour = "rgba(0, 255, 0, 0.2)";
    private long wiretapFoundCount = 0L;
    private long errorFoundCount = 0L;
    private long exclusionFoundCount = 0L;
    private long replayFoundCount = 0L;

    private int x;
    private int y;


    public Node(DesignerItemIdentifier id, int x, int y)
    {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    /**
     * Retrieves the identifier associated with this node.
     *
     * @return the identifier of the node as a {@code DesignerItemIdentifier}.
     */
    public DesignerItemIdentifier getId()
    {
        return id;
    }

    /**
     * Sets the unique identifier for this node.
     *
     * @param id the {@code DesignerItemIdentifier} to assign to the node.
     *           This identifier uniquely defines the associated designer item
     *           through its type, name, and UUID properties.
     */
    public void setId(DesignerItemIdentifier id)
    {
        this.id = id;
    }

    /**
     * Retrieves the color of the edge associated with the node.
     *
     * @return the edge color as a string in RGBA format.
     */
    public String getEdgeColour()
    {
        return edgeColour;
    }

    /**
     * Sets the color of the edge for this node.
     *
     * @param edgeColour the new edge color as a String, typically in a format such as
     *                   "rgba(r, g, b, a)" where r, g, b, and a represent the red, green, blue,
     *                   and alpha (transparency) components.
     */
    public void setEdgeColour(String edgeColour)
    {
        this.edgeColour = edgeColour;
    }

    /**
     * Retrieves the fill color of the node.
     *
     * @return the fill color as a string representation, typically in RGBA format.
     */
    public String getFillColour()
    {
        return fillColour;
    }

    /**
     * Sets the fill color for the node. This color is used to define
     * the visual representation of the node's interior.
     *
     * @param fillColour the new fill color, represented as a String.
     *                   It is expected to be in a valid color format
     *                   such as a color name, hexadecimal, or rgba value.
     */
    public void setFillColour(String fillColour)
    {
        this.fillColour = fillColour;
    }

    /**
     * Retrieves the count of wiretaps found.
     *
     * @return the number of wiretaps found as a long.
     */
    public long getWiretapFoundCount()
    {
        return wiretapFoundCount;
    }

    /**
     * Sets the count of wiretap events found for this node.
     *
     * @param wiretapFoundCount the number of wiretap events to set
     */
    public void setWiretapFoundCount(long wiretapFoundCount)
    {
        this.wiretapFoundCount = wiretapFoundCount;
    }

    /**
     * Retrieves the count of errors found within the node.
     *
     * @return the number of errors detected as a long value.
     */
    public long getErrorFoundCount() {
        return errorFoundCount;
    }

    /**
     * Sets the count of errors found for this node.
     *
     * @param errorFoundCount the number of errors to be set
     */
    public void setErrorFoundCount(long errorFoundCount) {
        this.errorFoundCount = errorFoundCount;
    }

    /**
     * Returns the count of exclusions found for this node.
     *
     * @return the number of exclusions found as a long value.
     */
    public long getExclusionFoundCount() {
        return exclusionFoundCount;
    }

    /**
     * Sets the count of exclusions found for the given Node instance.
     *
     * @param exclusionFoundCount the number of exclusions detected, represented as a long value
     */
    public void setExclusionFoundCount(long exclusionFoundCount) {
        this.exclusionFoundCount = exclusionFoundCount;
    }

    /**
     * Retrieves the count of replay events found for this node.
     *
     * @return the count of replay events found as a long value
     */
    public long getReplayFoundCount() {
        return replayFoundCount;
    }

    /**
     * Updates the count of replays found for the associated node.
     *
     * @param replayFoundCount the number of replays found to set
     */
    public void setReplayFoundCount(long replayFoundCount) {
        this.replayFoundCount = replayFoundCount;
    }

    /**
     * Retrieves the x-coordinate of the current node.
     *
     * @return the x-coordinate represented as an integer
     */
    public int getX() {
        return x;
    }

    /**
     * Retrieves the y-coordinate value of the node.
     *
     * @return the y-coordinate of the node as an integer.
     */
    public int getY() {
        return y;
    }
}
