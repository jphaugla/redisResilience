FROM maven:3.8.4-openjdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:11
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && \
    apt-get install -y gettext
COPY --from=build /usr/src/app/target/redis-0.0.1-SNAPSHOT.jar /usr/app/redis-0.0.1-SNAPSHOT.jar
COPY --from=build /usr/src/app/src/main/resources/runApplication.sh /usr/app/runApplication.sh
EXPOSE 8080
ENTRYPOINT ["/usr/app/runApplication.sh"]
