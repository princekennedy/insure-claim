# Build stage: compile the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy Maven wrapper and config first for better layer caching
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src ./src

# Maven wrapper needs execute permission inside the container
RUN chmod +x ./mvnw

# Build the application (skip tests for faster CI builds; remove -DskipTests for local dev)
RUN ./mvnw package -DskipTests -B

# Runtime stage: run the JAR with a slim JRE
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose the Spring Boot port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
