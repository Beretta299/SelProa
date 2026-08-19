FROM gradle:9-jdk21 AS build
WORKDIR /src
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/build/install/market-api ./
EXPOSE 8081
ENTRYPOINT ["/app/bin/market-api"]
