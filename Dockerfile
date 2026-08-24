# Stage 1: build the executable jar. Copying pom.xml before src lets Docker
# cache the dependency layer as long as pom.xml itself does not change.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
RUN mvn clean package -DskipTests

# Stage 2: run the jar on a minimal JRE, as a non-root user.
FROM eclipse-temurin:17-jre-jammy
RUN useradd --create-home appuser
WORKDIR /app
COPY --from=build /workspace/target/spring-graphql-tutorial-*.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
