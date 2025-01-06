package rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.prueba.apiwithcache.ApplicationInit;
import com.prueba.apiwithcache.rest.SearchEventsRest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import static com.prueba.apiwithcache.config.Constants.DISTRIBUTED_CACHE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationInit.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchEventsRestInternalServerError {


    @Autowired
    private MockMvc rest;

    @Autowired
    private SearchEventsRest searchEventsRest;


    @MockBean
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    SimpleDateFormat sdf;


    /**
     * Este test debe reproducir la situación en que el cliente
     * solicita los eventos de una franja de tiempo y el acceso a caché falla por alguna razón.
     * Debe devolver un error Internal Server Error siguiendo el formato exigido en https://app.swaggerhub.com/apis-docs/luis-pintado-feverup/backend-test/1.0.0#/default/searchEvents.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnInternalServerError() throws Exception {

        String startDate = "2021-07-31T20:00:00";
        String endDate = "2022-06-30T21:00:00";
        String errorMessage = "Error accediendo a caché";
        RuntimeException exception = new RuntimeException(errorMessage);
        Mockito.when(hazelcastInstance.getMap(DISTRIBUTED_CACHE)).thenThrow(exception);

        //Construimos el objeto de error esperado
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Se ha producido un error en la aplicación: \n\n"+errorMessage+"\n\n");
        error.put("code", "500");
        body.put("error", error);

        //Llamada REST junto con los debidos asserts.
        rest.perform(get("/search")
                        .param("starts_at", startDate)
                        .param("ends_at", endDate))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(body)));


    }


}
