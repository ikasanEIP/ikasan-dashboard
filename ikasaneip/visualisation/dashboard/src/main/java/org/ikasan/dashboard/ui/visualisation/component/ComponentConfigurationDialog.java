package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.general.component.AbstractConfigurationDialog;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.spec.module.client.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentConfigurationDialog extends AbstractConfigurationDialog
{
    private static Logger logger = LoggerFactory.getLogger(ComponentConfigurationDialog.class);

    /**
     * Constructor
     *
     * @param module
     * @param flowName
     * @param componentName
     * @param configurationRestService
     */
    public ComponentConfigurationDialog(Module module, String flowName, String componentName
        , ConfigurationService configurationRestService)
    {
        super(module, flowName, componentName, configurationRestService);
        super.setWidth("60vw");
        super.setHeight("80vh");
    }

    @Override
    protected boolean loadConfigurationMetaData()
    {
        try {
            this.configurationMetaData = this.configurationRestService
                .getConfiguredResourceConfiguration(module.getUrl(), module.getName(), flowName, componentName);

            if(configurationMetaData != null) {
                super.title.setText(getTranslation("button.component-configuration", UI.getCurrent().getLocale())
                    + " - " + this.configurationMetaData.getConfigurationId());
            }

            return this.configurationMetaData != null;
        } catch (Exception e) {
            logger.error(String.format("An error has occurred attempting to open configuration for component. " +
                "Module Name[%s], Module URL[%s], Flow Name[%s], Component Name[%s]", module.getName(), module.getUrl(),
                flowName, componentName), e);
            return false;
        }
    }
}
