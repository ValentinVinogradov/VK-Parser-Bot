FROM eclipse-temurin:23-jdk-alpine

WORKDIR /app

COPY . .

RUN chmod +x ./mvnw

RUN ./mvnw clean package -DskipTests

RUN apk add --no-cache curl

EXPOSE 8080

CMD ["java", "-jar", "target/vkparser.jar"]