package org.ikasan.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.Edge;

public class EdgeImpl implements Edge
{
    private String from;
    private String to;


    public String getFrom()
    {
        return from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getTo()
    {
        return to;
    }

    public void setTo(String to)
    {
        this.to = to;
    }
}
