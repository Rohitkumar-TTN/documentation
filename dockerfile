FROM eclipse-temurin:8-jdk

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew build

EXPOSE 8080

CMD ["java", "-jar", "build/libs/engineering-docs-hub-0.0.1-SNAPSHOT.jar"]