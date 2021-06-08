package org.ikasan.designer.action;

import com.vaadin.flow.router.BeforeLeaveEvent;

public class IgnoreSaveAndNavigateAction implements DesignerAction {

    private BeforeLeaveEvent.ContinueNavigationAction continueNavigationAction;

    public IgnoreSaveAndNavigateAction(BeforeLeaveEvent.ContinueNavigationAction continueNavigationAction) {
        this.continueNavigationAction = continueNavigationAction;
    }

    @Override
    public void execute() {
        this.continueNavigationAction.proceed();
    }
}
