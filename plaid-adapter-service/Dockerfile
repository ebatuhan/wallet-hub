# syntax=docker/dockerfile:1
FROM maven:3.9.7-eclipse-temurin-21


RUN apt-get update && apt-get install -y curl grep ca-certificates


RUN curl -fsSL https://get.tuna.am | sh

WORKDIR /app

COPY pom.xml .


RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B


COPY entrypoint.sh .
RUN chmod +x entrypoint.sh


ENTRYPOINT ["./entrypoint.sh"]