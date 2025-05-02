FROM gradle:7.6-jdk AS build
WORKDIR /app
COPY build.gradle settings.gradle* ./
COPY gradlew ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM openjdk:17-jdk-slim
WORKDIR /app

RUN apt-get update && \
    apt-get install -y libfreetype6 fontconfig && \
    apt-get clean

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java","-jar","app.jar"]
