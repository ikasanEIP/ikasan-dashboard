package org.ikasan.dashboard.ui.visualisation.layout;


import org.ikasan.dashboard.ui.visualisation.model.flow.Draw2DLayout;
import org.ikasan.dashboard.ui.visualisation.model.flow.Flow;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by stewmi on 08/11/2018.
 */
public class IkasanModuleLayoutManager extends LayoutManagerBase implements LayoutManager
{
    Logger logger = LoggerFactory.getLogger(IkasanModuleLayoutManager.class);

    private Module module;

    protected int flowSpacing = 300;

    public IkasanModuleLayoutManager(Module module)
    {
        this.module = module;
    }

    public Draw2DLayout layout()
    {
        int x = xStart;
        int y = yStart;

        logger.debug("Laying out module [{}]. ", module.getName());

        for(Flow flow: module.getFlows())
        {
            logger.debug("Laying out flow [{}]. ", module.getName());

            flow.getConsumer().setX(x);
            flow.getConsumer().setY(y);

            logger.debug("Adding consumer [{}] for flow [{}]. ", flow.getConsumer().getId().getName(), flow.getName());

            nodeList.add(flow.getConsumer());

            addEdge(flow.getConsumer().getId().getUuid(), flow.getConsumer().getTransition().getId().getUuid(), flow.getConsumer().getTransitionLabel(),
                -1, -1);

            manageTransition(flow.getConsumer().getTransition(), x, y);

            x = xStart;
            xExtent = x;
            y = yExtent + flowSpacing;
        }

        this.destinations.forEach(destination -> destination.setX(xExtentFinal + 300));

        return null;
    }

}
