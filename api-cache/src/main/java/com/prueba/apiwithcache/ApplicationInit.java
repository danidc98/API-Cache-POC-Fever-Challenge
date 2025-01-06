package com.prueba.apiwithcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.LocalDate;


@SpringBootApplication
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class ApplicationInit {

    private static final Logger log = LoggerFactory.getLogger(ApplicationInit.class);

    /**
     * Main method
     *
     * @param args
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws  IOException {

        SpringApplication app = new SpringApplication(ApplicationInit.class);
        Environment env = app.run(args).getEnvironment();
        log.info("\n------------------------------------------------------------------------------------------------------------------------------------------\n\t" +
                        "Application: \t{}\n\t" +
                        "Platform: \t\t{} \n\t" +
                        "------------------------------------------------------------------------------------------------------------------------------------------",
                "FEVER API CHALLENGE",
                "STANDALONE-MODE")
               ;
    }
}
