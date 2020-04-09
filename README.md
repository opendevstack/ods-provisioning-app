# OpenDevStack Provisioning Application

![](https://github.com/opendevstack/ods-provisioning-app/workflows/Provisioning%20App%20Build/badge.svg?branch=master)

## Introduction
This application creates new OpenDevStack digital projects. It is the central entrypoint to get started with a new project / or provision new components based on [quickstarters](https://github.com/opendevstack/ods-project-quickstarters).
It delegates the tasks to create / update resources to several services such as jira, confluence, bitbucket and jenkins - which are the default implementations

## Documentation

See [OpenDevStack Provisioning Application](https://www.opendevstack.org/ods-documentation/ods-provisioning-app/latest/index.html) for details.

The source of this documentation is located in the antora folder at https://github.com/opendevstack/ods-provisioning-app/tree/master/docs/modules/ROOT/pages.

A guide about how to contribute to the documentation is located at [The OpenDevStack documentation](https://www.opendevstack.org/ods-documentation/common/latest/documentation.html).

## Development

Open the cloned provision application in your favorite IDE.

If you run the application from your IDE, you will have to provide some addional informations.

In case you want to use your local Nexus, you will have to create a gradle.properties file in the ods-provisioning-app project to provide the Nexus credentials, because we disabled anonymous access.
```
nexus_url=http://nexus-cd.192.168.56.101.nip.io
nexus_user=developer
nexus_pw=developer
```

You also have to ensure the Nexus certificate is integrated in the keystore of the JDK the IDE uses.

If you don’t want to use the internal Nexus and run the application from your IDE, you will have to provide a gradle.properties file with the following content:
```
no_nexus=true
```
After startup via the IDE the application is available at http://localhost:8080/

You can login in with the Crowd admin user you set up earlier.
