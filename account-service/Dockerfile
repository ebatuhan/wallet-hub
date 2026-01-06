FROM maven:3.9.7-eclipse-temurin-21
COPY shared /shared
WORKDIR /shared
RUN mvn clean install -DskipTests
WORKDIR /app
COPY account-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY account-service/src ./src
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]