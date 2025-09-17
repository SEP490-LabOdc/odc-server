# Please use `mvn clean install` on your local machine to build the JAR file.
# Then use this Dockerfile to build the Docker image.
#
# Example:
#   $ mvn clean install
#   $ docker build -t your-image-name .

FROM eclipse-temurin:17-jre-focal
COPY target/api-gateway-0.0.1-SNAPSHOT.jar api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/api-gateway.jar"]