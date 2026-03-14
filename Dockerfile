# Stage 1: Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
# Sirf pom.xml copy karke dependencies download karo (Caching ke liye)
COPY pom.xml .
RUN mvn dependency:go-offline

# Ab source code copy karke build karo
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Build stage se sirf jar file uthao
COPY --from=build /app/target/*.jar app.jar

# Render dynamic port use karta hai, isliye hum use expose karenge
EXPOSE 8080

# Application run karo
ENTRYPOINT ["java", "-jar", "app.jar"]