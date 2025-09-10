FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app

# ✅ CRITICAL FIX: Install Tesseract with proper tessdata packages
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

# ✅ FIX: Find actual tessdata location and verify installation
RUN tesseract --version && \
    echo "=== Searching for tessdata directories ===" && \
    find /usr -name "tessdata" -type d 2>/dev/null || echo "No tessdata directories found" && \
    find /usr -name "*.traineddata" 2>/dev/null | head -5 && \
    echo "=== Available languages ===" && \
    tesseract --list-langs || echo "Language list unavailable"

COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
