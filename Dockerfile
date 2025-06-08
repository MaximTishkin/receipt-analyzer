FROM maven:3.9.5-eclipse-temurin-17 AS build

# Копирование исходного кода
WORKDIR /app
COPY . .

# Сборка приложения
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

# Установка Tesseract и русского языкового пакета
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-rus \
    && rm -rf /var/lib/apt/lists/*

# Копирование JAR файла
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Открытие порта
EXPOSE 8080

# Запуск приложения
CMD ["java", "-jar", "app.jar"] 