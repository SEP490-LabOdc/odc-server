FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

WORKDIR /app

COPY odc-parent/ ./odc-parent/
WORKDIR /app/odc-parent
RUN mvn install -N
WORKDIR /app

COPY shared/odc-common/ ./odc-common/
WORKDIR /app/odc-common
RUN mvn install -DskipTests
WORKDIR /app

COPY infrastructure/file-service/ ./file-service/
WORKDIR /app/file-service
RUN mvn package -DskipTests


FROM eclipse-temurin:17-jre-focal

WORKDIR /app

COPY --from=builder /app/file-service/target/file-service-1.0.0.jar file-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/file-service.jar"]