# Please use `mvn clean install` on your local machine to build the JAR file.
# Then use this Dockerfile to build the Docker image.
#
# Example:
#   $ mvn clean install
#   $ docker build -t your-image-name .

FROM eclipse-temurin:17-jre-focal
COPY target/eureka-server-1.0.0.jar eureka-server.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","/eureka-server.jar"]