package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a message channel in the system, which can be used as a destination.
 * A message channel is an extension of the Node class and implements the DestinationImpl interface.
 * It encapsulates properties and behaviors specific to message channels, such as privacy attributes.
 */
public class MessageChannel extends Node implements Destination
{
	public static final String IMAGE = "frontend/images/message-channel.png";

	private boolean isPrivate;

	/**
     * Constructs a new MessageChannel object with the specified identifier, name, and privacy setting.
     *
     * @param id       the unique identifier of the message channel, represented by a DesignerItemIdentifier object
     * @param name     the name of the message channel
     * @param isPrivate a boolean indicating whether the message channel is private
     */
    public MessageChannel(DesignerItemIdentifier id, String name, boolean isPrivate)
	{
        super(id, -1, -1, IMAGE);
		this.isPrivate = isPrivate;
	}

	/**
     * Indicates whether the message channel is private.
     *
     * @return true if the message channel is private; false otherwise.
     */
    public boolean isPrivate()
	{
		return isPrivate;
	}

    @Override
    public void setX(Integer x) {
        
    }
}
