package org.ikasan.rest.dashboard.component.converter;

import org.ikasan.rest.dashboard.model.metadata.module.DecoratorMetaDataImpl;
import org.ikasan.rest.dashboard.model.metadata.module.FlowElementMetaDataImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.rest.dashboard.model.metadata.module.TransitionImpl;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.ikasan.spec.metadata.model.*;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.topology.metadata.model.FlowMetaDataImpl;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class IkasanESBDocumentToModuleMetaDataConverter implements Converter<IkasanESBDocument, ModuleMetaData> {

    private final JsonMapper jsonMapper;

    public IkasanESBDocumentToModuleMetaDataConverter() {
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(FlowMetaData.class, FlowMetaDataImpl.class)
            .addAbstractTypeMapping(FlowElementMetaData.class, FlowElementMetaDataImpl.class)
            .addAbstractTypeMapping(Transition.class, TransitionImpl.class)
            .addAbstractTypeMapping(DecoratorMetaData.class, DecoratorMetaDataImpl.class);

        jsonMapper = JsonMapper.builder().addModule(simpleModule).build();
    }

    @Override
    public ModuleMetaData convert(IkasanESBDocument ikasanESBDocument) throws TransformationException {
        return jsonMapper.readValue(ikasanESBDocument.getEvent(), ModuleMetaDataImpl.class);
    }
}
