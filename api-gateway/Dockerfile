FROM maven:3.9.7-eclipse-temurin-21

WORKDIR /app

# 1. Copy only the POM first to cache dependencies
COPY api-gateway/pom.xml .

# 2. Download dependencies. 
# REMOVED --mount=type=cache so dependencies are actually saved to the image filesystem
RUN mvn dependency:go-offline -B

# 3. Copy source code
COPY api-gateway/src ./src

# 4. Build the JAR inside the image
RUN mvn clean package -DskipTests

# 5. Run the built JAR (No Maven downloading at runtime!)
CMD ["java", "-jar", "target/api-gateway-0.0.1-SNAPSHOT.jar"]