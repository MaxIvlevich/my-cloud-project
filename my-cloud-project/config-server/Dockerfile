FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y curl --no-install-recommends && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/config-server-0.0.1-SNAPSHOT.jar
WORKDIR /app
COPY config-repo /etc/config-repo
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]