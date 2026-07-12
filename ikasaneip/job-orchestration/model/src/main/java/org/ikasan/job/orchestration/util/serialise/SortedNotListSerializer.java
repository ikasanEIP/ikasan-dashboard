package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.Not;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Custom JSON serializer for sorting and serializing a list of "Not" objects based on their identifiers.
 */
public class SortedNotListSerializer extends ValueSerializer<List<Not>> {

    @Override
    public void serialize(List<Not> list, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getIdentifier() != null && b.getIdentifier() != null) {
                    return a.getIdentifier().compareTo(b.getIdentifier());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        gen.writeStartArray();
        if (list != null) {
            for (Not not : list) {
                ctxt.writeValue(gen, not);
            }
        }
        gen.writeEndArray();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, List<Not> value) {
        return value == null || value.isEmpty();
    }
}
