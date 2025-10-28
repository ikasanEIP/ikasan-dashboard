package org.ikasan.job.orchestration.util.deserialise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Custom JSON serializer for sorting and serializing a Map of Long keys and Long values in ascending order.
 */
public class SortedBlackoutWindowDateTimeRangesMapSerializer extends JsonSerializer<Map<Long, Long>> {

    @Override
    public void serialize(Map<Long, Long> map, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        SortedMap sortedMap = new TreeMap(Comparator.naturalOrder());
        sortedMap.putAll(map);
        serializerProvider.defaultSerializeValue(sortedMap, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Map<Long, Long> map) {
        if (map == null || map.isEmpty()) {
            return true;
        }
        return false;
    }
}
