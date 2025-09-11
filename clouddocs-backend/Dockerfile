FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install minimal Tesseract
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    tesseract-ocr-eng \
    curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Setup tessdata
RUN mkdir -p /usr/share/tessdata && \
    cp /usr/share/tesseract-ocr/tessdata/*.traineddata /usr/share/tessdata/ 2>/dev/null || \
    (wget -q -O /usr/share/tessdata/eng.traineddata \
     https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata && \
     wget -q -O /usr/share/tessdata/osd.traineddata \
     https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata) && \
    chmod -R 755 /usr/share/tessdata

ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV TESSERACT_PATH=/usr/bin/tesseract
ENV SPRING_PROFILES_ACTIVE=production

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

# Even more aggressive settings for Debian
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=55.0", \
    "-XX:InitialRAMPercentage=25.0", \
    "-XX:+UseStringDeduplication", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.awt.headless=true", \
    "-Dspring.main.lazy-initialization=true", \
    "-jar", "app.jar"]
