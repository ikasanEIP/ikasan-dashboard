package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Created by stewmi on 07/11/2018.
 */
public class FileLocation extends Node implements Destination
{
	public static final String IMAGE = "frontend/images/file-location.png";


	public FileLocation(DesignerItemIdentifier id, String name)
	{
        super(id, -1, -1, IMAGE);
//        super(id, name, Nodes.builder().withShape(Shape.image).withImage(IMAGE));
	}

    @Override
    public void setX(Integer x) {

    }
}
