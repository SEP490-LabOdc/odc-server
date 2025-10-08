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

COPY domain-services/checklist-service/ ./checklist-service/
WORKDIR /app/checklist-service
RUN mvn package -DskipTests


FROM eclipse-temurin:17-jre-focal

WORKDIR /app

COPY --from=builder /app/checklist-service/target/checklist-service-1.0.0.jar checklist-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/checklist-service.jar"]