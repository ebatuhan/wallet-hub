FROM maven:3.9.7-eclipse-temurin-21

WORKDIR /shared
COPY shared/pom.xml .
COPY shared/src ./src
RUN mvn clean install -DskipTests

WORKDIR /app
COPY transaction-service/pom.xml .
RUN mvn dependency:go-offline -B

CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.fork=false", "-Dspring-boot.run.profiles=dev"]
