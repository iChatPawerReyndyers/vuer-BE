# Stage 1: Build application with Gradle
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy gradle wrapper and configurations
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Copy source code
COPY src src

# Build executable jar without running tests
RUN chmod +x ./gradlew && ./gradlew bootJar -x test --no-daemon

# Stage 2: Minimal Java 17 runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy built artifact from build stage
COPY --from=build /app/build/libs/vuer-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
