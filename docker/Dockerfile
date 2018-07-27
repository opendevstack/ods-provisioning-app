FROM openjdk:8-jre-alpine
ARG nexusUsername
ARG nexusPassword
ARG nexusHost
ARG projectId
ARG componentId
ARG tagversion
ARG nexusfolder
 
RUN echo "projectId=${projectId}" && echo "${nexusHost}" && apk add --no-cache --virtual .build-deps curl && curl --fail -o app.jar -v -u ${nexusUsername}:${nexusPassword} ${nexusHost}/repository/candidates/org/opendevstack/${projectId}/${componentId}/${tagversion}/${componentId}-${tagversion}.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--spring.config.location=file:/config/application.properties"]
