FROM maven:3.9.7-eclipse-temurin-21

# 1. Install Shared Lib
WORKDIR /shared
COPY shared/pom.xml .
COPY shared/src ./src
RUN mvn clean install -DskipTests

# 2. Prepare Service
WORKDIR /app
COPY account-service/pom.xml .
RUN mvn dependency:go-offline -B

# 3. Source is mapped via Volume, so we just run
# We assume the volume ./shared is mapped to /shared at runtime
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]