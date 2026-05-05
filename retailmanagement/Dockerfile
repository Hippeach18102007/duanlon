# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy toàn bộ nội dung thư mục backend vào container
COPY retailmanagement/ /app/

# Chạy lệnh build ngay tại root của /app (nơi có pom.xml vừa copy sang)
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy file jar từ bước build sang.
# Lưu ý: folder target nằm ngay trong /app sau lệnh mvn package
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]