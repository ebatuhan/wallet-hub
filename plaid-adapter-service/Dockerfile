FROM maven:3.9.7-eclipse-temurin-21
RUN apt-get update && apt-get install -y curl grep ca-certificates
RUN curl -fsSL https://get.tuna.am | sh
COPY shared /shared
WORKDIR /shared
RUN mvn clean install -DskipTests
WORKDIR /app
COPY plaid-adapter-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY plaid-adapter-service/entrypoint.sh .
RUN chmod +x entrypoint.sh
COPY plaid-adapter-service/src ./src
ENTRYPOINT ["./entrypoint.sh"]