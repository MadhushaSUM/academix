# --- BUILD STAGE ---
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY services/pom.xml services/
COPY services/course-management-service/pom.xml services/course-management-service/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f services/course-management-service/pom.xml dependency:go-offline -B

COPY services/ services/

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean install -DskipTests -pl services/course-management-service -am

# --- RUNTIME STAGE ---
FROM eclipse-temurin:17-jre-focal

RUN groupadd --system appuser && useradd --system --create-home --gid appuser appuser
RUN mkdir /app && chown appuser:appuser /app
WORKDIR /app

COPY --from=builder /app/services/course-management-service/target/*.jar course-management-service.jar

EXPOSE 8443

USER appuser

ENTRYPOINT ["java", "-jar", "course-management-service.jar"]
