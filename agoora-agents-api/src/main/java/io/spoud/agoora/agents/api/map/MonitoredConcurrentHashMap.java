package io.spoud.agoora.agents.api.map;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

import java.util.concurrent.ConcurrentHashMap;

public class MonitoredConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

    MeterRegistry registry = Metrics.globalRegistry;

    public MonitoredConcurrentHashMap(String mapName, Class<?> typeUsing){
        this(mapName, typeUsing, Tags.empty());
    }

    public MonitoredConcurrentHashMap(String mapName, Class<?> typeUsing, Tags tags){
        super();
        if(tags == null){
            tags = Tags.empty();
        }
        tags = tags.and("type", typeUsing.getCanonicalName());
        tags = tags.and("name", mapName);
        registry.gaugeMapSize("monitored_map", tags, this);
    }
}
