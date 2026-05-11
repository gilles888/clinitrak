# ============================================================
# CliniTrak Pharmacy Service — Dockerfile multi-stage
# ============================================================

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S clinitrak && adduser -S clinitrak -G clinitrak
USER clinitrak
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
