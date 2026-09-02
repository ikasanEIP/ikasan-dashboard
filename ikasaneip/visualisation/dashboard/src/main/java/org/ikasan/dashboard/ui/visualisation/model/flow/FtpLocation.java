package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Represents a specific type of destination in the form of an FTP location.
 * This class extends the base {@code Node} class and implements the {@code DestinationImpl} interface.
 *
 * An {@code FtpLocation} object is used to model an FTP server or location with associated metadata,
 * such as a unique identifier, name, and an image representation.
 */
public class FtpLocation extends Node implements Destination
{
	public static final String IMAGE = "frontend/images/ftp-location.png";


	/**
     * Constructs a new {@code FtpLocation} object with a specified identifier and name.
     * The object represents an FTP destination with an associated image.
     *
     * @param id   the unique identifier for this FTP location, encapsulated in a {@code DesignerItemIdentifier} object
     * @param name the name of the FTP location
     */
    public FtpLocation(DesignerItemIdentifier id, String name)
	{
        super(id, -1, -1, IMAGE);
	}

    @Override
    public void setX(Integer x) {

    }
}
