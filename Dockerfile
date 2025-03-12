#Build stage
FROM maven:3-jdk-11 AS build

RUN useradd -m urlfrontier
USER urlfrontier
WORKDIR /home/urlfrontier

COPY --chown=urlfrontier pom.xml .

COPY --chown=urlfrontier API API 
COPY --chown=urlfrontier client client
COPY --chown=urlfrontier service service
COPY --chown=urlfrontier tests tests

# Build the project
RUN mvn clean package -DskipFormatCode=true && \
    rm service/target/original-*.jar && \
    cp service/target/*.jar urlfrontier-service.jar
	

#Run stage
FROM openjdk:11-jdk-slim

# Create user only once
RUN useradd -m urlfrontier
USER urlfrontier
WORKDIR /home/urlfrontier

COPY --chown=urlfrontier --from=build /home/urlfrontier/urlfrontier-service.jar .

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-XX:+UseG1GC", "-jar", "urlfrontier-service.jar"]
