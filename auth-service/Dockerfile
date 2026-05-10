# ============================================================
# CliniTrak Auth Service — Dockerfile multi-stage
# ============================================================

# Stage 1 : Build Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache des dépendances Maven (layer séparé)
COPY pom.xml .
COPY ../pom.xml ../pom.xml
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2 : Image runtime légère
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN addgroup -S clinitrak && adduser -S clinitrak -G clinitrak
USER clinitrak

# JVM flags optimisés pour conteneur
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
