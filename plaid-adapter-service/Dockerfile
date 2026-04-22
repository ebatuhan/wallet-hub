FROM maven:3.9.7-eclipse-temurin-21

RUN apt-get update && apt-get install -y curl grep ca-certificates
RUN curl -fsSL https://get.tuna.am | sh

WORKDIR /shared
COPY shared/pom.xml .
COPY shared/src ./src

WORKDIR /app
COPY plaid-adapter-service/pom.xml .

COPY --chmod=755 plaid-adapter-service/entrypoint.sh /app/entrypoint.sh
ENTRYPOINT ["sh", "/app/entrypoint.sh"]