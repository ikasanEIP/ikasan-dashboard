package org.ikasan.dashboard.ui.visualisation.model.business.stream;

import org.ikasan.dashboard.ui.visualisation.correlate.Correlator;;
import org.ikasan.dashboard.ui.visualisation.util.BusinessStreamItemTypes;
import org.ikasan.designer.pallet.DesignerItemIdentifier;
import org.ikasan.vaadin.visjs.network.NodeFoundStatus;

import java.util.UUID;

public class Flow extends Node
{
    private String state = FlowState.RUNNING;
    private String moduleName;
    private String flowName;
    private String wiretapEvent;
    private Correlator correlator;
    private int width;
    private int height;

    private String statusIdentifier = UUID.randomUUID().toString();
    private String errorIdentifier = UUID.randomUUID().toString();
    private String errorCountLabelIdentifier = UUID.randomUUID().toString();
    private String exclusionIdentifier = UUID.randomUUID().toString();
    private String exclusionCountLabelIdentifier = UUID.randomUUID().toString();
    private String wiretapIdentifier = UUID.randomUUID().toString();
    private String wiretapCountLabelIdentifier = UUID.randomUUID().toString();
    private String replayIdentifier = UUID.randomUUID().toString();
    private String replayCountLabelIdentifier = UUID.randomUUID().toString();

    public Flow(String id, String moduleName, String flowName, int x, int y, int width, int height)
    {
        super(DesignerItemIdentifier.getIdentifier(id), x, y);
        this.moduleName = moduleName;
        this.flowName = flowName;
        this.width = width;
        this.height = height;
    }


    private void changeNodeStatusColour()
    {
        if(state == null)
        {
           return;
        }

        if(state.equals(FlowState.RUNNING))
        {
            super.setEdgeColour("rgba(0, 255, 0, 0.8)");
            super.setFillColour("rgba(0, 255, 0, 0.2)");
        }
        else if(state.equals(FlowState.STOPPED_IN_ERROR))
        {
            super.setEdgeColour("rgba(255, 0, 0, 0.8)");
            super.setFillColour("rgba(255, 0, 0, 0.2)");
        }
        else if(state.equals(FlowState.RECOVERING))
        {
            super.setEdgeColour("rgba(238, 108, 15, 0.8)");
            super.setFillColour("rgba(238, 108, 15, 0.2)");
        }
        else if(state.equals(FlowState.STOPPED))
        {
            super.setEdgeColour("rgba(0, 0, 255, 0.8)");
            super.setFillColour("rgba(0, 0, 255, 0.2)");
        }
        else if(state.equals(FlowState.PAUSED))
        {
            super.setEdgeColour("rgba(165, 26, 255, 0.8)");
            super.setFillColour("rgba(165, 26, 255, 0.2)");
        }
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFlowName() {
        return flowName;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
        changeNodeStatusColour();
    }

    public String getWiretapEvent()
    {
        return wiretapEvent;
    }

    public void setWiretapEvent(String wiretapEvent)
    {
        this.wiretapEvent = wiretapEvent;
    }

    public Correlator getCorrelator()
    {
        return correlator;
    }

    public void setCorrelator(Correlator correlator)
    {
        this.correlator = correlator;
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getStatusIdentifier() {
        return statusIdentifier;
    }

    public DesignerItemIdentifier getErrorIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.ERROR.name(), this.getId().getName(),
            this.errorIdentifier);
    }

    public DesignerItemIdentifier getExclusionIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.EXCLUSION.name(), this.getId().getName(),
            this.exclusionIdentifier);
    }

    public DesignerItemIdentifier getWiretapIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.WIRETAP.name(), this.getId().getName(),
            this.wiretapIdentifier);
    }

    public DesignerItemIdentifier getReplayIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.REPLAY.name(), this.getId().getName(),
            this.replayIdentifier);
    }

    public DesignerItemIdentifier getErrorCountLabelIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.ERROR.name(), this.getId().getName(),
            this.errorCountLabelIdentifier);
    }

    public DesignerItemIdentifier getExclusionCountLabelIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.EXCLUSION.name(), this.getId().getName(),
            this.exclusionCountLabelIdentifier);
    }

    public DesignerItemIdentifier getWiretapCountLabelIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.WIRETAP.name(), this.getId().getName(),
            this.wiretapCountLabelIdentifier);
    }

    public DesignerItemIdentifier getReplayCountLabelIdentifier() {
        return new DesignerItemIdentifier(BusinessStreamItemTypes.REPLAY.name(), this.getId().getName(),
            this.replayCountLabelIdentifier);
    }
}
