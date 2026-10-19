FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
# 보안을 위해 관리자(root) 권한 없이, 앱 전용 일반 사용자(spring)로 실행
RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=builder --chown=spring:spring /app/build/libs/*-SNAPSHOT.jar app.jar
USER spring:spring
# 힙 크기 등을 바꿀 때 태스크 정의에서 JVM_OPTS만 넣으면 됨 (기본은 비어 있음)
ENV JVM_OPTS=""
# 컨테이너 기본 시간대가 UTC라 배치 실행 시각·날짜 경계가 한국 시간과 어긋나지 않게 맞춤
# exec로 java가 종료 신호(SIGTERM)를 직접 받아 정상 종료되게 함
ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -Duser.timezone=Asia/Seoul -jar app.jar"]
