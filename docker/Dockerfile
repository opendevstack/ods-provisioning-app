FROM registry.access.redhat.com/ubi9/openjdk-11

USER root

RUN microdnf upgrade -y && \
    microdnf clean all

RUN chown -c 1001:0 $JAVA_HOME/lib/security/cacerts \
    && chmod -c g+w $JAVA_HOME/lib/security/cacerts

USER 1001
 
COPY files/entrypoint.sh /usr/local/bin/
COPY app.jar app.jar

EXPOSE 8080

ENV CA_CERT none

VOLUME /opt/provision/history
VOLUME /config
VOLUME /opt/provision/ca_cert

ENTRYPOINT ["entrypoint.sh"]

# Note:
# spring.config.additional-location allows to override the defaults
# which are defined in classpath:/application.properties
# for details refert to Spring boot documention:
# https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config
CMD ["java", "-jar", "app.jar", "--spring.config.additional-location=/quickstarters/quickstarters.properties,optional:file:/jira-project-types/additional-templates.properties"]
