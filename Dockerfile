# ─── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/

# Pre-fetch plugin + dependency jars into Gradle cache
RUN ./gradlew --no-daemon compileJava 2>/dev/null; exit 0

# Copy source and build the layered bootJar
COPY . .
RUN ./gradlew --no-daemon :payment-platform:bootJar

# ─── Stage 2: Extract layers ─────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /app

COPY --from=builder /app/payment-platform/build/libs/payment-platform-*.jar app.jar
# Extract into layers: dependencies, spring-boot-loader, snapshot-dependencies, application
RUN java -Djarmode=layertools -jar app.jar extract

# ─── Stage 3: Runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S payhub && adduser -S payhub -G payhub
USER payhub
WORKDIR /app

# Copy layers in order of change frequency (Docker caches unchanged layers)
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

EXPOSE 8080

ENV SERVER_SHUTDOWN=graceful
ENV SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseZGC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
