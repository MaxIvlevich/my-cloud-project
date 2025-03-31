FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/config-server-0.0.1-SNAPSHOT.jar
WORKDIR /app
RUN apt-get update && apt-get install -y git
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]