# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/routeoptimizer-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "if [ -n \"$DATABASE_URL\" ]; then raw=${DATABASE_URL#postgresql://}; export SPRING_DATASOURCE_URL=jdbc:postgresql://${raw#*@}; fi; exec java -jar app.jar"]
