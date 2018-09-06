FROM openjdk:8-jre-alpine
 
COPY app.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--spring.config.location=file:/config/application.properties", "-Dserver.connection-timeout=60000"]
