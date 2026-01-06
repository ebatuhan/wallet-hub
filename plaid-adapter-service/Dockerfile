FROM maven:3.9.7-eclipse-temurin-21

RUN apt-get update && apt-get install -y curl grep ca-certificates
RUN curl -fsSL https://get.tuna.am | sh

# 1. Install Shared Lib
WORKDIR /shared
COPY shared/pom.xml .
COPY shared/src ./src
RUN mvn clean install -DskipTests

# 2. Prepare Plaid Service
WORKDIR /app
COPY plaid-adapter-service/pom.xml .
RUN mvn dependency:go-offline -B

# Copy entrypoint script
COPY plaid-adapter-service/entrypoint.sh .
RUN chmod +x entrypoint.sh

# Note: We do NOT copy src here because the volume mount handles it
ENTRYPOINT ["./entrypoint.sh"]