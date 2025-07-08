# --- BUILD STAGE ---
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY services/pom.xml services/
COPY services/api-gateway/pom.xml services/api-gateway/

RUN --mount=type=bind,source=./pom.xml,target=/app/pom.xml \
    --mount=type=bind,source=./services/pom.xml,target=/app/services/services/pom.xml \
    --mount=type=bind,source=./services/api-gateway/pom.xml,target=/app/services/api-gateway/pom.xml \
    mvn -f services/api-gateway/pom.xml dependency:go-offline -B

COPY services/ services/

RUN mvn clean install -DskipTests -pl services/api-gateway -am

# --- RUNTIME STAGE ---
FROM eclipse-temurin:17-jre-focal

RUN groupadd --system appuser && useradd --system --create-home --gid appuser appuser
RUN mkdir /app && chown appuser:appuser /app
WORKDIR /app

COPY --from=builder /app/services/api-gateway/target/*.jar api-gateway.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
