# OpenDevStack Provisioning Application

This application creates new OpenDevStack digital projects. It is the central entrypoint to get started with a new project / or provision new components based on quickstarters.
It delegates the tasks to create / update resources to several services such as jira, confluence, bitbucket and rundeck.

## Basic idea & usage:
1. An admin (user in a group defined in property `crowd.admin.group`) creates new ODS project. This creates 
    - a Jira Project (name based on project `key` & `name`)
    - a Confluence Space (name based on project's `key`)
    - the required Openshift projects named `key`-dev, `key`-test and `key`-cd - in case `openshiftproject == true`. Internally this is done thru a rest call to rundeck triggering the [create-projects rundeck job](https://github.com/opendevstack/ods-project-quickstarters/blob/master/rundeck-jobs/openshift/create-projects.yaml)
    - a Bitbucket Project (name based on project `key`) - in case `openshiftproject == true`

2. A normal user (user in a group defined in property `crowd.user.group`) creates all resources required for a working component - 
this happens thru the user interface - in going to modify project / picking your project and then the wanted quickstarter. Internally this is done thru a rest call to rundeck - with the picked job as parameter - [here](https://github.com/opendevstack/ods-project-quickstarters/tree/master/rundeck-jobs/quickstarts)
    - Bitbucket repository within the chosen project named `key`-`boilerplate name`
    - Openshift components based on the chosen boilerplate, coming from [ods-quickstarters](https://github.com/opendevstack/ods-project-quickstarters)

3. The involved people receive an email with the setup, URLs to components etc. - in case `mail.enabled == true` 

# Permissions

By default no special permissions are set on either confluence / jira / bitbucket or openshift, only system-wide settings are inherited.

However there is a special knob to tighten security (which can be passed with the project input `createpermissionset : boolean`)  - based on three groups that need to be provided as part of the API call / from the userinterface.

1. admin group: admin rights on the generated projects / spaces / repositories
1. user group: read / write rights on the generated projects / spaces / repositories
1. readonly group: read rights on the generated projects / spaces / repositories

The configuration for the permission sets are configured:
1. JIRA Project is provisioned with its own permissionset [defined in src/main/resources/permission-templates/jira.permission.all.txt](src/main/resources/permission-templates/jira.permission.all.txt)
2. Confluence Project is provisioned with special permission set [defined in src/main/resources/permission-templates/confluence.permission.*](src/main/resources/permission-templates)
3. Bitbucket Project is provisioned with tight read & write roles

# Project types
The default jira / confluence project' types are defined in [src/main/resources/application.properties](src/main/resources/application.properties) - and correspondingly in the config maps

```
project.template.key.names=default

jira.project.template.key=com.pyxis.greenhopper.jira:gh-scrum-template
jira.project.template.type=software

confluence.blueprint.key=com.atlassian.confluence.plugins.confluence-software-project:sp-space-blueprint
```

To add a new template - copy, and add your config, based on a new `<name>`

```
jira.project.template.key.<name>=com.pyxis.greenhopper.jira:gh-scrum-template
jira.project.template.type.<name>=software

# optional, can stay as is
confluence.blueprint.key.<name>=com.atlassian.confluence.plugins.confluence-software-project:sp-space-blueprint
```

and add the new <name> from above to the existing property `project.template.key.names`

```
project.template.key.names=default,<name>
```

# Internal architecture

The Project is based on Spring Boot, using several technologies which can be seen in the [build.gradle](build.gradle).

The provision app is merely an orchestrator that does HTTP REST calls to Atlassian Crowd, Jira, Confluence, Bitbucket and
Rundeck (for openshift interaction).

The APIs exposed for direct usage, and also for the UI are in the [controller package](src/main/java/org/opendevstack/provision/controller). 
The connectors to the various tools to create resources are in the [services package](src/main/java/org/opendevstack/provision/services)

If you want to build locally - create gradle.properties in the root 

    - nexus_url=<NEXUS HOST>
    - nexus_folder=candidates
    - nexus_user=<NEXUS USER>
    - nexus_pw=<NEXUS_PW> 
 
```bash
# to run the server execute
gradle bootRun
```

If you want to overwrite the provided [application.properties](src/main/resources/application.properties) just create a configmap out of them and inject the key into /config/application.properties in the container - voila, you can overwrite the config.
The base configuration map /as well as the deployment yamls can be found in [ocp-config](ocp-config/prov-app/cm.yml), and overwrite parameters from application.

# Frontend Code

The frontend UI - is based on jquery and thymeleaf. All [posting to the API](src/main/resources/static/js/client.js) happens out of java script (client.js)
 
# Backend Code

The backend is based on Spring Boot, and authenticates against Atlassian Crowd. Both frontend (html) and backend are tested thru Mockito 
 
## Consuming REST APIs in Java

Generally this is a pain. To ease development, a few tools are in use:
- Jackson
- OKTTP3 Client (see link below)
- jsonschema2pojo generator (see link below)

The process for new operations to be called is: 
1. Look up the API call that you intend to make
2. see if there is a JSON Schema available
3. Generate (a) Pojo('s) for the Endpoint
4. Use the pojo to build your request, convert it to JSON with Jackson and send it via OKHTTP3
 
## Link collection

- [Mkyong spring boot + security + thymeleaf example](http://www.mkyong.com/spring-boot/spring-boot-spring-security-thymeleaf-example/)

- [Getting more Webjars](http://www.webjars.org/)

- [Generating POJOs from JSON Schemas](http://www.jsonschema2pojo.org/) very helpful for the Atlassian API Docs

- [OKHttp3](https://square.github.io/okhttp)

- [Mockito](https://site.mockito.org)

**Atlassian API's**

- [Jira API](https://docs.atlassian.com/jira/REST/server/#api/2/fullJiraProject-createProject)
- [Confluence API](https://docs.atlassian.com/ConfluenceServer/rest/6.12.1/)
- [Bitbucket API](https://developer.atlassian.com/server/bitbucket/reference/rest-api/)
- [Crowd API](https://developer.atlassian.com/server/crowd/crowd-rest-apis/)
- [Rundeck API](https://rundeck.org/docs/api/)

