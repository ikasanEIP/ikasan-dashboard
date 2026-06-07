package org.ikasan.dashboard.ui.visualisation.model.flow;

import org.ikasan.designer.pallet.DesignerItemIdentifier;

public class Node
{
    private DesignerItemIdentifier id;
    private String name;
    private String edgeColour = "rgba(0, 255, 0, 0.8)";
    private String fillColour = "rgba(0, 255, 0, 0.2)";
    private long wiretapFoundCount = 0L;
    private long errorFoundCount = 0L;
    private long exclusionFoundCount = 0L;
    private long replayFoundCount = 0L;
    private String image;

    private int x;
    private int y;


    /**
     * Constructs a new Node object with the specified identifier, coordinates, and image.
     *
     * @param id    the unique identifier of the node
     * @param x     the x-coordinate of the node
     * @param y     the y-coordinate of the node
     * @param image the image associated with the node
     */
    public Node(DesignerItemIdentifier id, int x, int y, String image)
    {
        this.id = id;
        this.x = x;
        this.y = y;
        this.image = image;
    }

    /**
     * Retrieves the identifier associated with this node.
     *
     * @return the identifier of type DesignerItemIdentifier representing this node's unique ID.
     */
    public DesignerItemIdentifier getId()
    {
        return id;
    }

    /**
     * Sets the identifier for the node.
     *
     * @param id the identifier to be assigned to the node, represented as a {@code DesignerItemIdentifier}
     */
    public void setId(DesignerItemIdentifier id)
    {
        this.id = id;
    }

    /**
     * Retrieves the name.
     *
     * @return the name as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the node.
     *
     * @param name the name to set for this node
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the color used for the edge representation of this node.
     *
     * @return the edge color as a string.
     */
    public String getEdgeColour()
    {
        return edgeColour;
    }

    /**
     * Sets the colour of the edge.
     *
     * @param edgeColour the new colour to set for the edge, represented as a string.
     */
    public void setEdgeColour(String edgeColour)
    {
        this.edgeColour = edgeColour;
    }

    /**
     * Retrieves the fill color of this node.
     *
     * @return the current fill color as a string in RGBA format.
     */
    public String getFillColour()
    {
        return fillColour;
    }

    /**
     * Sets the fill color of the node.
     *
     * @param fillColour the new fill color to set, represented as a string (e.g., an RGBA color value)
     */
    public void setFillColour(String fillColour)
    {
        this.fillColour = fillColour;
    }

    /**
     * Retrieves the count of wiretap events found.
     *
     * @return the number of wiretap events encountered as a long value.
     */
    public long getWiretapFoundCount()
    {
        return wiretapFoundCount;
    }

    /**
     * Sets the count of wiretaps found.
     *
     * @param wiretapFoundCount the new count of wiretaps found
     */
    public void setWiretapFoundCount(long wiretapFoundCount)
    {
        this.wiretapFoundCount = wiretapFoundCount;
    }

    /**
     * Retrieves the count of errors found for this node.
     *
     * @return the number of errors found, represented as a long value
     */
    public long getErrorFoundCount() {
        return errorFoundCount;
    }

    /**
     * Sets the count of errors found.
     *
     * @param errorFoundCount the new count of errors found
     */
    public void setErrorFoundCount(long errorFoundCount) {
        this.errorFoundCount = errorFoundCount;
    }

    /**
     * Retrieves the count of exclusions found.
     *
     * @return the number of exclusions encountered as a long value
     */
    public long getExclusionFoundCount() {
        return exclusionFoundCount;
    }

    /**
     * Sets the number of exclusions found associated with the node.
     *
     * @param exclusionFoundCount the count of exclusions to set
     */
    public void setExclusionFoundCount(long exclusionFoundCount) {
        this.exclusionFoundCount = exclusionFoundCount;
    }

    /**
     * Returns the count of replays found.
     *
     * @return the number of replays found as a long value
     */
    public long getReplayFoundCount() {
        return replayFoundCount;
    }

    /**
     * Sets the count of replays found for this node.
     *
     * @param replayFoundCount the number of replays to associate with this node
     */
    public void setReplayFoundCount(long replayFoundCount) {
        this.replayFoundCount = replayFoundCount;
    }

    /**
     * Returns the X-coordinate of the node.
     *
     * @return the X-coordinate of this node
     */
    public int getX() {
        return x;
    }

    /**
     * Retrieves the Y-coordinate of this node.
     *
     * @return the Y-coordinate as an integer.
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the x-coordinate of the node.
     *
     * @param x the new x-coordinate to be set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the y-coordinate of the node.
     *
     * @param y the y-coordinate to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Retrieves the image associated with this node.
     *
     * @return the image file path or identifier as a string.
     */
    public String getImage() {
        return image;
    }
}
