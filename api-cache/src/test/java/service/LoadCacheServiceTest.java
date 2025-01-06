package service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.prueba.apiwithcache.ApplicationInit;
import com.prueba.apiwithcache.config.ConfigBeans;
import com.prueba.apiwithcache.model.EventSummary;
import com.prueba.apiwithcache.service.LoadCacheService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.UUID;

import static com.prueba.apiwithcache.config.Constants.DISTRIBUTED_CACHE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationInit.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {ConfigBeans.class, LoadCacheService.class})
public class LoadCacheServiceTest {


    @MockBean
    CloseableHttpClient httpClient;
    @Autowired
    LoadCacheService loadCacheService;

    @Autowired
    HazelcastInstance hazelcastInstance;
    static String xmlResponse = "<eventList xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.0\" xsi:noNamespaceSchemaLocation=\"eventList.xsd\"><output><base_event base_event_id=\"291\" sell_mode=\"online\" title=\"Camela en concierto\"><event event_start_date=\"2021-06-30T21:00:00\" event_end_date=\"2021-06-30T22:00:00\" event_id=\"291\" sell_from=\"2020-07-01T00:00:00\" sell_to=\"2021-06-30T20:00:00\" sold_out=\"false\"><zone zone_id=\"40\" capacity=\"243\" price=\"20.00\" name=\"Platea\" numbered=\"true\"/><zone zone_id=\"38\" capacity=\"100\" price=\"15.00\" name=\"Grada 2\" numbered=\"false\"/><zone zone_id=\"30\" capacity=\"90\" price=\"30.00\" name=\"A28\" numbered=\"true\"/></event></base_event><base_event base_event_id=\"322\" sell_mode=\"online\" organizer_company_id=\"2\" title=\"Pantomima Full\"><event event_start_date=\"2021-02-10T20:00:00\" event_end_date=\"2021-02-10T21:30:00\" event_id=\"1642\" sell_from=\"2021-01-01T00:00:00\" sell_to=\"2021-02-09T19:50:00\" sold_out=\"false\"><zone zone_id=\"311\" capacity=\"2\" price=\"55.00\" name=\"A42\" numbered=\"true\"/></event></base_event><base_event base_event_id=\"1591\" sell_mode=\"online\" organizer_company_id=\"1\" title=\"Los Morancos\"><event event_start_date=\"2021-07-31T20:00:00\" event_end_date=\"2021-07-31T21:00:00\" event_id=\"1642\" sell_from=\"2021-06-26T00:00:00\" sell_to=\"2021-07-31T19:50:00\" sold_out=\"false\"><zone zone_id=\"186\" capacity=\"2\" price=\"75.00\" name=\"Amfiteatre\" numbered=\"true\"/><zone zone_id=\"186\" capacity=\"16\" price=\"65.00\" name=\"Amfiteatre\" numbered=\"false\"/></event></base_event></output></eventList>";

    /**
     * Este método simula la recarga de la caché, que tendrá lugar una vez por minuto. Este test hace una comprobación hard-coded basada en los
     * valores introducidos en xmlResponse. Para hacer este test generalizable a muchos más datos, debería escribirse un código más general. Dado el tiempo limitado del que
     * dispongo, opto por el hard-code para esta prueba de concepto.
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testLoadCache() throws IOException, URISyntaxException {

        //Hacemos todos los mocks necesarios para que la llamada al endpoint del provider nos dé el xml xmlResponse.
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);
        InputStream inputStream = new ByteArrayInputStream(xmlResponse.getBytes());

        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpEntity.getContent()).thenReturn(inputStream);
        Mockito.doNothing().when(response).close();
        Mockito.doNothing().when(httpClient).close();

        //Simulamos una llamada a loadAndCleanCache(), que almacenará los datos del provider en caché Hazelcast. Esta llamada en la realidad será efectuada por el CRON de Spring Boot.
        loadCacheService.loadAndCleanCache();

        //Comprobamos que el resultado obtenido es correcto
        IMap<String, EventSummary> map = hazelcastInstance.getMap(DISTRIBUTED_CACHE);
        assert (map.size() == 3);

        //Comprobamos que las claves para los eventos se corresponden con los uuid generados a partir del ID del evento.
        //Para ello, obtenemos los UUID a partir de los identificadores únicos.
        UUID uuid1 = UUID.nameUUIDFromBytes("1591".getBytes());
        UUID uuid2 = UUID.nameUUIDFromBytes("291".getBytes());
        UUID uuid3 = UUID.nameUUIDFromBytes("322".getBytes());

        EventSummary ev1 = map.get(uuid1.toString());
        EventSummary ev2 = map.get(uuid2.toString());
        EventSummary ev3 = map.get(uuid3.toString());

        assertEquals(uuid1, UUID.fromString(ev1.getId()));
        assertEquals(uuid2, UUID.fromString(ev2.getId()));
        assertEquals(uuid3, UUID.fromString(ev3.getId()));


        assertEquals("Los Morancos", ev1.getTitle());
        assertEquals("Camela en concierto", ev2.getTitle());
        assertEquals("Pantomima Full", ev3.getTitle());

        assertEquals("2021-07-31", ev1.getStartDate());
        assertEquals("2021-06-30", ev2.getStartDate());
        assertEquals("2021-02-10", ev3.getStartDate());

        assertEquals("2021-07-31", ev1.getEndDate());
        assertEquals("2021-06-30", ev2.getEndDate());
        assertEquals("2021-02-10", ev3.getEndDate());


        assertEquals("20:00:00", ev1.getStartTime());
        assertEquals("21:00:00", ev2.getStartTime());
        assertEquals("20:00:00", ev3.getStartTime());


        assertEquals("21:00:00", ev1.getEndTime());
        assertEquals("22:00:00", ev2.getEndTime());
        assertEquals("21:30:00", ev3.getEndTime());


        assertEquals((float) 65.0, ev1.getMinPrice(), 0.0001);
        assertEquals((float) 15.0, ev2.getMinPrice(), 0.0001);
        assertEquals((float) 55.0, ev3.getMinPrice(), 0.0001);

        assertEquals((float) 75.0, ev1.getMaxPrice(), 0.0001);
        assertEquals((float) 30.0, ev2.getMaxPrice(), 0.0001);
        assertEquals((float) 55.0, ev3.getMaxPrice(), 0.0001);

    }
}
