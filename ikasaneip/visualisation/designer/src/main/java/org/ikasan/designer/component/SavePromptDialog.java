package org.ikasan.designer.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.ikasan.designer.action.DesignerAction;

public class SavePromptDialog extends ConfirmDialog {
    public SavePromptDialog(DesignerAction designerAction, String header, String text,
                            String okButton, String cancelButton) {
        super(header, text, okButton
            , (ComponentEventListener<ConfirmDialog.ConfirmEvent>) confirmEvent -> {
                if(designerAction != null) {
               designerAction.execute();
            }
        }, cancelButton, (ComponentEventListener<ConfirmDialog.CancelEvent>) cancelEvent -> {});
        super.setConfirmButtonTheme("error primary");
    }
}
