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

COPY shared/odc-common-lib/ ./odc-common-lib/
WORKDIR /app/odc-common-lib
RUN mvn install -DskipTests
WORKDIR /app

COPY infrastructure/email-service/ ./email-service/
WORKDIR /app/email-service
RUN mvn package -DskipTests


FROM eclipse-temurin:17-jre-focal

WORKDIR /app

COPY --from=builder /app/email-service/target/email-service-1.0.0.jar email-service.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/email-service.jar"]