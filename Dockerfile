FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.antrun.skip=true

# --- Финальный образ ---
FROM eclipse-temurin:21-jdk
WORKDIR /app

# 1. Создаем пользователя и группу
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# 2. Копируем сертификат (предполагается, что папка ssl лежит рядом с Dockerfile)
COPY src/main/resources/ssl/server.crt /tmp/server.crt

# 3. Импортируем сертификат в системный TrustStore Java (делаем это под ROOT)
# Пароль по умолчанию у хранилища cacerts — 'changeit'
RUN keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -noprompt -alias seaweedfs -file /tmp/server.crt

# 4. Копируем файлы приложения
COPY --from=builder /app/target/cloud-storage-backend-1.0-SNAPSHOT.jar app.jar
COPY --from=builder /app/target/native-libs /app/native-libs

# 5. Настраиваем права доступа
RUN chown -R appuser:appgroup /app
USER appuser

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "--enable-preview", "/app/app.jar"]