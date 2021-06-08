package org.ikasan.designer.action;

import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.function.OpenFunction;

public class IgnoreSaveAndNewAction implements DesignerAction {

    private DesignerCanvas designerCanvas;

    public IgnoreSaveAndNewAction(DesignerCanvas designerCanvas) {
        this.designerCanvas = designerCanvas;
    }

    @Override
    public void execute() {
        this.designerCanvas.clear();
    }
}
