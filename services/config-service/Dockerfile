FROM maven:3.9.7-eclipse-temurin-21

WORKDIR /app
COPY services/config-service/pom.xml .

CMD ["sh", "-c", "mvn clean spring-boot:run -Dspring-boot.run.profiles=dev,native"]
