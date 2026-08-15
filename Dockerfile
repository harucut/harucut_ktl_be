# ── Build stage ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

COPY src src
RUN ./gradlew bootJar -x test --no-daemon -q

# ── Run stage ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 내부 LocalDateTime(createdAt/updatedAt, 배치 스케줄 등)은 UTC 기준으로 다룬다(JacksonConfig가
# API 응답에서만 KST(+09:00)로 변환해 내려준다). 베이스 이미지 기본값에 암묵적으로 의존하지 않도록 명시적으로 고정한다.
ENV TZ=UTC

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
