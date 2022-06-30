# RedisResilience
This code is derived from Digital Banking github using redistemplate. The focus in this github is on failover and retry when a redis database is not available.  This would allow a client to failover from one active/active redis instance to another.
<a href="" rel="Digital Banking"><img src="images/DigitalBanking.png" alt="" /></a>

## Outline

- [Overview](#overview)
- [Important Links](#important-linksnotes)
- [Instructions](#instructions)
  - [Create Environment](#create-environment)
  - [Multiple Deployment options](#multiple-options-for-creating-the-environment)
  - [Docker Compose](#docker-compose-startup)
    - [Setup Redis](#setup-redis-enterprise-cluster-and-db)
  - [Kubernetes](#kubernetes)
    - [Install Redis Enterprise](#install-redis-enterprise-k8s)
    - [Add Redisinsights](#add-redisinsights)
    - [Deploy application](#deploy-redis-resilience-on-kubernetes)
  - [Run Java on Unix](#run-local-java)
  - [Use the Application](#use-the-application)
    - [Test Connection Loop](#test-connection-loop)
  - [Test Password Rotation](#test-password-rotation)
  - [Documentation on Failover Logic](#documentation-on-fail-over-code)
- [Cleaning up](#cleaning-up)


## Overview
In this tutorial, a java spring boot application is run through a jar file to support typical API calls to a REDIS banking data layer.  A redis docker configuration is included with 2 separate redis containers running at different ports.  These are simulating two active/active redis dadtabases.  Each database can be easily stopped/started with docker.  Alternatively, k8s can be used to run active/active redisenterprise clusters demmonstrating the full capability
Finally, a distributed active/active key writing solution is also provided.

This github uses resilience4j circuit breaker and retry mechanisms to make a more resilient client for Spring data redistemplate.  A resilience4j circuit breaker is used in a continual loop to do the failover logic while a resilience4j retry is used on client writes to get retry capability.  The failover decision is controlled by the circuit breaker callback method.



## Requirements
* Docker installed on your local system, see [Docker Installation Instructions](https://docs.docker.com/engine/installation/).
* Alternatively, can run Redis Enterprise and set the redis host and port in the application.yml file
* When using Docker for Mac or Docker for Windows, the default resources allocated to the linux VM running docker are 2GB RAM and 2 CPU's. Make sure to adjust these resources to meet the resource requirements for the containers you will be running. More information can be found here on adjusting the resources allocated to docker.

[Docker for mac](https://docs.docker.com/docker-for-mac/#advanced)
[Docker for windows](https://docs.docker.com/docker-for-windows/#advanced)

Or, can use kubernetes.  GKE example is provided.

## Important Links/Notes

 * [spring data for redis github](https://github.com/spring-projects/spring-data-examples/tree/master/redis/repositories)
 * [spring data for redis sample code](https://www.oodlestechnologies.com/blogs/Using-Redis-with-CrudRepository-in-Spring-Boot/)
 * [lettuce tips redis spring boot](https://www.bytepitch.com/blog/redis-integration-spring-boot/)
 * [spring data Reference in domain](https://github.com/spring-projects/spring-data-examples/blob/master/redis/repositories/src/main/java/example/springdata/redis/repositories/Person.java)
 * [spring data reference test code](https://github.com/spring-projects/spring-data-examples/blob/master/redis/repositories/src/test/java/example/springdata/redis/repositories/PersonRepositoryTests.java)
 * [Resilience4j quickguide](https://www.baeldung.com/resilience4j)
 * [Resilience4j documentation](https://resilience4j.readme.io/docs/getting-started-3)
 * [Redis Retry Client](https://gitlab.com/deji.alaran/redis-java-clients/-/blob/master/src/main/java/com/redislabs/examples/redis/service/RedisClientsService.java)
 * [Workaround for waitDurationInOpenState issue](https://stackoverflow.com/questions/65909665/circuitbreaker-not-loading-defaults-from-yaml-file)
 * [k8s persistent volume claim](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#claims-as-volumes)
 * [GKE persistent volume claims](https://cloud.google.com/kubernetes-engine/docs/concepts/persistent-volumes)
 
### The spring java code
This is basic spring links
* *config*-Initial configuration module using autoconfiguration, redistemplate connections, and hard coded resilience4j circuit breaker needed due to a bug.  Should be able to define the circuit breaker with application.yml but odd bug with waitDurationInOpenState at 7.1.
* *controller*-http API call interfaces
* *data*-code to generate POC type of customer, account, and transaction code
* *domain*-has each of the java objects with their columns.  Enables all the getter/setter methods
* *repository*-has repository definitions
* *service*- bankservice doing the interaction with redis

The java code demonstrates common API actions with the data layer in REDIS.  The java spring Boot framework minimizes the amount of code to build and maintain this solution.  Maven is used to build the java code and the code is deployed to the tomcat server.

### Data Structures in use
<a href="" rel="Tables Structures Used"><img src="images/Tables.png" alt="" /></a>

## Instructions
* Prepare Docker environment-see the Pre-requisites section above.
### Create Environment
* Pull this github into a directory
```bash
git clone https://github.com/jphaugla/redisResilience
```

### Multiple options for creating the environment:
* run with docker-compose using a flask and redis container
* installing for mac os
* running on linux (probably in the cloud)
* running on kubernetes (example uses GKE)

### Docker Compose
* Open terminal and change to the github home where you will see the docker-compose.yml file
  * If docker resources are limited, comment out nodes re2 and re4 in the docker compose file.
```bash
docker-compose build
docker-compose up -d
```
### Setup Redis Enterprise and DB
NOTE:   There is a second docker-compose-consumer.yml file .   I was not able to get this to work to run the consumer under the same docker-compose setup.   Not sure if it is a resource issue on my mac or a network issue with the second yaml file.  For now, the consumer needs to be run outside of docker
* Setup the redis enterprise cluster and create the active/active database
  * if docker resources are limited, comment out the lines for nodes re2 and re4
```bash
./setup-2AA.sh
./crdcreate.sh
```
* bringing up the docker compose will attempt to start the java container called resilience.  However, this may fail because the database was not yet created.
* restart the bankapp and it should work now that cluster is created and database is created
```bash
docker-compose start resilience
```
### Kubernetes
This example is showing GKE steps-adjust accordingly for other versions
#### Install Redis Enterprise k8s
* Get to K8 directory
```bash
cd k8s
```
* Follow [Redis Enterprise k8s installation instructions](https://github.com/RedisLabs/redis-enterprise-k8s-docs#installation) all the way through to step 4.  Use the demo namespace as instructed.
* The admission controller is not needed for this demo so don't do step 5
* Don't do Step 6 as the databases for this github are in the k8s subdirectory of this github
### Create database
* Create redis enterprise database.
```bash
kubectl apply -f redis-enterprise-database.yml
```
* Try cluster username and password script as well as databases password and port information scripts.
```bash
./getClusterUnPw.sh
```
#### Install second RedisEnterprise k8s
* Get to K8 directory
* Ensure still in namespace demo2
```bash
cd k8s
```
* Follow [Redis Enterprise k8s installation instructions](https://github.com/RedisLabs/redis-enterprise-k8s-docs#installation) all the way through to step 4.  However, change the namespace to demo2 for all steps.
* The admission controller is not needed for this demo so don't do step 5
* Don't do Step 6 as the databases for this github are in the k8s subdirectory of this github
* Create redis enterprise database.

```bash
kubectl apply -f redis-enterprise-database2.yml
```
* Try cluster username and password script as well as databases password and port information scripts.
```bash
./getClusterUnPw2.sh
```

#### Add redisinsights
These instructions are based on [Install RedisInsights on k8s](https://docs.redis.com/latest/ri/installing/install-k8s/)
&nbsp;
The above instructions have two options for installing redisinights, this uses the second option to install[ without a service](https://docs.redis.com/latest/ri/installing/install-k8s/#create-the-redisinsight-deployment-without-a-service) (avoids creating a load balancer)
* switch back to demo namespace
* create redisinsights
```bash
kubectl config set-context --current --namespace=demo 
kubectl apply -f redisinsight.yaml
kubectl port-forward deployment/redisinsight 8001
```
* from chrome or firefox open the browser using http://localhost:8001
* Click "I already have a database"
* Click "Connect to Redis Database"
* Create Connection to target redis database with following parameter entries

| Key      | Value                                     |
|----------|-------------------------------------------|
| host     | redis-enterprise-database.demo            |
| port     | 18154 (get from ./getDatabasepw.sh above) |
| name     | TargetDB                                  |
| Username | (leave blank)                             |
| Password | DrCh7J31 (from ./getDatabasepw.sh above) |
* click ok

Also connect to the secondary database
* click on redisinsight logo on the top left
* Click on "ADD REDIS DATABASE"

| Key      | Value                                     |
|----------|-------------------------------------------|
| host     | redis-enterprise-database2.demo2          |
| port     | 18154 (get from ./getDatabasepw.sh above) |
| name     | TargetDB                                  |
| Username | (leave blank)                             |
| Password | DrCh7J31 (from ./getDatabasepw.sh above)  |
#### Deploy redis-resilience on Kubernetes

* must [log into docker](https://docs.docker.com/engine/reference/commandline/login/) to have access to the docker image
```bash
docker login
```
* modify, create the environmental variables by editing configmap.yml
  * can find the IP addresses and ports for each of the databases by running ```kubectl get services```
  * In the example below the IP address for the REDIS_HOST in the configmap.yaml is *10.28.16.188*
    ![services](src/static/k8sgetservices.png)
  * get the database password by running ```getDatabasePw```.  Put the returned password the configmap REDIS_PASSWORD
* apply the configuration map
```bash
cd k8s
kubectl apply -f configmap.yaml
```
* deploy the redis-resilience
```bash
kubectl apply -f resilience.yaml
```
* port forward and continue with testing of the APIs
  * NOTE:  get exact name use ```kubectl get pods```
```bash
kubectl port-forward redis-resilience-c568d9b6b-z2mnf 5000
```

## Run local Java
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
## Use the Application
```bash
cd scripts
./saveCustomer.sh 
Done%                                                
```
### Test Connection Loop
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
## Password rotation
* Password rotation tests the ability in an active/active deployment to rotate passwords ensure the same password is used on all instances.
* To ensure all instances have received the password change, a Redis Streams listener is running on each crdb instance.
* When a password change is made, the username, the password, and the timestamp is added to a Redis Stream for the username.  Each username will have its own Redis Stream.
* When each CRDB instance listener receives the Redis Streams message, it increments a sorted set score for the paasword member.
* For the getPassword API call, the password score must be the same as the number of crdb instances.  So, if there are 5 instances the password score must be 5 to ensure all 5 CRDB instances have the password value
* In case there are multiple passwords with the optimal score, only the most recently updated value is returned using the Redis Stream
* Cleanup API will remove old passwords

### Test Password Rotation
* In addition to the normal startup tasks, a consumer needs to be started consuming off the stream in each of the CRDB instances.
```bash
export REDIS_URL=redis://localhost:12000
#  user must match the user in password.json in subsequent step
export STREAMS_KEY=STREAM:USER:ralph
cd consumer
mvn package 
./runconsumer.sh
```
```bash
export REDIS_URL=redis://localhost:12001
#  user must match the user in password.json in subsequent step
export STREAMS_KEY=STREAM:USER:ralph
cd consumer
mvn package
./runconsumer.sh
```
* create a new password
```bash
cd ../scripts
./putPassword.sh
./getPassword.sh
```
* edit the ./scripts/password.json to add a new password
* create the new password and now should get the newerPassword on the get.   Check the logs to see what is happening
```bash
./putPassword.sh
./getPassword.sh
```
* now try the step of adding a third password but take down one of the nodes first
```bash
docker-compose stop re2
./putPassword.sh
#  make sure this script has targetinstance set to the number of instances
./getPassword.sh
```
Should return the second and not the third parameter

## Documentation on Fail-over Code
### Starting the Fail-over loop
* To test the failover code, use the API to [start the failover process](#test-connection-loop) and then [run test failover scenarios](#test-password-rotation)
* The test loop is started by calling the scripts ![scripts/startConnectionLoop.sh](scripts/startConnectionLoop.sh)
  * This shell scripts makes an api call to startConnect
* The api call is handled by the main API controller ![controller/BankingController.java](src/main/java/com/jphaugla/controller/BankingController.java)
  * This api call uses the main service routine startRedisWrite from ![service/BankService.java](src/main/java/com/jphaugla/service/BankService.java)
    * The bank service method is startRedisWrite.  
    * startRedisWrite starts a write test loop using testTheWrite method from ![repository/RedisTemplateRepository.java](src/main/java/com/jphaugla/repository/RedisTemplateRepository.java)
* testTheWrite ![repository/RedisTemplateRepository.java](src/main/java/com/jphaugla/repository/RedisTemplateRepository.java)
  * uses an array element value, redisIndex, to write a test value to the active redis index
  * this array element index is the pointer in to the array of the redis connections
    * In this sample code, only two redis connections are coded.  This should be changed to an application property for number of redis active/active instances
  * the call to testTheWrite is decorated with a resilience4j circuit breaker annotation.
    * This decoration introduces the resilience4j circuit breaker logic-the key is the call back routine.
      * Anytime the write to the active database fails, the callback routine will be called
      * In the callback routine, the exception is checked.  The callback routine will not initiate the failover unless the exception is circuit breaker open
      * If it is circuit breaker open, the redisIndex will be switched to the other database
      * Wait interval will be increased so don't get in infinite failover loop
  * The actual application writes are not part of the circuit breaker logic.  The application code has a resilience4j retry decoration and uses the redisIndex to connect to the appropriate redis instance