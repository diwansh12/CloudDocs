FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-slim
WORKDIR /app

# ✅ REMOVED: All Tesseract installations and configurations for memory optimization
# Previous OCR setup removed:
# - tesseract-ocr packages (~50-80MB)
# - Multiple language packs (~20-30MB each)
# - tessdata setup and verification scripts
# - Environment variables (TESSDATA_PREFIX, TESSERACT_PATH)

# ✅ MINIMAL: Install only essential utilities
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 10000

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:10000/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
