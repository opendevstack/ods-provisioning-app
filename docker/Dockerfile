FROM adoptopenjdk/openjdk11:ubi-minimal-jre
 
COPY files/entrypoint.sh /usr/local/bin/
COPY app.jar app.jar

EXPOSE 8080

ENV CA_CERT none

RUN chgrp -R 0 /opt/java/openjdk/lib/security && \
    chmod -R g+rwX /opt/java/openjdk/lib/security

VOLUME /opt/provision/history
VOLUME /config
VOLUME /opt/provision/ca_cert

ENTRYPOINT ["entrypoint.sh"]
CMD ["java", "-jar", "app.jar", "--spring.config.location=file:/config/"]
