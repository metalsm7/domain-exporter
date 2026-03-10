# docker build --rm --no-cache --platform linux/amd64 -t docker.io/mparang/domain-exporter:latest .

FROM gradle:9.1-jdk21-alpine AS builder
RUN mkdir -p /usr/local/src/javalin || true
WORKDIR /usr/local/src/javalin
COPY . .
RUN gradle build build

FROM eclipse-temurin:21-jre-alpine
RUN mkdir -p /usr/local/jooni || true
WORKDIR /usr/local/jooni
COPY --from=builder /usr/local/src/javalin/app/build/libs/*.jar .

ENTRYPOINT ["java", "-jar", "domain-exporter-0.0.1-all.jar"]