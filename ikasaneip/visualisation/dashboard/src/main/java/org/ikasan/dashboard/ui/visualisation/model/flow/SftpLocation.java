package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents an SFTP (Secure File Transfer Protocol) node in a visual design or workflow application.
 *
 * The SftpLocation class extends the Node class and implements the DestinationImpl interface,
 * providing additional configuration specific to SFTP-based destinations.
 * This class sets a default image to visually represent the SFTP location within the application and
 * inherits core functionality such as coordinate management and identifier association from its superclass.
 */
public class SftpLocation extends Node implements Destination
{
	public static final String IMAGE = "frontend/images/sftp-location.png";


	/**
     * Constructs a new SftpLocation instance to represent an SFTP node in a workflow or visual design application.
     * This class uses a default SFTP image and inherits functionality from the Node class.
     *
     * @param id   the unique identifier for the SFTP node, which contains type, name, and UUID information
     * @param name the name of the SFTP location
     */
    public SftpLocation(DesignerItemIdentifier id, String name)
	{
        super(id, -1, -1, IMAGE);
	}

    @Override
    public void setX(Integer x) {
        super.setX(x);
    }
}
