package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Custom JSON serializer for sorting and serializing a List of String in ascending order.
 */
public class SortedStringListSerializer extends ValueSerializer<List<String>> {

    @Override
    public void serialize(List<String> value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value != null && !value.isEmpty()) {
            Collections.sort(value);
            gen.writeStartArray();
            for (String str : value) {
                gen.writeString(str);
            }
            gen.writeEndArray();
        }
        else {
            gen.writeStartArray();
            gen.writeEndArray();
        }
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, List<String> value) {
        return value == null || value.isEmpty();
    }
}
