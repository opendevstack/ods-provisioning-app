FROM openjdk:8-jre-alpine
 
COPY app.jar app.jar


#Import base domain wildcard certificate
RUN echo $JAVA_HOME
COPY derp.der derp.der
RUN echo yes | $JAVA_HOME/bin/keytool -importcert -alias oc-root -keystore  $JAVA_HOME/lib/security/cacerts -storepass changeit -file /derp.der

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--spring.config.location=file:/config/application.properties"]
