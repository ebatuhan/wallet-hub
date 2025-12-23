# syntax=docker/dockerfile:1
FROM maven:3.9.7-eclipse-temurin-21

WORKDIR /app

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B
COPY src ./src
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]