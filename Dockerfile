# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Maven Wrapper und pom.xml kopieren für Dependency-Caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dependencies herunterladen (wird gecached)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Source Code kopieren
COPY src src

# Build ohne Tests (Tests laufen in CI/CD)
RUN ./mvnw package -DskipTests -B

# JAR extrahieren für Layered Build
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:17-jre AS runtime

# Sicherheit: Non-root User
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

WORKDIR /app

# Health Check Dependencies
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Layered JARs kopieren (besseres Caching)
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Ownership ändern
RUN chown -R appuser:appgroup /app

USER appuser

# Port exponieren
EXPOSE 8080

# Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Spring Boot Layered Launcher
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]