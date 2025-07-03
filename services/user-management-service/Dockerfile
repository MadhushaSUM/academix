# --- BUILD STAGE ---
# Use a Maven base image with OpenJDK 17 for building the application.
# This stage contains the JDK and Maven necessary to compile your Java code.
FROM maven:3.8.5-openjdk-17 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the common 'libs' modules from the monorepo first.
# This leverages Docker cache as these dependencies change less frequently.
# We're copying them into a temporary structure that Maven expects.
# Adjust paths if your `libs` directory structure is different.
# NOTE: This assumes you run `docker build` from the monorepo root.
#COPY libs/common-models/pom.xml libs/common-models/
#COPY libs/common-utils/pom.xml libs/common-utils/
#COPY libs/common-models/src libs/common-models/src
#COPY libs/common-utils/src libs/common-utils/src

# Copy the parent 'services' pom.xml and the service's pom.xml.
# These also change less frequently than the actual code.
COPY pom.xml ./
COPY services/pom.xml services/
COPY services/user-management-service services/user-management-service/

# Download all project dependencies (including those for 'libs' and 'services')
# This caches dependencies, so if only code changes, this step is skipped.
# We go into the specific service directory for the build to generate its JAR.
# The `../` is relative to the WORKDIR /app.
RUN mvn -f services/user-management-service/pom.xml clean install -DskipTests

# --- RUNTIME STAGE ---
# Use a lightweight JRE base image for running the application.
# This stage contains only the Java Runtime Environment, not the full JDK.
# eclipse-temurin provides reliable and production-ready OpenJDK builds.
FROM eclipse-temurin:17-jre-focal

# Create a non-root user and group for security best practices.
# Running as root is a security risk in production containers.
RUN groupadd --system appuser && useradd --system --create-home --gid appuser appuser
# Create application directory and set permissions
RUN mkdir /app && chown appuser:appuser /app
WORKDIR /app

# Copy the built JAR from the 'builder' stage to the final image.
# The JAR file will be in target/ directory of user-management-service.
COPY --from=builder /app/services/user-management-service/target/*.jar user-management-service.jar

# Expose the port your Spring Boot application listens on.
# Default for Spring Boot is 8080. Ensure this matches your application.properties/yml.
EXPOSE 8080

# Switch to the non-root user
USER appuser

# Define the command to run your Spring Boot application.
# `java -jar` is the standard way to run a Spring Boot executable JAR.
# Consider adding JVM memory flags, e.g., `-Xmx256m` for resource constraints.
ENTRYPOINT ["java", "-jar", "user-management-service.jar"]

# For Spring Boot 3.2+ with layered jars, consider using the JarLauncher entrypoint
# if you've enabled layers in your spring-boot-maven-plugin.
# ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
# If using layered JARs, the COPY commands would be different (copying layers).
# For simplicity, we'll stick to a fat JAR approach for now, which is very common.