FROM amazoncorretto:23.0.2-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src

RUN ./gradlew dependencies --write-locks
RUN ./gradlew bootJar

FROM amazoncorretto:23.0.2-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]