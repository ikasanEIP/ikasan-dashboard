package org.ikasan.job.orchestration.util.serialise;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Custom JSON serializer for sorting and serializing a Map of Long keys and Long values in ascending order.
 */
public class SortedBlackoutWindowDateTimeRangesMapSerializer extends ValueSerializer<Map<Long, Long>> {

    @Override
    public void serialize(Map<Long, Long> map, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        SortedMap<Long, Long> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        if (map != null) {
            sortedMap.putAll(map);
        }

        gen.writeStartObject();
        for (Map.Entry<Long, Long> entry : sortedMap.entrySet()) {
            gen.writeName(String.valueOf(entry.getKey()));
            gen.writeNumber(entry.getValue());
        }
        gen.writeEndObject();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, Map<Long, Long> map) {
        return map == null || map.isEmpty();
    }
}
