FROM maven:3.9.7-eclipse-temurin-21

WORKDIR /shared
COPY services/shared/pom.xml .
COPY services/shared/src ./src

WORKDIR /app
COPY services/api-gateway/pom.xml .

CMD ["sh", "-c", "mvn -f /shared/pom.xml install -DskipTests && mvn clean spring-boot:run -Dspring-boot.run.profiles=dev"]
