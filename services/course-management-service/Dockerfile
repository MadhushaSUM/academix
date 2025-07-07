# --- BUILD STAGE ---
# Use a Maven base image with OpenJDK 17 for building the application.
FROM maven:3.8.5-openjdk-17 AS builder

# Set the working directory inside the container
WORKDIR /app

COPY ../../. .
# Go up two levels to the monorepo root, then copy everything down.

RUN mvn -f services/course-management-service/pom.xml package -DskipTests

# --- RUNTIME STAGE ---
# Use a lightweight JRE base image for running the application.
FROM eclipse-temurin:17-jre-focal

# Create a non-root user and group for security best practices.
# Running as root is a security risk in production containers.
RUN groupadd --system appuser && useradd --system --create-home --gid appuser appuser
# Create application directory and set permissions
RUN mkdir /app && chown appuser:appuser /app
WORKDIR /app

# Copy the built JAR from the 'builder' stage to the final image.
# The JAR file will be in target/ directory of user-management-service.
COPY --from=builder /app/services/course-management-service/target/*.jar course-management-service.jar

EXPOSE 8080

# Switch to the non-root user
USER appuser

# Consider adding JVM memory flags, e.g., `-Xmx256m` for resource constraints.
ENTRYPOINT ["java", "-jar", "course-management-service.jar"]