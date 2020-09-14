# OpenDevStack Provisioning Application

![](https://github.com/opendevstack/ods-provisioning-app/workflows/Provisioning%20App%20Build/badge.svg?branch=master)
![](https://4e53c33a6387.ngrok.io/images/provapptestsoutcome_master.svg)

## Introduction
This application creates new OpenDevStack projects. It is the central entrypoint to get started with a new project / or provision new components based on [quickstarters](https://github.com/opendevstack/ods-quickstarters).
It delegates the tasks to create / update resources to several services such as Jira, Confluence, Bitbucket and Jenkins.

## Documentation

The sources are located in the antora folder at https://github.com/opendevstack/ods-provisioning-app/tree/master/docs/.

## Development

Open the cloned provision application in your favorite IDE.

If you run the application locally, you will have to provide some addional information.

In case you want to use a local Nexus, you will have to create a `gradle.properties` file in the root to provide the Nexus credentials.
```
nexus_url=http://nexus-cd.192.168.56.101.nip.io
nexus_user=developer
nexus_pw=developer
```

You also have to ensure the Nexus certificate is integrated in the keystore of the JDK the IDE uses.

If you don’t want to use the internal Nexus, you will have to provide a `gradle.properties` file with the following content:
```
no_nexus=true
```

After startup the application is available at http://localhost:8080/.

You can login in with the Crowd admin user.
