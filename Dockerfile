# ---- build stage: JDK + Maven, not part of the final image ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# ---- run stage: only a JRE ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --from=build /app/target/tor-exit-checker.jar app.jar

EXPOSE 8080

# Healthy = the service has loaded an exit list (see TorExitListHealthIndicator).
# start-period gives the first download time to finish before failures are counted.
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
