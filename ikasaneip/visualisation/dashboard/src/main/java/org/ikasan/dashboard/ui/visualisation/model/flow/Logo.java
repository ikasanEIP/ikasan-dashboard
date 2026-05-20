package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * Created by stewmi on 07/11/2018.
 */
public class Logo extends Node
{
	public static final String IMAGE = "frontend/images/ikasan-titling-transparent.png";

	public Logo(DesignerItemIdentifier id)
	{
        super(id, -1, -1, IMAGE);
//        super(id, "", Nodes.builder().withShape(Shape.image).withImage(IMAGE));
	}
}
