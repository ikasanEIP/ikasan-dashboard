package org.ikasan.designer.action;

import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.function.OpenFunction;

public class IgnoreSaveAndOpenAction implements DesignerAction {

    private OpenFunction openFunction;
    private DesignerCanvas designerCanvas;

    public IgnoreSaveAndOpenAction(OpenFunction openFunction, DesignerCanvas designerCanvas) {
        this.openFunction = openFunction;
        this.designerCanvas = designerCanvas;
    }

    @Override
    public void execute() {
        this.openFunction.open(designerCanvas);
    }
}
