# Fever code challenge

Hello! Glad you are on this step of the process. We would like to see how you are doing while coding and this exercise
tries to be a simplified example of something we do on our daily basis.

At Fever we work to bring experiences to people. We have a marketplace of events from different providers that are
curated and then consumed by multiple applications. We work hard to expand the range of experiences we offer to our customers.
Consequently, we are continuosly looking for new providers with great events to integrate in our platforms. 
In this challenge, you will have to set up a simple integration with one of those providers to offer new events to our users.

Even if this is just a disposable test, imagine when coding that somebody will pick up this code an maintain it on
the future. It will be evolved, adding new features, adapting existent ones, or even removing unnecessary functionalities.
So this should be conceived as a long term project, not just one-off code.

## Evaluation
We will value the solution as a whole, but some points that we must special attention are:
- How the proposed solution matches the given problem.
- Code style.
- Consistency across the codebase.
- Software architecture proposed to solve the problem.
- Documentation about decisions you made.

## Tooling
- Use Python 3 unless something different has been told.
- You can use any library, framework or tool that you think are the best for the job.
- To provide your code, use the master branch of this repository.

## Description
We have an external provider that gives us some events from their company, and we want to integrate them on the Fever
marketplace, in order to do that, we are developing this microservice.

##### External provider service
The provider will have one endpoint:

https://provider.code-challenge.feverup.com/api/events

Where they will give us their list of events on XML. Every time we fetch the events,
the endpoint will give us the current events available on their side. Here we provide some examples of three different
calls to that endpoint on three different consecutive moments.

Response 1
https://gist.githubusercontent.com/sergio-nespral/82879974d30ddbdc47989c34c8b2b5ed/raw/44785ca73a62694583eb3efa0757db3c1e5292b1/response_1.xml

Response 2
https://gist.githubusercontent.com/sergio-nespral/82879974d30ddbdc47989c34c8b2b5ed/raw/44785ca73a62694583eb3efa0757db3c1e5292b1/response_2.xml

Response 3
https://gist.githubusercontent.com/sergio-nespral/82879974d30ddbdc47989c34c8b2b5ed/raw/44785ca73a62694583eb3efa0757db3c1e5292b1/response_3.xml

As you can see, the events that aren't available anymore aren't shown on their API anymore.

##### What we need to develop
Our mission is to develop and expose just one endpoint, and should respect the following Open API spec, with
the formatted and normalized data from the external provider:
https://app.swaggerhub.com/apis-docs/luis-pintado-feverup/backend-test/1.0.0

This endpoint should accept a "starts_at" and "ends_at" param, and return only the events within this time range.
- It should only return the events that were available at some point in the provider's endpoint(the sell mode was online, the rest should be ignored)
- We should be able to request this endpoint and get events from the past (events that came in previous API calls to the provider service since we have the app running) and the future.
- The endpoint should be fast in hundred of ms magnitude order, regardless of the state of other external services. For instance, if the external provider service is down, our search endpoint should still work as usual.

Example: If we deploy our application on 2021-02-01, and we request the events from 2021-02-01 to 2022-07-03, we should
see in our endpoint the events 291, 322 and 1591 with their latest known values. 

## Requirements
- The service should be as resource and time efficient as possible.
- The Open API specification should be respected.
- Use PEP8 guidelines for the formatting
- Add a README file that includes any considerations or important decision you made.
- If able, add a Makefile with a target named `run` that will do everything that is needed to run the application.

## The extra mile
With the mentioned above we can have a pretty solid application. Still we would like to know your opinion, either 
directly coded (if you want to invest the time) or explained on a README file about how to scale this application
to focus on performance. The examples are small for the sake of the test, but imagine that those files contains
thousands of events with hundreds of zones each. Also consider, that this endpoint developed by us, will have peaks
of traffic between 5k/10k request per second.

## Feedback
If you have any questions about the test you can contact us, we will try to reply as soon as possible.

In Fever, we really appreciate your interest and time. We are constantly looking for ways to improve our selection processes,
our code challenges and how we evaluate them. Hence, we would like to ask you to fill the following (very short) form:

https://forms.gle/6NdDApby6p3hHsWp8

Thank you very much for participating!

## Solution
The solution has been developed through a Java application based in a Spring Boot (Version 2.5.6). The application is developed leveraging the features of the Beans(IoC) and Rest Controllers.  As you may notice, it is just a POC, so there are many improvements that can be carried out. I would like to have developed at least some of them, but I haven´t enough time since I am studying a Master´s and also working part-time. Here we will explain some of them, which I would be really happy to implement working with you.

To achieve the functionality of a cache, we set up a Hazelcast instance. This will be an in-memory cache, so the data given by the provider since the application has been started will be stored in this cache. However, if the application fails for some reason and it needs to be restarted, this cache will be lost. However, the solution can be fault tolerant if we define periodical checkpoints in a persistent database
like HDFS or S3 and prepare the application to detect the last checkpoint while starting.

To fill the cache, the provider endpoint will be requested in intervals of 1 minute with a CRON, following the Spring framework standards (https://spring.io/blog/2020/11/10/new-in-spring-5-3-improved-cron-expressions). On the other hand, with a RestController (https://www.baeldung.com/spring-controller-vs-restcontroller](https://spring.io/guides/tutorials/rest) we define the response of our REST API. This controller will check that the requests have the appropiate format and will read the in-memory cache to return the results.


### How data is stored in Hazelcast?
The events are stored in Hazelcast (version 4.1) in a Map where the Keys will be the UUIDs generated with the base_event_id and the Values will follow the schema that is required in the output (EventSummary) but
with an additional field: endTimestamp, that corresponds to the event_date in timestamp format. We prefer to add directly the field in this format in order to avoid the parse in all the requests.
The query that will be performed in the cache will check if the endTimestamp of each event is lower than the timestamp of the date 'ends_at' and greater than the one of 'starts_at'. The
events that fullfill this condition will be returned to the client.

As it can be noticed in the code, the TimestampExtractor class defines how to extract the timestamp from the events stored in the cache.

### Cache updates
The cache updates will be performed in periods of 1 minute. The events in state sold_out=true will be removed from the cache and the events whose end_date has expired (they are lower than current time) will also be deleted from the cache.

### SSL Certificates
The application checks the certificates of the provider against the JKS truststore inside the folder src/main/resources of the project.

### How does the app handle the data format?
The data format is ensured by usage of the Jackson libraries for both XML (input) and JSON (output) format.

### Logging.
For emitting logs, the app uses Log4j2 and writes data in a file in the filesystem of the machine that runs the application. More sophisticated Appenders (custom appenders, with which I have experience in Log4j2, Logback and Log4j) can be developed to keep track of the application behaviour in a persistent way.

### Unit tests
In the tests, we run instances of our application, that is ready to receive requests. We perform some test requests having in cache the events of Pantomima Full, Camela en concierto y los Morancos. 
SearchEventsRestInternalServerError will test the case in which an internal server error occurs, satisfying the required format.
SearchEventsRestTestOKAndBadRequest will test the case in which a client performs a request of events between 2021-07-31 20:00:00 and 2022-06-30 21:30:00. The same class will test the case in which the format of the input parameters are not correct, satisfying the error response schema.
There is another test to prove the functionality of reloading and cleaning expired events from the cache.


### How to scale the solution?
Since Hazelcast can be created as a distributed cache and also the Spring Boot Application can be replicated, this solution is scalable. As I have good experience with Kubernetes, we will explain a possible solution using Kubernetes.

If we deploy this application in Kubernetes in several pods, we can create a load balancer (https://cast.ai/blog/kubernetes-load-balancer-expert-guide-with-examples/) that registers all these pods by means of some labels. With this, all the requests that arrive to the load balancer will be distributed accross the different replicas.
However, the load balancer needs to know which instances are available or not. For achieving this, we need to develop a new endpoint that serves as a health check. This can be done through the Spring Boot Actuator library, with which I have already worked for the same objective.

Besides that, as each application instance will be a node of the distributed cache, we need to know which nodes belong to the same Hazelcast cluster in order to know where to request the cache data from. For this, we need that all the existent nodes in Hazelcast are registered in the same Kubernetes service. In Hazelcast, you can enable some configuration so that all the replicas are considered to belong to the same Hazelcast cluster(https://docs.hazelcast.com/hazelcast/5.5/kubernetes/kubernetes-auto-discovery). In this way, when a request arrives to one of the instances and this instance needs to read data from the cache, the instance will read the data from all the nodes of the Service. 

Of course, the write access to the distributed cache needs to be locked when one of the nodes is already writting. We should change the LoadCacheService.loadAndCleanCache() method in order to lock the access to the cache. In this way, though all the pods will call this method each minute (remember that each pod uses a CRON for this), only the first one that runs the method will be able to write in the cache. This means that only the node with less workload will perform the request to the provider and write the data in the distributed cache.



## How to run the code.
The JAR has been built using Maven 3.8.1 with Java 11.0.21. Running the command

```bash
mvn clean package
```
from the api-cache folder, you should obtain the JAR in the target folder. After that, it will be enough to run the JAR as follows.

```bash
java "-Djavax.net.ssl.trustStore=src/main/resources/certificadoFever.jks" "-Djavax.net.ssl.trustStorePassword=changeit" "-Djavax.net.ssl.truststoreType=jks" -jar .\target\api-cache-1.0-SNAPSHOT.jar 
```

Note that the path of the JAR is given following Windows rules. If this command runs successfully, you will be able to perform requests in localhost:8778/search with params starts_at and ends_at. At the beginning, you will see these logs:

```plaintext
2024-12-13 21:57:09.498  INFO 1012 --- [           main] com.hazelcast.core.LifecycleService      : [180.102.110.106]:5701 [dev] [4.1] [180.102.110.106]:5701 is STARTED
2024-12-13 21:57:10.260  INFO 1012 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8778 (http) with context path ''
2024-12-13 21:57:10.280  INFO 1012 --- [           main] com.prueba.apiwithcache.ApplicationInit  : Started ApplicationInit in 8.003 seconds (JVM running for 8.51)
2024-12-13 21:57:10.285  INFO 1012 --- [           main] com.prueba.apiwithcache.ApplicationInit  :
------------------------------------------------------------------------------------------------------------------------------------------
        Application:    FEVER API CHALLENGE
        Platform:               STANDALONE-MODE
        ------------------------------------------------------------------------------------------------------------------------------------------
```

Whenever you perform a request or the cache is reloaded, you will see more logs.

 


