FROM maven:3.9.7-eclipse-temurin-21

# 1. Install Shared Lib
WORKDIR /shared
COPY shared/pom.xml .
COPY shared/src ./src
RUN mvn clean install -DskipTests

# 2. Prepare Gateway
WORKDIR /app
COPY api-gateway/pom.xml .
RUN mvn dependency:go-offline -B

CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]