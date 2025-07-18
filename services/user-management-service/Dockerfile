FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY services/pom.xml services/
COPY services/user-management-service/pom.xml services/user-management-service/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f services/user-management-service/pom.xml dependency:go-offline -B

COPY services/ services/

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean install -DskipTests -pl services/user-management-service -am

# --- RUNTIME STAGE ---
FROM eclipse-temurin:17-jre-focal

RUN groupadd --system appuser && useradd --system --create-home --gid appuser appuser
RUN mkdir /app && chown appuser:appuser /app
WORKDIR /app

COPY --from=builder /app/services/user-management-service/target/*.jar user-management-service.jar

EXPOSE 8080
USER appuser

ENTRYPOINT ["java", "-jar", "user-management-service.jar"]