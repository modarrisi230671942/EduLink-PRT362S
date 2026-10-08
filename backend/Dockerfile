# ── Build stage: compile and package the Spring Boot app ──
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# Download dependencies first so they are cached between builds when only source code changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ── Run stage: a small JRE image, running as a non-root user ──
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home edulink && mkdir -p /app/uploads && chown edulink /app/uploads
COPY --from=build /app/target/edulink-backend-*.jar app.jar
USER edulink
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
