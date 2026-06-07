package org.ikasan.dashboard.ui.visualisation.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.ui.general.component.AbstractConfigurationDialog;
import org.ikasan.dashboard.ui.visualisation.model.flow.Flow;
import org.ikasan.dashboard.ui.visualisation.model.flow.FlowItemTypes;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.spec.module.client.ConfigurationService;

public class FlowConfigurationDialog extends AbstractConfigurationDialog
{
    private Flow flow;
    private DesignerCanvas designerCanvas;

    /**
     * Constructs a dialog for configuring a specific flow within a given module.
     *
     * @param module The module to which the flow belongs. It provides context and metadata
     *               necessary for configuration, such as the module's URL and name.
     * @param flow The flow to be configured. This provides access to flow-specific details,
     *             such as its name, coordinates, and recording status.
     * @param configurationRestService The service responsible for retrieving and saving
     *                                 configuration-related metadata for the flow.
     * @param designerCanvas The designer canvas instance where the flow's visual representation
     *                       can be updated based on changes in its configuration.
     */
    public FlowConfigurationDialog(Module module, Flow flow
        , ConfigurationService configurationRestService,  DesignerCanvas designerCanvas)
    {
        super(module, flow.getName(), null, configurationRestService);
        this.flow = flow;
        this.designerCanvas = designerCanvas;
        super.setHeight("500px");
    }

    @Override
    protected boolean loadConfigurationMetaData()
    {
        this.configurationMetaData = this.configurationRestService
            .getFlowConfiguration(module.getUrl(), module.getName(), flowName);

        return this.configurationMetaData != null;
    }

    @Override
    protected void save() {
        super.save();

        configurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("isRecording"))
            .findFirst()
            .ifPresent(configurationParameterMetaData -> flow.setRecording((Boolean) configurationParameterMetaData.getValue()));

        designerCanvas.removeFigure(flow.getName() + "-recording");

        if(flow.isRecording()) {
            ImageBuilder recordingBuilder = new ImageBuilder()
                .withId(flow.getName() + "-recording")
                .withX(flow.getX() + 20)
                .withY(flow.getY() + 20)
                .withWidth(30)
                .withHeight(30)
                .withSelectable(true)
                .withPath("frontend/images/replay-service.png")
                .withUserData(new UserDataBuilder().withItemType(FlowItemTypes.FLOW_RECORDING)
                    .build());

            try {
                designerCanvas.addImageFigureWithXYOfImageProvided(new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(recordingBuilder.build()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        this.close();
    }
}
