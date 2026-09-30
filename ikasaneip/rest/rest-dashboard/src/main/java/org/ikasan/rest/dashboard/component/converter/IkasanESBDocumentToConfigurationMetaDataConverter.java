package org.ikasan.rest.dashboard.component.converter;

import org.ikasan.rest.dashboard.model.metadata.configuration.ConfigurationMetaDataImpl;
import org.ikasan.rest.dashboard.model.metadata.configuration.ConfigurationParameterMetaDataImpl;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.ikasan.spec.metadata.model.ConfigurationParameterMetaData;
import org.ikasan.spec.search.model.IkasanESBDocument;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class IkasanESBDocumentToConfigurationMetaDataConverter implements Converter<IkasanESBDocument, ConfigurationMetaData> {

    private final JsonMapper jsonMapper;

    public IkasanESBDocumentToConfigurationMetaDataConverter() {
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(ConfigurationParameterMetaData.class, ConfigurationParameterMetaDataImpl.class);

        jsonMapper = JsonMapper.builder().addModule(simpleModule).build();
    }

    @Override
    public ConfigurationMetaData convert(IkasanESBDocument ikasanESBDocument) throws TransformationException {
        return jsonMapper.readValue(ikasanESBDocument.getEvent(), ConfigurationMetaDataImpl.class);
    }
}
