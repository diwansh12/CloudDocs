FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app

# ✅ Install Tesseract with required dependencies
RUN apt-get update && \
    apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    curl \
    wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ✅ CRITICAL: Ensure tessdata is properly installed and accessible
RUN echo "=== TESSDATA SETUP ===" && \
    tesseract --version && \
    echo "=== Finding tessdata locations ===" && \
    find /usr -name "*.traineddata" -type f 2>/dev/null | head -5 && \
    echo "=== Setting up /usr/share/tessdata ===" && \
    mkdir -p /usr/share/tessdata && \
    if [ -d "/usr/share/tesseract-ocr/tessdata" ]; then \
        cp -v /usr/share/tesseract-ocr/tessdata/*.traineddata /usr/share/tessdata/ 2>/dev/null || true; \
        echo "Copied from /usr/share/tesseract-ocr/tessdata"; \
    elif [ -d "/usr/share/tesseract-ocr/4.00/tessdata" ]; then \
        cp -v /usr/share/tesseract-ocr/4.00/tessdata/*.traineddata /usr/share/tessdata/ 2>/dev/null || true; \
        echo "Copied from /usr/share/tesseract-ocr/4.00/tessdata"; \
    fi && \
    echo "=== Downloading required files if missing ===" && \
    if [ ! -f "/usr/share/tessdata/eng.traineddata" ]; then \
        wget -O /usr/share/tessdata/eng.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata; \
    fi && \
    if [ ! -f "/usr/share/tessdata/osd.traineddata" ]; then \
        wget -O /usr/share/tessdata/osd.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata; \
    fi && \
    echo "=== Setting permissions ===" && \
    chmod -R 755 /usr/share/tessdata && \
    echo "=== Final verification ===" && \
    ls -la /usr/share/tessdata/ && \
    export TESSDATA_PREFIX=/usr/share && \
    tesseract --list-langs && \
    echo "✅ Tessdata setup complete"

# ✅ Set environment variables
ENV TESSDATA_PREFIX=/usr/share
ENV TESSERACT_PATH=/usr/bin/tesseract

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 10000

# ✅ CRITICAL: Add JVM options for native crash protection
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.awt.headless=true", \
    "-jar", "app.jar"]
