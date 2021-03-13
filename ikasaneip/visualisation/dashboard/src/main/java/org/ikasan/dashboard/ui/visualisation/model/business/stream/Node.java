package org.ikasan.dashboard.ui.visualisation.model.business.stream;

import org.ikasan.vaadin.visjs.network.NodeFoundStatus;

public class Node
{
    private String id;
    private String edgeColour = "rgba(0, 255, 0, 0.8)";
    private String fillColour = "rgba(0, 255, 0, 0.2)";
    private String wiretapFoundStatus = NodeFoundStatus.EMPTY;
    private String errorFoundStatus = NodeFoundStatus.EMPTY;
    private String exclusionFoundStatus = NodeFoundStatus.EMPTY;
    private String replayFoundStatus = NodeFoundStatus.EMPTY;

    private int x;
    private int y;


    public Node(String id, int x, int y)
    {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
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

    public String getWiretapFoundStatus()
    {
        return wiretapFoundStatus;
    }

    public void setWiretapFoundStatus(String wiretapFoundStatus)
    {
        this.wiretapFoundStatus = wiretapFoundStatus;
    }

    public String getErrorFoundStatus() {
        return errorFoundStatus;
    }

    public void setErrorFoundStatus(String errorFoundStatus) {
        this.errorFoundStatus = errorFoundStatus;
    }

    public String getExclusionFoundStatus() {
        return exclusionFoundStatus;
    }

    public void setExclusionFoundStatus(String exclusionFoundStatus) {
        this.exclusionFoundStatus = exclusionFoundStatus;
    }

    public String getReplayFoundStatus() {
        return replayFoundStatus;
    }

    public void setReplayFoundStatus(String replayFoundStatus) {
        this.replayFoundStatus = replayFoundStatus;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
