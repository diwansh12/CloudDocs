FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml from the correct path
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:21-jdk-slim
WORKDIR /app

# Install Tesseract with proper setup
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    curl \
    wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# ✅ CRITICAL FIX: Setup tessdata correctly
RUN echo "=== FIXING TESSDATA PATH ===" && \
    mkdir -p /usr/share/tessdata && \
    if [ -d "/usr/share/tesseract-ocr/tessdata" ]; then \
        cp -v /usr/share/tesseract-ocr/tessdata/*.traineddata /usr/share/tessdata/ 2>/dev/null || true; \
        echo "Copied tessdata files"; \
    fi && \
    if [ ! -f "/usr/share/tessdata/eng.traineddata" ]; then \
        wget -q -O /usr/share/tessdata/eng.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata; \
    fi && \
    if [ ! -f "/usr/share/tessdata/osd.traineddata" ]; then \
        wget -q -O /usr/share/tessdata/osd.traineddata \
        https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata; \
    fi && \
    chmod -R 755 /usr/share/tessdata && \
    ls -la /usr/share/tessdata/

# ✅ CORRECTED: Set TESSDATA_PREFIX to tessdata directory
ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV TESSERACT_PATH=/usr/bin/tesseract
ENV SPRING_PROFILES_ACTIVE=production

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 10000

# Memory-optimized JVM settings for Render
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
