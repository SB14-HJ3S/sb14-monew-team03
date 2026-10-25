FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=builder --chown=spring:spring /app/build/libs/*-SNAPSHOT.jar app.jar
USER spring:spring
ENV JVM_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=70 $JVM_OPTS -Duser.timezone=Asia/Seoul -jar app.jar"]
