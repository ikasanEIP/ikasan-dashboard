package org.ikasan.dashboard.ui.visualisation.layout;


import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.visualisation.model.flow.*;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.LabelBuilder;
import org.ikasan.designer.builder.RectangleBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.designer.model.Label;
import org.ikasan.spec.module.StartupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


/**
 * The IkasanFlowLayoutManager class is responsible for managing the layout of
 * flow diagrams within the application. It provides functionality for organizing
 * and visualizing flow-level elements, components, and their connections.
 *
 * This class is designed to work with the Ikasan Dashboard visualization framework
 * and offers specialized methods for handling flow-specific layout operations,
 * such as managing background elements, control components, labels, and edges.
 *
 * The layout logic considers the flow's configuration and ensures that all
 * components are visually represented and positioned appropriately for the diagram.
 */
public class IkasanFlowLayoutManager extends LayoutManagerBase implements LayoutManager
{
    Logger logger = LoggerFactory.getLogger(IkasanFlowLayoutManager.class);

    private Flow flow;

    public IkasanFlowLayoutManager(Flow flow)
    {
        this.flow = flow;
    }

    @Override
    public Draw2DLayout layout()
    {
        try {
            int x = xStart;
            int y = yStart;

            this.performInitialLayout(x, y);
            this.manageFlowLevelItems(x, y);
            this.manageFlowComponents(x, y);
            this.manageEdges();

            // push the down stream destinations beyond the boundary of the flow
            this.destinations.forEach(messageChannel -> messageChannel.setX(xExtentFinal + 200));

            // Now all the work is done get the Draw2D items.
            ArrayList<Object> items = diagramBuilder.build();

            return new Draw2DLayout(this.nodeList, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items));
        } catch (Exception e) {
            throw new LayoutManagerException(String.format("An exception has occurred attempting to layout flow[%s]"
                , flow.getName()), e);
        }
    }

    /**
     * Performs the initial layout for the flow by setting coordinates and managing transitions.
     * This method initializes the position of the consumer's source node within the layout,
     * adds the node to the layout's internal list, and logs the configuration details.
     * It also manages additional layout transitions starting from the consumer.
     *
     * @param x the x-coordinate where the consumer's source node is to be positioned
     * @param y the y-coordinate where the consumer's source node is to be positioned
     */
    private void performInitialLayout(int x, int y) {
        flow.getConsumer().getSource().setX(x);
        flow.getConsumer().getSource().setY(y);

        logger.debug("Adding consumer [{}] for flow [{}]. ", flow.getConsumer().getId().getUuid(), flow.getName());

        nodeList.add(flow.getConsumer().getSource());

        manageTransition(flow.getConsumer(), x, y);
    }

    /**
     * Manages the flow-level items in the diagram, including adding status boundaries, backgrounds,
     * labels, control images, and other elements based on the flow's configuration.
     *
     * @param x the x-coordinate used for positioning elements relative to the flow layout
     * @param y the y-coordinate used for positioning elements relative to the flow layout
     */
    private void manageFlowLevelItems(int x, int y) {
        // Add the status boundary for the flow reflecting the relevant status colour.
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

        // Now add the flow background.
        rb = diagramBuilder.getRectangleBuilder()
            .withId(flow.getName() + "_flow_background")
            .withWidth(xExtent - x)
            .withHeight(yExtent + 280 - y)
            .withStroke(0)
            .withRadius(20)
            .withX(x + 200)
            .withY(y - 120);

        diagramBuilder.addItem(rb.build());

        // Add a label for the flow
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

        // Sort out the startup image to reflect the flow start up type and add the image.
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

        // Finally add an image to reflect that the flow is recording if that is the case.
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
    }


    /**
     * Manages components within a flow by assigning coordinates, updating the diagram builder
     * with component images, wiretap indicators, and labels.
     * The method processes each node in the flow, creating visual elements and adding them
     * to the diagram layout as necessary.
     *
     * @param x the x-coordinate for positioning components within the diagram
     * @param y the y-coordinate for positioning components within the diagram
     */
    private void manageFlowComponents(int x, int y) {
        // Iterate over the node list which contains list of components that have been assigned
        // a x and y coordinate.
        this.nodeList.forEach(item -> {
            // Add an image for the component itself.
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

            // Now determine if we want to add any wiretap images to the component indicating that
            // there are wiretaps or logging wiretaps set on the component.
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

            // Finally add a label to the component.
            double labelLength = item.getId().getName().length() * this.fontSize * 0.65;

            Label label = new LabelBuilder().withText(item.getId().getName())
                .withX(item.getX() + 50 - (labelLength / 2))
                .withY(item.getY() + 80)
                .withFontSize(this.fontSize+"pt")
                .build();
            diagramBuilder.addItem(jobBuilder.build());
            diagramBuilder.addItem(label);
        });
    }

    /**
     * Manages the connections (edges) within the flow diagram by adding an edge
     * between the consumer's source node and the consumer itself, and delegating
     * further edge management to the superclass.
     *
     * This method ensures that a direct connection is established between the
     * source node's unique identifier and the consumer's unique identifier.
     * Additionally, it invokes the parent class's edge management logic to handle
     * more complex connections and transitions within the flow.
     */
    private void manageEdges() {
        addEdge(flow.getConsumer().getSource().getId().getUuid(), flow.getConsumer().getId().getUuid(), "", -1, -1);

        super.manageEdges(flow.getConsumer());
    }
}
