package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * The FileLocation class represents a specific type of node that corresponds
 * to a file's physical or logical location. It extends the Node class and
 * implements the DestinationImpl interface, inheriting common node-related
 * properties and behaviors while enforcing additional destination-specific
 * functionality.
 *
 * This class is uniquely identified by a DesignerItemIdentifier and includes
 * a static image path reference representing its visual appearance in a GUI.
 * It can also override behavior defined by its parent or implemented interface.
 */
public class FileLocation extends Node implements Destination
{
	public static final String IMAGE = "frontend/images/file-location.png";


	/**
     * Constructs a FileLocation object with a specified unique identifier and name.
     * This object represents a node that corresponds to a file's physical or logical location.
     *
     * @param id   the unique identifier of the file location, represented as a DesignerItemIdentifier
     * @param name the name associated with the file location
     */
    public FileLocation(DesignerItemIdentifier id, String name)
	{
        super(id, -1, -1, IMAGE);
	}

    @Override
    public void setX(Integer x) {

    }
}
