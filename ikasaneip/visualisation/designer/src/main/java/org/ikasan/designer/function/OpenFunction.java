package org.ikasan.designer.function;

import org.ikasan.designer.DesignerCanvas;

public interface OpenFunction {
    String getName();
    String getId();
    String getDescription();
    String getJson();
    void open(DesignerCanvas designerCanvas);
}
