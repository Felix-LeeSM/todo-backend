# back/Dockerfile

# Build stage: Gradle 8.12 with JDK 21
FROM gradle:8.12-jdk21 AS builder
WORKDIR /home/gradle/project
COPY . .
RUN gradle clean build --no-daemon -x spotlessJavaCheck

# Runtime stage: OpenJDK 21 (slim version for smaller image and security)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user and switch to it
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

# Copy the built JAR file
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]