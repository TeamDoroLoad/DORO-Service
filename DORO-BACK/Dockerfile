# syntax=docker/dockerfile:1

# ---------- 1) Build stage ----------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Gradle wrapper와 빌드 스크립트만 먼저 복사해 의존성 레이어를 캐싱
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version --no-daemon

# 소스 복사 후 부트 JAR 빌드 (테스트는 이미지 빌드 단계에서 제외)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test \
    && find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

# ---------- 2) Run stage ----------
FROM eclipse-temurin:21-jre-jammy AS run
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=aws
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && exec 3<&-' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
