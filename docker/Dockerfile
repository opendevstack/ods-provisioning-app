FROM adoptopenjdk/openjdk11:ubi-minimal-jre
 
COPY files/entrypoint.sh /usr/local/bin/
COPY app.jar app.jar

EXPOSE 8080

ENV CA_CERT none

RUN chmod g+w /opt/java/openjdk/lib/security/cacerts

VOLUME /opt/provision/history
VOLUME /config
VOLUME /opt/provision/ca_cert

ENTRYPOINT ["entrypoint.sh"]

# Note:
# spring.config.additional-location allows to override the defaults
# which are defined in classpath:/application.properties
# for details refert to Spring boot documention:
# https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config
CMD ["java", "-jar", "app.jar", "--spring.config.additional-location=/quickstarters/quickstarters.properties,/jira-project-types/additional-templates.properties,/config/*/"]
