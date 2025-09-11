FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Limit Maven memory
ENV MAVEN_OPTS="-Xmx256m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
RUN mvn clean package -DskipTests

# ---- Runtime Image ----
FROM openjdk:21-jre-slim
WORKDIR /app

# Install only what’s needed
RUN apt-get update && \
    apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    curl wget && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Environment variables
ENV TESSDATA_PREFIX=/usr/share
ENV TESSERACT_PATH=/usr/bin/tesseract

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 10000

# Cap JVM memory for Render 512MB instances
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-Xms128m", \
    "-Xmx384m", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.awt.headless=true", \
    "-jar", "app.jar"]
