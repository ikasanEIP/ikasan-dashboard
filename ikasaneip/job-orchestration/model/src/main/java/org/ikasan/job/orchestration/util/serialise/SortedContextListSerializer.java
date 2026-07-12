package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Comparator;
import java.util.List;


/**
 * Custom JsonSerializer implementation for serializing a list of Context objects in a sorted manner based on the context name.
 */
public class SortedContextListSerializer extends ValueSerializer<List<Context>> {
    @Override
    public void serialize(List<Context> list, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (list != null) {
            // We set the ordinals on export if necessary
            for(int i=0; i<list.size(); i++) {
                if(list.get(i).getOrdinal() == -1) {
                    list.get(i).setOrdinal(i);
                }
            }

            list.sort(Comparator.comparing(Context::getName));
        }

        gen.writeStartArray();
        if (list != null) {
            for (Context context : list) {
                ctxt.writeValue(gen, context);
            }
        }
        gen.writeEndArray();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, List<Context> value) {
        return value == null || value.isEmpty();
    }
}
