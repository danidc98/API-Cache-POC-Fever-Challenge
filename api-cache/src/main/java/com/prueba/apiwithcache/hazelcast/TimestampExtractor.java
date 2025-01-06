package com.prueba.apiwithcache.hazelcast;

import com.hazelcast.query.extractor.ValueCollector;
import com.hazelcast.query.extractor.ValueExtractor;
import com.prueba.apiwithcache.model.EventSummary;


/**Con esta clase definimos cómo se extrae el campo timestamp de los eventos almacenados en cache.
 *
 */
public class TimestampExtractor implements ValueExtractor<EventSummary, Long> {

    @Override
    public void extract(EventSummary eventSummary, Long timestamp, ValueCollector valueCollector) {
        valueCollector.addObject(eventSummary.getEndTimestamp());
    }
}
