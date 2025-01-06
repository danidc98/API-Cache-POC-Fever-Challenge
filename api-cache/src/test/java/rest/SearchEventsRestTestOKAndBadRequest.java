package rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.prueba.apiwithcache.ApplicationInit;
import com.prueba.apiwithcache.model.EventSummary;
import com.prueba.apiwithcache.rest.SearchEventsRest;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.prueba.apiwithcache.config.Constants.DISTRIBUTED_CACHE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationInit.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchEventsRestTestOKAndBadRequest {


    @Autowired
    private MockMvc rest;

    @Autowired
    private SearchEventsRest searchEventsRest;


    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    SimpleDateFormat sdf;

    /**
     * Este test debe reproducir la situación en que hay tres eventos almacenados en la caché y
     * un cliente realiza una consulta de eventos cuyos finales se hallan comprendidos entre
     * 2021-07-31T20:00:00 y 2021-07-31T20:00:00. Debe devolver únicamente el evento de los Morancos
     * siguiendo el formato exigido en https://app.swaggerhub.com/apis-docs/luis-pintado-feverup/backend-test/1.0.0#/default/searchEvents.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnOKAfterReturninTheEventsFromCache() throws Exception {

        String startDate = "2021-07-31T20:00:00";
        String endDate = "2022-06-30T21:00:00";

        IMap<String, EventSummary> distributedCache = hazelcastInstance.getMap(DISTRIBUTED_CACHE);

        //Evento con fecha de inicio 2021-07-31T20:00:00 y id 1591
        String firstKey = "1591";
        EventSummary firstEvent = new EventSummary.Builder("1591", "Los Morancos")
                .withStartDate("2021-07-31")
                .withStartTime("20:00:00")
                .withEndDate("2021-07-31")
                .withEndTime("21:00:00")
                .withMinPrice((float) 65.00)
                .withMaxPrice((float) 75.00)
                .withEndTimestamp(sdf.parse("2021-07-31T21:00:00").getTime())
                .build();

        distributedCache.put(firstKey, firstEvent);


        String secondKey = "322";
        EventSummary secondEvent = new EventSummary.Builder("322", "Pantomima Full")
                .withStartDate("2021-02-10")
                .withStartTime("20:00:00")
                .withEndDate("2021-02-10")
                .withEndTime("21:30:00")
                .withMinPrice((float) 55.00)
                .withMaxPrice((float) 55.00)
                .withEndTimestamp(sdf.parse("2021-02-10T21:30:00").getTime())
                .build();

        distributedCache.put(secondKey, secondEvent);


        //Evento con fecha de inicio 2021-07-31T20:00:00 y id 1591
        String thirdKey = "291";
        EventSummary thirdEvent = new EventSummary.Builder("291", "Camela en concierto")
                .withStartDate("2021-06-30")
                .withStartTime("21:00:00")
                .withEndDate("2021-06-30")
                .withEndTime("22:00:00")
                .withMinPrice((float) 15.00)
                .withMaxPrice((float) 30.00)
                .withEndTimestamp(sdf.parse("2021-06-30T22:00:00").getTime())
                .build();

        distributedCache.put(thirdKey, thirdEvent);


        //Una vez añadidos los elementos a la caché, construimos una respuesta JSON para nuestro REST con el
        // evento que esperamos a la salida, que es el de los Morancos.
        ArrayList<EventSummary> expectedEvents = new ArrayList<EventSummary>();
        expectedEvents.add(firstEvent);
        JSONObject eventsWrapper = new JSONObject().put("events", expectedEvents);
        Map<String, Object> dataObject = new HashMap<>();
        Map<String, Object> eventsObject = new HashMap<>();
        eventsObject.put("events", expectedEvents);
        dataObject.put("data", eventsObject);


        //Llamada REST junto con los debidos asserts.
        rest.perform(get("/search")
                        .param("starts_at", startDate)
                        .param("ends_at", endDate))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(dataObject)));


    }


    /**
     * Este test debe reproducir la situación en que el cliente introduce mal starts_at y ends_at, es decir,
     * va con un formato inadecuado. LA respuesta esperada es un bad request con un mensaje de error y un código de error
     * según el formato especificado en https://app.swaggerhub.com/apis-docs/luis-pintado-feverup/backend-test/1.0.0#/default/searchEvents.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnBadRequest() throws Exception {

        String startDate = "asdsadas";
        String endDate = "asdasd";

        //Construimos el objeto de error esperado
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("message", "The parameters starts_at and ends_at must satisfy the format 2017-07-21T17:32:28Z and the former must be lower than the latter.");
        error.put("code", "400");
        body.put("error", error);
        //Llamada REST junto con los debidos asserts.
        rest.perform(get("/search")
                        .param("starts_at", startDate)
                        .param("ends_at", endDate))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(body)));
    }

}
