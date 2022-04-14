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
* Prepare Docker environment-see the Prerequisites section above...
* Pull this github into a directory
```bash
git clone https://github.com/jphaugla/redisResilience
```
* Open terminal and change to the github home where you will see the docker-compose.yml file
  * If docker resources are limited, comment out nodes re2 and re4 in the docker compose file.
```bash
docker-compose up -d
```
* Setup the redis enterprise cluster and create the active/active database
  * if docker resources are limited, comment out the lines for nodes re2 and re4
```bash
./setup-2AA.sh
./crdcreate.sh
```
* bringing up the docker compose will attempt to start the java container called bankapp.  However, this will fail because the database was not yet created.
* restart the bankapp and it should work now that cluster is created and database is created
```bash
docker-compose start bankapp
```
## To execute the code from command line
(Alternatively, this can be run through intelli4j)

* Compile the code
```bash
mvn package
```
* set the environment variables to match those in docker-compose 
* run the jar file.   
```bash
java -jar target/redis-0.0.1-SNAPSHOT.jar
```
* Note:  parameters for the circuit breaker and retry are in the application.yml.  For the circuit breaker, the code reads all the default values such as:  resilience4j.circuitbreaker.configs.default.failureRateThreshold.  However, the waitDurationInOpenState has an odd error so added waitDurationInOpenStateInt.  
* Once the solution is running do a test write
```bash
cd scripts
./saveCustomer.sh 
Done%                                                
```
## Test Connection Loop
* Start the connection loop test running with this API call script
```bash
./startConnectionLoop.sh
```
* Now do each of these relatively quickly so, the failover completes before the retry is expired on the write. 
```bash
docker stop re1
./saveCustomer.sh
```
NOTE:   Because redis1 stopped, the circuit breaker will kick in on the failure of the write to redis in the connection loop.  This will call the circuit breaker to go to its callback routine.  This callback routine will do a failover once the circuit break opens.  At the same time, the client write will retry until successful or maximum retries occur
* re-start redis1
```bash
docker start re1
```
* switch back is a manual process
```bash
./switchRedis.sh
```
## Test password rotation
* Password rotation tests the ability in an active/active deployment to rotate passwords ensure the same password is used on all instances.
* To ensure all instances have received the password change, a Redis Streams listener is running on each crdb instance.
* When a password change is made, the username, the password, and the timestamp is added to a Redis Stream for the username.  Each username will have its own Redis Stream.
* When each CRDB instance listener receives the Redis Streams message, it increments a sorted set score for the paasword member.
* For the getPassword API call, the password score must be the same as the number of crdb instances.  So, if there are 5 instances the password score must be 5 to ensure all 5 CRDB instances have the password value
* In case there are multiple passwords with the optimal score, only the most recently updated value is returned using the Redis Stream
* Cleanup API will remove old passwords
