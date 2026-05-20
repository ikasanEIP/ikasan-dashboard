package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Created by stewmi on 07/11/2018.
 */
public class MessageChannel extends Node implements Destination
{
	public static final String IMAGE = "frontend/images/message-channel.png";

	private boolean isPrivate;

	public MessageChannel(DesignerItemIdentifier id, String name, boolean isPrivate)
	{
        super(id, -1, -1, IMAGE);
//        super(id, name, Nodes.builder().withShape(Shape.image).withImage(IMAGE));
		this.isPrivate = isPrivate;
	}

	public boolean isPrivate()
	{
		return isPrivate;
	}

    @Override
    public void setX(Integer x) {
        
    }
}
