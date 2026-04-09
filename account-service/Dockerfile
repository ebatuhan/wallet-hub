FROM maven:3.9.7-eclipse-temurin-21

WORKDIR /shared
COPY shared/pom.xml .
COPY shared/src ./src

WORKDIR /app
COPY account-service/pom.xml .

CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]