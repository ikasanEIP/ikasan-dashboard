package org.ikasan.dashboard.ui.visualisation.layout;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.dashboard.ui.visualisation.model.flow.Flow;
import org.ikasan.dashboard.ui.visualisation.model.flow.Logo;
import org.ikasan.designer.builder.DiagramBuilder;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.designer.model.PositionedItem;
import org.ikasan.designer.model.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;


/**
 * Created by stewmi on 08/11/2018.
 */
public class IkasanFlowLayoutManager extends LayoutManagerBase implements LayoutManager
{
    Logger logger = LoggerFactory.getLogger(IkasanFlowLayoutManager.class);

    protected DiagramBuilder diagramBuilder = new DiagramBuilder();

    private Flow flow;

    public IkasanFlowLayoutManager(Flow flow)
    {
        this.flow = flow;
    }

    public String layout()
    {
        int x = xStart;
        int y = yStart;


        flow.getConsumer().getSource().setX(x);
        flow.getConsumer().getSource().setY(y);

        logger.debug("Adding consumer [{}] for flow [{}]. ", flow.getConsumer().getId().getUuid(), flow.getName());

        nodeList.add(flow.getConsumer().getSource());

        addEdge(flow.getConsumer().getSource().getId().getUuid(), flow.getConsumer().getId().getUuid(), "");

        manageTransition(flow.getConsumer(), x, y);

        flow.setBorder(x + 100, y - 150, xExtent - x , yExtent + 250 - y);

        this.nodeList.forEach(item -> {
            ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                .withId(item.getId().getUuid())
//                .withWidth(30)
//                .withHeight(30)
                .withX(item.getX())
                .withY(item.getY())
                .withPath(item.getImage())
//                .withUserData(new UserDataBuilder().withItemType(UserData.).build())
                .withComposite(item.getId().getUuid());

            diagramBuilder.addItem(jobBuilder.build());
        });

        ArrayList<Object> items = diagramBuilder.build();

        this.destinations.forEach(messageChannel -> messageChannel.setX(xExtentFinal + 200));

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
