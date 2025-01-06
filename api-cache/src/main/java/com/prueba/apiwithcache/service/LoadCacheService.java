package com.prueba.apiwithcache.service;//package com.santander.scib.observ.api2getafe.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.prueba.apiwithcache.model.EventSummary;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.prueba.apiwithcache.config.Constants.*;


@Component
@EnableScheduling
public class LoadCacheService {
    HazelcastInstance hazelcastInstance;

    Logger logger= LogManager.getLogger(LoadCacheService.class);

    CloseableHttpClient httpClient;
    SimpleDateFormat sdf;
    Environment env;
    String providerUrl;

    public LoadCacheService(Environment env, HazelcastInstance hazelcastInstance, CloseableHttpClient httpClient, SimpleDateFormat sdf) throws ExecutionException, InterruptedException, TimeoutException {
        this.env = env;
        this.providerUrl = env.getProperty("provider.url");
        this.hazelcastInstance = hazelcastInstance;
        this.httpClient = httpClient;
        this.sdf = sdf;

    }


    /**
     *
     */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "30000")
    //We ensure taht there are 5mins between the end of and exec and the beginning of the next.
//    @Profile("!test")
    //We define synchronized method for testing purposes...
    public void loadAndCleanCache() throws URISyntaxException, IOException {
        String providerUrl = env.getProperty("provider.url");
        HttpUriRequest httpGet = RequestBuilder.get()
                .setUri(new URI(providerUrl))
                .build();
        logger.info("Loading cache Hazelcast caché...");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        IMap<String, EventSummary> eventMap = hazelcastInstance.getMap(DISTRIBUTED_CACHE);

        //Primero, borramos los eventos que hayan terminado ya de la caché.
        // Iteramos sobre todas las claves y comparamos el timestamp de la clave (final de evento) con el timestamp actual.
        Long now = LocalDate.now().toEpochDay();
        for (Map.Entry<String, EventSummary> entry : eventMap.entrySet()) {
            EventSummary value = entry.getValue(); // Clave
            if (value.getEndTimestamp() < now) {
                //Borramos los eventos finalizados de la caché. No tiene sentido tenerlos.
                eventMap.remove(entry.getKey());
            }
        }
        //Por un lado, tenemos eventos almacenados en caché que pueden haber expirado y que no serán
        //corregidos por la API
        //Por otro lado, tenemos eventos que puedenestar en sold out


        String xmlString = EntityUtils.toString(response.getEntity());
        Map<String, Object> map = xmlMapper.readValue(xmlString, Map.class);
        Map<String, Object> output = (Map<String, Object>) map.get(OUTPUT);

        var baseEvents = (List<Map<String, Object>>) output.get(BASE_EVENT);

        //Obtenemos los eventos que se venden online y calculamos los precios mínimo y máximo de los tickets.
        baseEvents.stream()
                .filter(baseEvent -> ((String) baseEvent.get(SELL_MODE)).equals(ONLINE)) //Filtramos por sell_mode. Los eventos que llegan serán los disponibles,
                //por lo que no hace falta hacer más filtrados.
                .forEach(baseEvent -> {
                    Map<String, Object> event = (Map<String, Object>) baseEvent.get(EVENT);
                    float minPrice = (float) -1.0;
                    float maxPrice = (float) -1.0;
                    var zones = event.get(ZONE);
                    //Se recibe zone como un Map o como lista de Maps, según sea una zona o varias.
                    if (zones instanceof Map) {
                        Map<String, Object> zone = (Map<String, Object>) zones;
                        float price = Float.parseFloat((String) zone.get(PRICE));
                        minPrice = price;
                        maxPrice = price;
                    } else {
                        for (Map<String, Object> zone : (List<Map<String, Object>>) event.get(ZONE)) {
                            float price = Float.parseFloat((String) zone.get(PRICE));
                            if (minPrice == -1.0 || price < minPrice) {
                                minPrice = price;
                            }
                            if (maxPrice == -1.0 || price > maxPrice) {
                                maxPrice = price;
                            }
                        }
                    }




                    UUID eventId = UUID.nameUUIDFromBytes(((String) baseEvent.get(BASE_EVENT_ID)).getBytes(StandardCharsets.UTF_8));
                    String eventStartDate = (String) event.get(EVENT_START_DATE);
                    String eventEndDate = (String) event.get(EVENT_END_DATE);
                    String soldOut = (String) event.get(SOLD_OUT);
                    try {
                        //Si las entradas se han vendido ya , borramos el evento de la caché.
                        if (soldOut == "true") {
                            eventMap.delete( eventId.toString());
                        } else {
                            EventSummary eventSummary = new EventSummary.Builder(eventId.toString(), (String) baseEvent.get(TITLE))
                                    .withStartDate(eventStartDate.split("T")[0])
                                    .withEndDate(eventEndDate.split("T")[0])
                                    .withMinPrice(minPrice)
                                    .withMaxPrice(maxPrice)
                                    .withStartTime(eventStartDate.split("T")[1])
                                    .withEndTime(eventEndDate.split("T")[1])
                                    .withEndTimestamp(sdf.parse(eventEndDate).getTime())
                                    .build();
                            //Puede que el evento exista ya en la caché, pero también puede que haya cambiado el precio.
                            eventMap.put(eventId.toString(), eventSummary);
                        }
                    }
                    catch(ParseException e){
                            throw new RuntimeException(e);
                        }
                    });
                }

    }


