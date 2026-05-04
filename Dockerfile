FROM amazoncorretto:17 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew build -x test --no-daemon

FROM amazoncorretto:17

WORKDIR /app

RUN yum install -y curl

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 80

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
