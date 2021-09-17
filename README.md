# RedisResilience
This code is derived from Digital Banking github using redistemplate. The focus in this github is on failover and retry when a redis database is not available.  This would allow a client to failover from one active/active redis instance to another.
<a href="" rel="Digital Banking"><img src="images/DigitalBanking.png" alt="" /></a>


## Overview
In this tutorial, a java spring boot application is run through a jar file to support typical API calls to a REDIS banking data layer.  A redis docker configuration is included with 2 separate redis containers running at different ports.  These are simulating two active/active redis dadtabases.  Each database can be easily stopped/started with docker.


## Requirements
* Docker installed on your local system, see [Docker Installation Instructions](https://docs.docker.com/engine/installation/).
* Alternatively, can run Redis Enterprise and set the redis host and port in the application.yml file
* When using Docker for Mac or Docker for Windows, the default resources allocated to the linux VM running docker are 2GB RAM and 2 CPU's. Make sure to adjust these resources to meet the resource requirements for the containers you will be running. More information can be found here on adjusting the resources allocated to docker.

[Docker for mac](https://docs.docker.com/docker-for-mac/#advanced)
[Docker for windows](https://docs.docker.com/docker-for-windows/#advanced)

## Links that help!

 * [spring data for redis github](https://github.com/spring-projects/spring-data-examples/tree/master/redis/repositories)
 * [spring data for redis sample code](https://www.oodlestechnologies.com/blogs/Using-Redis-with-CrudRepository-in-Spring-Boot/)
 * [lettuce tips redis spring boot](https://www.bytepitch.com/blog/redis-integration-spring-boot/)
 * [spring data Reference in domain](https://github.com/spring-projects/spring-data-examples/blob/master/redis/repositories/src/main/java/example/springdata/redis/repositories/Person.java)
 * [spring data reference test code](https://github.com/spring-projects/spring-data-examples/blob/master/redis/repositories/src/test/java/example/springdata/redis/repositories/PersonRepositoryTests.java)
 * [Resilience4j quickguide](https://www.baeldung.com/resilience4j)
 * [Resilience4j documentation](https://resilience4j.readme.io/docs/getting-started-3)
 * [Redis Retry Client](https://gitlab.com/deji.alaran/redis-java-clients/-/blob/master/src/main/java/com/redislabs/examples/redis/service/RedisClientsService.java)
 * [Workaround for waitDurationInOpenState issue](https://stackoverflow.com/questions/65909665/circuitbreaker-not-loading-defaults-from-yaml-file)

## Technical Overview

This github uses resilience4j circuit breaker and retry mechanisms to make a more resilient client for Spring data redistemplate.  A resilience4j circuit breaker is used in a continual loop to do the failover logic while a resilience4j retry is used on client writes to get retry capability.  The failover decision is controlled by the circuit breaker callback method.

### The spring java code
This is basic spring links
* *config*-Initial configuration module using autoconfiguration, redistemplate connections, and hard coded resilience4j circuit breaker needed due to a bug.  Should be able to define the circuit breaker with application.yml but odd bug with waitDurationInOpenState at 7.1.
* *controller*-http API call interfaces
* *data*-code to generate POC type of customer, account, and transaction code
* *domain*-has each of the java objects with their columns.  Enables all the getter/setter methods
* *repository*-has repository definitions
* *service*- bankservice doing the interaction with redis
### 
The java code demonstrates common API actions with the data layer in REDIS.  The java spring Boot framework minimizes the amount of code to build and maintain this solution.  Maven is used to build the java code and the code is deployed to the tomcat server.

### Data Structures in use
<a href="" rel="Tables Structures Used"><img src="images/Tables.png" alt="" /></a>

## Getting Started
1. Prepare Docker environment-see the Prerequisites section above...
2. Pull this github into a directory
```bash
git clone https://github.com/jphaugla/redisResilience
```
3. Refer to the notes for redis Docker images used but don't get too bogged down as docker compose handles everything except for a few admin steps on tomcat.
 * [https://hub.docker.com/r/bitnami/redis/](https://hub.docker.com/r/bitnami/redis/)  
4. Open terminal and change to the github home where you will see the docker-compose.yml file, then: 
```bash
docker-compose up -d
```

## To execute the code
(Alternatively, this can be run through intelli4j)

1. Compile the code
```bash
mvn package
```
2.  run the jar file.   
```bash
java -jar target/redis-0.0.1-SNAPSHOT.jar
```
3. Note:  parameters for the circuit breaker and retry are in the application.yml.  For the circuit breaker, the code reads all the default values such as:  resilience4j.circuitbreaker.configs.default.failureRateThreshold.  However, the waitDurationInOpenState has an odd error so added waitDurationInOpenStateInt.  
4. Once the solution is running do a test write
```bash
cd scripts
./saveCustomer.sh 
Done%                                                
```
5. Start the connection loop test running with this API call script
```bash
./startConnectionLoop.sh
```
6. Now do each of these relatively quickly so, the failover completes before the retry is expired on the write. 
```bash
docker stop redis1
./saveCustomer.sh
```
NOTE:   Because redis1 stopped, the circuit breaker will kick in on the failure of the write to redis in the connection loop.  This will call the circuit breaker to go to its callback routine.  This callback routine will do a failover once the circuit break opens.  At the same time, the client write will retry until successful or maximum retries occur
7. re-start redis1
```bash
docker start redis1
```
8. switch back is a manual process
```bash
./switchRedis.sh
```