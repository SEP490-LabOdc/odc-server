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

COPY infrastructure/api-gateway/ ./api-gateway/
WORKDIR /app/api-gateway
RUN mvn package -DskipTests


FROM eclipse-temurin:17-jre-focal

WORKDIR /app

COPY --from=builder /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar api-gateway.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]