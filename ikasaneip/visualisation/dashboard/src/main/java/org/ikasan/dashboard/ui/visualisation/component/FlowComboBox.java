package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcastListener;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.cache.CacheStateBroadcastListener;
import org.ikasan.dashboard.cache.CacheStateBroadcaster;
import org.ikasan.dashboard.ui.visualisation.model.flow.Flow;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowComboBox extends ComboBox<Flow> implements FlowStateBroadcastListener, CacheStateBroadcastListener
{
    Logger logger = LoggerFactory.getLogger(FlowComboBox.class);
    private Module currentModule;
    private UI ui;

    public FlowComboBox()
    {
    }

    public void setCurrentModule(Module currentModule)
    {
        this.currentModule = currentModule;
        this.setItems(this.currentModule.getFlows());
        this.setValue(this.currentModule.getFlows().get(0));
        this.setLabel(this.currentModule.getName());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();
        FlowStateBroadcaster.register(this);
        CacheStateBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent)
    {
        super.onDetach(detachEvent);
        FlowStateBroadcaster.unregister(this);
        CacheStateBroadcaster.unregister(this);
    }

    @Override
    public void receiveFlowStateBroadcast(FlowState flowState) {
        if(this.ui != null && ui.isAttached()) {
            ui.access(() ->
            {
                logger.debug("Received flow state: " + flowState);

                if (this.currentModule != null) {
                    Flow flow = this.getValue();
                    setItems(currentModule.getFlows());
                    this.getDataProvider().refreshAll();
                    this.setValue(flow);
                }
            });
        }
    }

    @Override
    public void receiveCacheStateBroadcast(FlowState flowState) {
        if(ui != null && ui.isAttached()) {
            ui.access(() ->
            {
                logger.debug("Received flow state: " + flowState);

                if (this.currentModule != null) {
                    Flow flow = this.getValue();
                    setItems(currentModule.getFlows());
                    this.getDataProvider().refreshAll();
                    this.setValue(flow);
                }
            });
        }
    }
}
