# --- BUILD STAGE ---
FROM maven:3.8.5-openjdk-17 AS builder

# Set the working directory inside the container.
WORKDIR /app

COPY pom.xml .
COPY services/pom.xml services/

COPY services/discovery-server/pom.xml services/discovery-server/

RUN --mount=type=bind,source=./pom.xml,target=/app/pom.xml \
    --mount=type=bind,source=./services/pom.xml,target=/app/services/services/pom.xml \
    --mount=type=bind,source=./services/discovery-server/pom.xml,target=/app/services/discovery-server/pom.xml \
    mvn -f services/discovery-server/pom.xml dependency:go-offline -B

COPY services/ services/

RUN mvn clean install -DskipTests -pl services/discovery-server -am

# --- RUNTIME STAGE ---
FROM eclipse-temurin:17-jre-focal

RUN groupadd --system appuser && useradd --system --create-home --gid appuser appuser

RUN mkdir /app && chown appuser:appuser /app

WORKDIR /app

COPY --from=builder /app/services/discovery-server/target/*.jar discovery-server.jar

EXPOSE 8761

USER appuser

ENTRYPOINT ["java", "-jar", "discovery-server.jar"]
