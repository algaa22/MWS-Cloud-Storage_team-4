FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.antrun.skip=true

FROM eclipse-temurin:21-jdk
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY src/main/resources/ssl/server.crt /tmp/server.crt

RUN keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -noprompt -alias seaweedfs -file /tmp/server.crt

COPY --from=builder /app/target/cloud-storage-backend-1.0-SNAPSHOT.jar app.jar
COPY --from=builder /app/target/native-libs /app/native-libs

RUN chown -R appuser:appgroup /app
USER appuser

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "--enable-preview", "/app/app.jar"]