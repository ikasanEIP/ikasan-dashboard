package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.ikasan.spec.search.model.IkasanESBDocument;

public class EntityContentsViewDialog extends AbstractEntityViewDialog<IkasanESBDocument> {

    public EntityContentsViewDialog(String titleString) {
        super();
        super.aceEditor.setHeight("100vh");
        super.title.setText(titleString);
    }

    @Override
    public Component getEntityDetailsLayout() {
        return new HorizontalLayout();
    }

    @Override
    public void populate(IkasanESBDocument document) {
        super.open(document.getEvent());
    }
}
