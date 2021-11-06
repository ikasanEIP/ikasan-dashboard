package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.ikasan.solr.model.IkasanSolrDocument;

public class EntityContentsViewDialog extends AbstractEntityViewDialog<IkasanSolrDocument> {

    private String titleString;

    public EntityContentsViewDialog(String titleString) {
        super();
        this.titleString = titleString;

        super.aceEditor.setHeight("100vh");
        super.title.setText(titleString);
    }

    @Override
    public Component getEntityDetailsLayout() {
        return new HorizontalLayout();
    }

    @Override
    public void populate(IkasanSolrDocument solrDocument) {
        super.open(solrDocument.getEvent());
    }
}
