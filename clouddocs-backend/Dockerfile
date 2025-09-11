FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app

# ✅ ENHANCED: Install Tesseract with comprehensive tessdata setup
RUN apt-get update && \
    apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-fra \
    tesseract-ocr-deu \
    tesseract-ocr-spa \
    libtesseract-dev \
    curl \
    wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ✅ CRITICAL: Comprehensive tessdata location detection and setup
RUN echo "=== TESSDATA SETUP AND VERIFICATION ===" && \
    echo "Tesseract version:" && \
    tesseract --version && \
    echo "=== Searching for all tessdata locations ===" && \
    find /usr -name "*.traineddata" -type f 2>/dev/null | head -10 && \
    find /usr -name "tessdata" -type d 2>/dev/null && \
    echo "=== Checking common tessdata directories ===" && \
    ls -la /usr/share/tessdata/ 2>/dev/null && echo "Found tessdata in /usr/share/tessdata" || echo "/usr/share/tessdata not found" && \
    ls -la /usr/share/tesseract-ocr/tessdata/ 2>/dev/null && echo "Found tessdata in /usr/share/tesseract-ocr/tessdata" || echo "/usr/share/tesseract-ocr/tessdata not found" && \
    ls -la /usr/share/tesseract-ocr/4.00/tessdata/ 2>/dev/null && echo "Found tessdata in /usr/share/tesseract-ocr/4.00/tessdata" || echo "/usr/share/tesseract-ocr/4.00/tessdata not found" && \
    echo "=== Creating tessdata in expected location ===" && \
    mkdir -p /usr/share/tessdata && \
    echo "=== Copying tessdata from actual location to expected location ===" && \
    if [ -d "/usr/share/tesseract-ocr/tessdata" ]; then \
        cp -r /usr/share/tesseract-ocr/tessdata/* /usr/share/tessdata/ 2>/dev/null || true; \
        echo "Copied from /usr/share/tesseract-ocr/tessdata to /usr/share/tessdata"; \
    elif [ -d "/usr/share/tesseract-ocr/4.00/tessdata" ]; then \
        cp -r /usr/share/tesseract-ocr/4.00/tessdata/* /usr/share/tessdata/ 2>/dev/null || true; \
        echo "Copied from /usr/share/tesseract-ocr/4.00/tessdata to /usr/share/tessdata"; \
    else \
        echo "No standard tessdata found, downloading manually..."; \
        wget -q -O /usr/share/tessdata/eng.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata && \
        wget -q -O /usr/share/tessdata/osd.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata && \
        echo "Downloaded tessdata manually"; \
    fi && \
    echo "=== Final tessdata verification ===" && \
    ls -la /usr/share/tessdata/ && \
    echo "=== Testing tesseract with new tessdata location ===" && \
    export TESSDATA_PREFIX=/usr/share && \
    tesseract --list-langs && \
    echo "✅ Tessdata setup completed successfully"

# ✅ Set correct environment variables
ENV TESSDATA_PREFIX=/usr/share
ENV TESSERACT_PATH=/usr/bin/tesseract

# Copy JAR file
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory (temporary until persistent storage configured)
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

# Expose port
EXPOSE 10000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:10000/api/health || exit 1

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
