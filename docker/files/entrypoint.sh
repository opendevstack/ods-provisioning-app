#!/bin/bash
set -e

# By default if NO CA_CERT env is passed, the entrypoint tries to find the Openshift default CA and import it into the cacerts of the JVM.
# If a certificate is passed via the CA_CERT env it is tried to find it in the volume mount /opt/provision/ca_cert.
# If no certificate is configured and the Openshift default can not be found (e.g. running locally) the prov app will start without importing an additional CA cert.

# Openshift default CA. See https://docs.openshift.com/container-platform/3.11/dev_guide/secrets.html#service-serving-certificate-secrets
SERVICEACCOUNT_CA='/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt'
CA_CERT_PATH='/opt/provision/ca_cert'

IMPORT_CA='none'
if [[ $CA_CERT == 'none' ]]; then
  echo "INFO: no CA_CERT configured checking for default '$SERVICEACCOUNT_CA'"

  if [[ -f $SERVICEACCOUNT_CA ]]; then
    echo "INFO: found $SERVICEACCOUNT_CA"
    IMPORT_CA=$SERVICEACCOUNT_CA
  else
    echo "INFO: could not find '$SERVICEACCOUNT_CA'"
  fi

else
  echo "INFO: CA_CERT is set to '$CA_CERT'"

  if [[ -f "$CA_CERT_PATH/$CA_CERT" ]]; then
    echo "INFO: found $CA_CERT_PATH/$CA_CERT"
    IMPORT_CA="$CA_CERT_PATH/$CA_CERT"
  else
    echo "WARN: CA_CERT is set but could not be found, maybe you forgot to mount '$CA_CERT_PATH' ?"
    exit 1
  fi

fi

if [[ $IMPORT_CA != 'none' ]]; then
  echo "INFO: importing '$IMPORT_CA' into cacerts"
  keytool -importcert -v -trustcacerts -alias user-ca-root -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -file $IMPORT_CA -noprompt
else
  echo "INFO: no CA imported"
fi

exec "$@"
