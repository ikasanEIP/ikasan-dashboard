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

    public DesignerItemIdentifier getId()
    {
        return id;
    }

    public void setId(DesignerItemIdentifier id)
    {
        this.id = id;
    }

    public String getEdgeColour()
    {
        return edgeColour;
    }

    public void setEdgeColour(String edgeColour)
    {
        this.edgeColour = edgeColour;
    }

    public String getFillColour()
    {
        return fillColour;
    }

    public void setFillColour(String fillColour)
    {
        this.fillColour = fillColour;
    }

    public long getWiretapFoundCount()
    {
        return wiretapFoundCount;
    }

    public void setWiretapFoundCount(long wiretapFoundCount)
    {
        this.wiretapFoundCount = wiretapFoundCount;
    }

    public long getErrorFoundCount() {
        return errorFoundCount;
    }

    public void setErrorFoundCount(long errorFoundCount) {
        this.errorFoundCount = errorFoundCount;
    }

    public long getExclusionFoundCount() {
        return exclusionFoundCount;
    }

    public void setExclusionFoundCount(long exclusionFoundCount) {
        this.exclusionFoundCount = exclusionFoundCount;
    }

    public long getReplayFoundCount() {
        return replayFoundCount;
    }

    public void setReplayFoundCount(long replayFoundCount) {
        this.replayFoundCount = replayFoundCount;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
