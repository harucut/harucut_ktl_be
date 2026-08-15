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

# LocalDateTime(createdAt/updatedAt 등)이 UTC 기준이라는 전제로 직렬화한다(JacksonConfig 참고).
# 베이스 이미지 기본값에 암묵적으로 의존하지 않도록 명시적으로 고정한다.
ENV TZ=UTC

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
