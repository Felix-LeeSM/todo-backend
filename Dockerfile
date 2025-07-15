FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /home/gradle/project
COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew clean build --no-daemon -x spotlessJavaCheck

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]