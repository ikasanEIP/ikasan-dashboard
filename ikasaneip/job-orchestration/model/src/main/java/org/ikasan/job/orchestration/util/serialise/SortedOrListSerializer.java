package org.ikasan.job.orchestration.util.serialise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.ikasan.spec.scheduled.context.model.Or;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Custom JSON serializer for sorting and serializing a list of "Or" objects based on their identifiers.
 */
public class SortedOrListSerializer extends JsonSerializer<List<Or>> {

    @Override
    public void serialize(List<Or> list, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getIdentifier() != null && b.getIdentifier() != null) {
                    return a.getIdentifier().compareTo(b.getIdentifier());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        serializerProvider.defaultSerializeValue(list, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, List<Or> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }
}
