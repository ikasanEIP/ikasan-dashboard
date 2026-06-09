package org.ikasan.dashboard.ui.visualisation.layout;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.visualisation.model.flow.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.LabelBuilder;
import org.ikasan.designer.builder.RectangleBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.designer.model.Label;
import org.ikasan.spec.module.StartupType;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


/**
 * Created by stewmi on 08/11/2018.
 */
public class IkasanFlowLayoutManager extends LayoutManagerBase implements LayoutManager
{
    Logger logger = LoggerFactory.getLogger(IkasanFlowLayoutManager.class);

    private Flow flow;

    public IkasanFlowLayoutManager(Flow flow)
    {
        this.flow = flow;
    }

    public Draw2DLayout layout()
    {
        int x = xStart;
        int y = yStart;


        flow.getConsumer().getSource().setX(x);
        flow.getConsumer().getSource().setY(y);

        logger.debug("Adding consumer [{}] for flow [{}]. ", flow.getConsumer().getId().getUuid(), flow.getName());

        nodeList.add(flow.getConsumer().getSource());

        manageTransition(flow.getConsumer(), x, y);

        RectangleBuilder rb = diagramBuilder.getRectangleBuilder();
        rb.withId(flow.getName() + "_status")
            .withWidth(xExtent - x + 20)
            .withHeight(yExtent + 300 - y)
            .withRadius(20)
            .withStroke(5)
            .withX(x + 190)
            .withY(y - 130);

        rb.withBgColor(IkasanColours.TRANSPARENT);
        rb.withColor(flow.getStatus().getStateColour());

        diagramBuilder.addItem(rb.build());

        rb = diagramBuilder.getRectangleBuilder()
            .withId(flow.getName() + "_flow_background")
            .withWidth(xExtent - x)
            .withHeight(yExtent + 280 - y)
            .withStroke(0)
            .withRadius(20)
            .withX(x + 200)
            .withY(y - 120);

        diagramBuilder.addItem(rb.build());

        double flowLabelLength = flow.getName().length() * this.fontSize * 0.65;

        Label flowLabel = new LabelBuilder()
            .withText(flow.getName())
            .withId(flow.getName()+"-label")
            .withX(((double) ((x + 150) + ((xExtent - x) / 2)) - (flowLabelLength / 2)))
            .withY( y - 110)
            .withFontSize("20pt")
            .build();

        diagramBuilder.addItem(flowLabel);

        flow.setBorder(x + 190, y - 130, xExtent - x + 20, yExtent + 300 - y);
        flow.setControlRelativeX(-80);
        flow.setControlRelativeY(-30);
        flow.setControlImageH(60);
        flow.setControlImageW(60);

        ImageBuilder startupBuilder = diagramBuilder.getImageBuilder()
            .withId(flow.getName() + "-start-up")
            .withX(flow.getControlX())
            .withY(flow.getControlY())
            .withWidth(flow.getControlImageW())
            .withHeight(flow.getControlImageW())
            .withSelectable(true)
            .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.FLOW_START_UP_CONTROL)
                .build());

        if(flow.getStartupType() != null) {
            if (flow.getStartupType().equals(StartupType.MANUAL)) {
                startupBuilder
                    .withPath(FlowStartup.FLOW_MANUAL_IMAGE);
                diagramBuilder.addItem(startupBuilder.build());
            } else if (flow.getStartupType().equals(StartupType.AUTOMATIC)) {
                startupBuilder
                    .withPath(FlowStartup.FLOW_AUTO_IMAGE);
                diagramBuilder.addItem(startupBuilder.build());
            } else if (flow.getStartupType().equals(StartupType.DISABLED)) {
                startupBuilder
                    .withPath(FlowStartup.FLOW_DISABLED_IMAGE);
                diagramBuilder.addItem(startupBuilder.build());
            }
        }

        if(flow.isRecording()) {
            ImageBuilder recordingBuilder = diagramBuilder.getImageBuilder()
                .withId(flow.getName() + "-recording")
                .withX(flow.getX() + 20)
                .withY(flow.getY() + 20)
                .withWidth(30)
                .withHeight(30)
                .withSelectable(true)
                .withPath("frontend/images/replay-service.png")
                .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.FLOW_RECORDING)
                    .build());

            diagramBuilder.addItem(recordingBuilder.build());
        }

        this.nodeList.forEach(item -> {
            ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                .withId(item.getId().getUuid())
                .withX(item.getX())
                .withY(item.getY())
                .withRightPort()
                .withLeftPort()
                .withPath(item.getImage())
                .withSelectable(true)
                .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.FLOW_COMPONENT)
                    .withComponentName(item.getName())
                    .build());

            if(item instanceof AbstractWiretapNode) {
                if(((AbstractWiretapNode) item).getWiretapBeforeStatus().equals(NodeFoundStatus.FOUND)) {
                    ImageBuilder wiretapBuilder = diagramBuilder.getImageBuilder()
                        .withId(item.getId().getUuid() + "before-wiretap")
                        .withWidth(((AbstractWiretapNode) item).getWiretapBeforeImageW())
                        .withHeight(((AbstractWiretapNode) item).getWiretapBeforeImageH())
                        .withX(item.getX() + ((AbstractWiretapNode) item).getWiretapBeforeImageX())
                        .withY(item.getY() + ((AbstractWiretapNode) item).getWiretapBeforeImageY())
                        .withSelectable(true)
                        .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.BEFORE_WIRETAP)
                            .withIdentifier(((AbstractWiretapNode) item).getDecoratorMetaDataList().stream()
                                .filter(decoratorMetaData -> decoratorMetaData.getType().equals("Wiretap")
                                    && decoratorMetaData.getName().startsWith("BEFORE")).findFirst().get().getConfigurationId())
                            .withComponentName(item.getName())
                            .build())
                        .withPath(AbstractWiretapNode.WIRETAP_IMAGE);

                    diagramBuilder.addItem(wiretapBuilder.build());
                }

                if(((AbstractWiretapNode) item).getLogWiretapBeforeStatus().equals(NodeFoundStatus.FOUND)) {
                    ImageBuilder wiretapBuilder = diagramBuilder.getImageBuilder()
                        .withId(item.getId().getUuid() + "before-log-wiretap")
                        .withWidth(((AbstractWiretapNode) item).getLogWiretapBeforeImageW())
                        .withHeight(((AbstractWiretapNode) item).getLogWiretapBeforeImageH())
                        .withX(item.getX() + ((AbstractWiretapNode) item).getLogWiretapBeforeImageX())
                        .withY(item.getY() + ((AbstractWiretapNode) item).getLogWiretapBeforeImageY())
                        .withSelectable(true)
                        .withUserData(new UserDataBuilder()
                            .withItemType(FlowItemTypes.BEFORE_LOGGING_WIRETAP)
                            .withIdentifier(((AbstractWiretapNode) item).getDecoratorMetaDataList().stream()
                                .filter(decoratorMetaData -> decoratorMetaData.getType().equals("LogWiretap")
                                    && decoratorMetaData.getName().startsWith("BEFORE")).findFirst().get().getConfigurationId())
                            .withComponentName(item.getName())
                            .build())
                        .withPath(AbstractWiretapNode.LOG_WIRETAP_IMAGE);

                    diagramBuilder.addItem(wiretapBuilder.build());
                }

                if(((AbstractWiretapNode) item).getWiretapAfterStatus().equals(NodeFoundStatus.FOUND)) {
                    ImageBuilder wiretapBuilder = diagramBuilder.getImageBuilder()
                        .withId(item.getId().getUuid() + "after-wiretap")
                        .withWidth(((AbstractWiretapNode) item).getWiretapAfterImageW())
                        .withHeight(((AbstractWiretapNode) item).getWiretapAfterImageH())
                        .withX(item.getX() + ((AbstractWiretapNode) item).getWiretapAfterImageX())
                        .withY(item.getY() + ((AbstractWiretapNode) item).getWiretapAfterImageY())
                        .withSelectable(true)
                        .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.AFTER_WIRETAP)
                            .withIdentifier(((AbstractWiretapNode) item).getDecoratorMetaDataList().stream()
                                .filter(decoratorMetaData -> decoratorMetaData.getType().equals("Wiretap")
                                    && decoratorMetaData.getName().startsWith("AFTER")).findFirst().get().getConfigurationId())
                            .withComponentName(item.getName())
                            .build())
                        .withPath(AbstractWiretapNode.WIRETAP_IMAGE);

                    diagramBuilder.addItem(wiretapBuilder.build());
                }

                if(((AbstractWiretapNode) item).getLogWiretapAfterStatus().equals(NodeFoundStatus.FOUND)) {
                    ImageBuilder wiretapBuilder = diagramBuilder.getImageBuilder()
                        .withId(item.getId().getUuid() + "after-log-wiretap")
                        .withWidth(((AbstractWiretapNode) item).getLogWiretapAfterImageW())
                        .withHeight(((AbstractWiretapNode) item).getLogWiretapAfterImageH())
                        .withX(item.getX() + ((AbstractWiretapNode) item).getLogWiretapAfterImageX())
                        .withY(item.getY() + ((AbstractWiretapNode) item).getLogWiretapAfterImageY())
                        .withSelectable(true)
                        .withUserData(new UserDataBuilder()
                            .withItemType(FlowItemTypes.AFTER_LOGGING_WIRETAP)
                            .withIdentifier(((AbstractWiretapNode) item).getDecoratorMetaDataList().stream()
                                .filter(decoratorMetaData -> decoratorMetaData.getType().equals("LogWiretap")
                                    && decoratorMetaData.getName().startsWith("AFTER")).findFirst().get().getConfigurationId())
                            .withComponentName(item.getName())
                            .build())
                        .withPath(AbstractWiretapNode.LOG_WIRETAP_IMAGE);

                    diagramBuilder.addItem(wiretapBuilder.build());
                }
            }

            double labelLength = item.getId().getName().length() * this.fontSize * 0.65;

            Label label = new LabelBuilder().withText(item.getId().getName())
                .withX(item.getX() + 50 - (labelLength / 2))
                .withY(item.getY() + 80)
                .withFontSize(this.fontSize+"pt")
                .build();
            diagramBuilder.addItem(jobBuilder.build());
            diagramBuilder.addItem(label);
        });

        addEdge(flow.getConsumer().getSource().getId().getUuid(), flow.getConsumer().getId().getUuid(), "", -1, -1);

        super.manageEdges(flow.getConsumer());

        ArrayList<Object> items = diagramBuilder.build();

        this.destinations.forEach(messageChannel -> messageChannel.setX(xExtentFinal + 200));

        try {
            return new Draw2DLayout(this.nodeList, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
