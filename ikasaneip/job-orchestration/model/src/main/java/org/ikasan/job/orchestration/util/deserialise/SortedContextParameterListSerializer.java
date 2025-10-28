package org.ikasan.job.orchestration.util.deserialise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Custom JSON serializer for sorting and serializing a list of ContextParameters in ascending order based on name.
 * Extends JsonSerializer to provide custom serialization logic.
 */
public class SortedContextParameterListSerializer extends JsonSerializer<List<ContextParameter>> {

    @Override
    public void serialize(List<ContextParameter> list, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getName() != null && b.getName() != null) {
                    return a.getName().compareTo(b.getName());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        serializerProvider.defaultSerializeValue(list, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, List<ContextParameter> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }
}
