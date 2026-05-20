package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Created by stewmi on 07/11/2018.
 */
public class FtpLocation extends Node implements Destination
{
	public static final String IMAGE = "frontend/images/ftp-location.png";


	public FtpLocation(DesignerItemIdentifier id, String name)
	{
        super(id, -1, -1, IMAGE);
//        super(id, name, Nodes.builder().withShape(Shape.image).withImage(IMAGE));
	}

    @Override
    public void setX(Integer x) {

    }
}
