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

COPY domain-services/operation-service/ ./operation-service/
WORKDIR /app/operation-service
RUN mvn package -DskipTests


FROM eclipse-temurin:17-jre-focal

WORKDIR /app

RUN apt-get update \
 && apt-get install -y wget \
 && rm -rf /var/lib/apt/lists/*

RUN wget https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.5/applicationinsights-agent-3.7.5.jar \
    -O /agent.jar

COPY --from=builder /app/operation-service/target/operation-service-1.0.0.jar operation-service.jar

EXPOSE 8087

ENTRYPOINT ["java", "-javaagent:/agent.jar", "-Dapplicationinsights.role.name=notification-service", "-jar", "/app/operation-service.jar"]