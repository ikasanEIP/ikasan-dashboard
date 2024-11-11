package org.ikasan.scheduled.visualisation.model;

import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayout;

public class SolrContextVisualisationLayoutImpl implements ContextVisualisationLayout {
    private String layoutJson;

    @Override
    public String getLayoutJson() {
        return this.layoutJson;
    }

    @Override
    public void setLayoutJson(String layoutJson) {
        this.layoutJson = layoutJson;
    }
}
