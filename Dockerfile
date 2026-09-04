FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
RUN chmod +x mvnw && ./mvnw -q --no-transfer-progress -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/logs/access \
    && chown -R appuser:appgroup /app/logs

ENV JAVA_OPTS=""
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
