package org.ikasan.job.orchestration.util.deserialise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import org.ikasan.spec.scheduled.context.model.Context;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;


/**
 * Custom JsonSerializer implementation for serializing a list of Context objects in a sorted manner based on the context name.
 */
public class SortedContextListSerializer extends JsonSerializer<List<Context>> {

    @Override
    public void serialize(List<Context> list, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (list != null) {
            list.sort(Comparator.comparing(Context::getName));
        }
        serializerProvider.defaultSerializeValue(list, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, List<Context> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }


}
