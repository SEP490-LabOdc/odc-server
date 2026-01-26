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

COPY infrastructure/eureka-server/ ./eureka-server/
WORKDIR /app/eureka-server
RUN mvn package -DskipTests


FROM eclipse-temurin:17-jre-focal

WORKDIR /app

RUN apt-get update \
 && apt-get install -y wget \
 && rm -rf /var/lib/apt/lists/*

RUN wget https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.5/applicationinsights-agent-3.7.5.jar \
    -O /agent.jar

COPY --from=builder /app/eureka-server/target/eureka-server-1.0.0.jar eureka-server.jar

EXPOSE 8761

ENTRYPOINT ["java", "-javaagent:/agent.jar", "-Dapplicationinsights.role.name=eureka-server", "-jar", "/app/eureka-server.jar"]
