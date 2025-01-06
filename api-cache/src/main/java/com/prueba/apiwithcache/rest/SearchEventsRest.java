package com.prueba.apiwithcache.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.prueba.apiwithcache.model.EventSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


import org.apache.http.impl.client.CloseableHttpClient;

import static com.prueba.apiwithcache.config.Constants.DISTRIBUTED_CACHE;
import static com.prueba.apiwithcache.config.Constants.TIMESTAMP;

@RestController
@RequestMapping("/")
public class SearchEventsRest {

    Logger logger= LogManager.getLogger(SearchEventsRest.class);

    Environment env;


    SimpleDateFormat sdf;
    HazelcastInstance hazelcastInstance;


    private ObjectMapper objectMapper;
    CloseableHttpClient httpClient;

    /**
     *
     */
    public SearchEventsRest(Environment env, HazelcastInstance hazelcastInstance, CloseableHttpClient httpClient, SimpleDateFormat sdf, ObjectMapper objectMapper) {
        this.env = env;
        this.hazelcastInstance = hazelcastInstance;
        this.httpClient = httpClient;
        this.sdf = sdf;
        this.objectMapper=objectMapper;
    }


    @GetMapping(value = "search")
    public ResponseEntity search(@RequestParam String starts_at, @RequestParam String ends_at) throws ParseException, JsonProcessingException {
        logger.info("Received request with starts_at: {} and ends_at {}", starts_at, ends_at);
        ResponseEntity responseEntity = null;
        HttpHeaders headers = new HttpHeaders();
        Map<String, Object> body = new HashMap<>();
        headers.add("Content-Type", "application/json");
        Long startTimestamp, endTimestamp;

        try {
            startTimestamp = sdf.parse(starts_at).getTime();
            endTimestamp = sdf.parse(ends_at).getTime();
            if (endTimestamp-startTimestamp<0L) throw new Exception();
        }
        catch(Exception e){
            String message= "The parameters starts_at and ends_at must satisfy the format 2017-07-21T17:32:28Z and the former must be lower than the latter.";
            Map<String, Object> error= new HashMap<>();
            error.put("message", message);
            error.put("code", "400");
            body.put("error",error);
            logger.error("Bad request:\n\n"+ e.getMessage()+"\n\n", e);
            return new ResponseEntity(objectMapper.writeValueAsString(body),headers,HttpStatus.BAD_REQUEST);
        }
            //Asumimos en una primera aproximación que el ambos valores serán correctos, es decir, vienen con el formato adecuado
            //y que el valor de starts_at es menor que el de ends_at. En caso de considerarlo, devolveríamos una 400 Bad request si no se cumpliera.
           try {

               hazelcastInstance.getMap(DISTRIBUTED_CACHE);

               //Definimos la consulta a realizar en el cache distribuido
               Predicate<String, EventSummary> predicate = Predicates.between(TIMESTAMP, startTimestamp, endTimestamp);
               IMap<String, EventSummary> cacheMap = hazelcastInstance.getMap(DISTRIBUTED_CACHE);

               //Hacemos la consulta sobre el rango de tiempo solicitado.
               List<EventSummary> events = cacheMap.values(predicate).stream().collect(Collectors.toList());

               Map<String, Object> eventsObject = new HashMap<>();
               eventsObject.put("events", events);
               body.put("data", eventsObject);

               responseEntity = new ResponseEntity(objectMapper.writeValueAsString(body), headers, HttpStatus.OK);
               logger.info("Respuesta ante la request:\n\n"+ responseEntity.getBody().toString()+"\n\n");

           }
           catch(Exception exception){
               Map<String, Object> error = new HashMap<>();
               error.put("code","500");
               error.put("message", "Se ha producido un error en la aplicación: \n\n"+ exception.getMessage()+"\n\n");
               body.put("error",error);
               logger.error("Se ha producido un error en la aplicación: \n\n"+ exception.getMessage()+"\n\n",exception);

               responseEntity = new ResponseEntity(objectMapper.writeValueAsString(body), headers, HttpStatus.INTERNAL_SERVER_ERROR);
           }
            return responseEntity;
        }

    }