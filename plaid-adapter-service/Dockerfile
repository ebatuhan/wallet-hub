FROM maven:3.9.7-eclipse-temurin-21
WORKDIR /app

COPY pom.xml .

COPY src ./src

ENTRYPOINT ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]
