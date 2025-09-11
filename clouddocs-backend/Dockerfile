FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app

# ✅ Install Tesseract with language packages
RUN apt-get update && \
    apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-fra \
    tesseract-ocr-deu \
    tesseract-ocr-spa \
    libtesseract-dev \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ✅ CRITICAL FIX: Set correct tessdata environment variables
ENV TESSDATA_PREFIX=/usr/share
ENV TESSERACT_PATH=/usr/bin/tesseract

# ✅ Verify tessdata installation and show actual locations
RUN echo "=== Tesseract Version ===" && \
    tesseract --version && \
    echo "=== Available Languages ===" && \
    tesseract --list-langs && \
    echo "=== Tessdata Locations ===" && \
    find /usr -name "*.traineddata" -type f 2>/dev/null | head -10 && \
    echo "=== Tessdata Directory Structure ===" && \
    ls -la /usr/share/tessdata/ 2>/dev/null || ls -la /usr/share/tesseract-ocr/tessdata/ 2>/dev/null || echo "Standard tessdata not found" && \
    echo "=== Environment Variables ===" && \
    echo "TESSDATA_PREFIX=$TESSDATA_PREFIX" && \
    echo "TESSERACT_PATH=$TESSERACT_PATH"

# Copy JAR file
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

# Expose port (use 10000 to match your backend)
EXPOSE 10000

# Health check (update port to 10000)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:10000/api/health || exit 1

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
