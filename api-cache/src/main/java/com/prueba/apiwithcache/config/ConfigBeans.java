package com.prueba.apiwithcache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.AttributeConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.prueba.apiwithcache.hazelcast.TimestampExtractor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

import static com.prueba.apiwithcache.config.Constants.DISTRIBUTED_CACHE;
import static com.prueba.apiwithcache.config.Constants.TIMESTAMP;

@Component
public class ConfigBeans {
    @Bean
    public SimpleDateFormat sdf() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        MapConfig mapConfig = new MapConfig()
                .setName(DISTRIBUTED_CACHE)
                .addAttributeConfig(new AttributeConfig()
                        .setName(TIMESTAMP)
                        .setExtractorClassName(TimestampExtractor.class.getName()));
        config.addMapConfig(mapConfig);
        //Especificamos el extractor del atributo timestamp de nuestra CompositeKey.
        // Inicializa Hazelcast
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }

}


