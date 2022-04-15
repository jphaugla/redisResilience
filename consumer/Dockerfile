FROM maven:3.8.4-openjdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:11
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && \
    apt-get install -y gettext
COPY --from=build /usr/src/app/target/streams-101-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/app/streams-101-1.0-SNAPSHOT-jar-with-dependencies.jar
COPY --from=build /usr/src/app/src/main/resources/runconsumer.sh /usr/app/runconsumer.sh
ENTRYPOINT ["/usr/app/runconsumer.sh"]
