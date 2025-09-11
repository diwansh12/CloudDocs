FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app

# Install Tesseract with proper tessdata
RUN apt-get update && \
    apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    curl \
    wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Setup tessdata properly
RUN mkdir -p /usr/share/tessdata && \
    if [ -d "/usr/share/tesseract-ocr/tessdata" ]; then \
        cp -v /usr/share/tesseract-ocr/tessdata/*.traineddata /usr/share/tessdata/ 2>/dev/null || true; \
    fi && \
    if [ ! -f "/usr/share/tessdata/eng.traineddata" ]; then \
        wget -O /usr/share/tessdata/eng.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata; \
    fi && \
    if [ ! -f "/usr/share/tessdata/osd.traineddata" ]; then \
        wget -O /usr/share/tessdata/osd.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata; \
    fi && \
    chmod -R 755 /usr/share/tessdata && \
    ls -la /usr/share/tessdata/

ENV TESSDATA_PREFIX=/usr/share
ENV TESSERACT_PATH=/usr/bin/tesseract

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 10000

# ✅ CRITICAL: Memory-optimized JVM settings for Render's 512MB limit
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-Xms256m", \
    "-Xmx400m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/", \
    "-Djava.awt.headless=true", \
    "-Dfile.encoding=UTF-8", \
    "-Dspring.profiles.active=production", \
    "-jar", "app.jar"]
