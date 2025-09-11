FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY clouddocs-backend/pom.xml ./
RUN mvn dependency:go-offline -B
COPY clouddocs-backend/src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:21-jdk-slim
WORKDIR /app

# Install Tesseract
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    curl \
    wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# ✅ CRITICAL FIX: Correct tessdata path configuration
RUN echo "=== FIXING TESSDATA PATH ===" && \
    echo "Finding tessdata files..." && \
    find /usr -name "*.traineddata" -type f 2>/dev/null | head -5 && \
    echo "Ensuring tessdata is accessible..." && \
    if [ -d "/usr/share/tesseract-ocr/tessdata" ]; then \
        echo "Found tessdata in /usr/share/tesseract-ocr/tessdata"; \
        mkdir -p /usr/share/tessdata && \
        cp -v /usr/share/tesseract-ocr/tessdata/*.traineddata /usr/share/tessdata/ && \
        echo "Copied traineddata files to /usr/share/tessdata/"; \
    fi && \
    echo "Downloading required files if missing..." && \
    if [ ! -f "/usr/share/tessdata/eng.traineddata" ]; then \
        mkdir -p /usr/share/tessdata && \
        wget -O /usr/share/tessdata/eng.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata; \
    fi && \
    if [ ! -f "/usr/share/tessdata/osd.traineddata" ]; then \
        wget -O /usr/share/tessdata/osd.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata; \
    fi && \
    chmod -R 755 /usr/share/tessdata && \
    echo "Final tessdata verification:" && \
    ls -la /usr/share/tessdata/ && \
    echo "Testing Tesseract configuration:" && \
    export TESSDATA_PREFIX=/usr/share/tessdata && \
    tesseract --list-langs

# ✅ CRITICAL: Set TESSDATA_PREFIX to the tessdata directory itself
ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV TESSERACT_PATH=/usr/bin/tesseract

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 10000

# Memory-optimized JVM settings
ENTRYPOINT ["java", \
    "-server", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-Xms256m", \
    "-Xmx384m", \
    "-XX:+UseContainerSupport", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.awt.headless=true", \
    "-jar", "app.jar"]
