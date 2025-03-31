FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/api-gateway-0.0.1-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]