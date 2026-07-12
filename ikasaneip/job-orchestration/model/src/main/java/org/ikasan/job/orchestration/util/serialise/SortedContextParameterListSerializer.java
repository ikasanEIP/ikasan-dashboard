package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Custom JSON serializer for sorting and serializing a list of ContextParameters in ascending order based on name.
 * Extends ValueSerializer to provide custom serialization logic.
 */
public class SortedContextParameterListSerializer extends ValueSerializer<List<ContextParameter>> {

    @Override
    public void serialize(List<ContextParameter> list, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getName() != null && b.getName() != null) {
                    return a.getName().compareTo(b.getName());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        gen.writeStartArray();
        if (list != null) {
            for (ContextParameter param : list) {
                ctxt.writeValue(gen, param);
            }
        }
        gen.writeEndArray();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, List<ContextParameter> value) {
        return value == null || value.isEmpty();
    }
}
