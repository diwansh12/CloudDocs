FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# OCR/Tesseract completely removed for memory optimization
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 10000

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:10000/api/health || exit 1

# ✅ FIXED: Increased thread stack size from 128k to 512k
ENTRYPOINT ["java", \
  "-Xms100m", \
  "-Xmx200m", \
  "-Xss512k", \
  "-XX:+UseSerialGC", \
  "-XX:MaxMetaspaceSize=80m", \
  "-XX:CompressedClassSpaceSize=10m", \
  "-XX:MaxDirectMemorySize=15m", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app", \
  "-XX:MaxGCPauseMillis=100", \
  "-jar", "app.jar"]
