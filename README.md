# OpenDevStack Provisioning Application

This application creates new opendevstack digital projects. It delegates the tasks required to several services such as jira, confluence, bitbucket and openshift (thru rundeck).

Basic idea:
1. Admin (user in `crowd.admin.group`) creates new project
    - Jira Space (name based on project key & name)
    - Confluence Spance (name based on project key)
    - Openshift projects project key-dev *-test *-cd - based on property `openshiftproject : boolean`
    - Bitbucket Project (name based on project key) - in case `openshiftproject` == true

2. Normal user `crowd.user.group` creates all resources required for a working component
    - jenkins pipeline
    - confluence
    - Bitbucket repository
    - subdomain & service discovery in openshift

3. The involved people receive an email with the setup, URLs etc - in case `mail.enabled` == true 

# Permission sets

There is a special knob to tighten security (which can be passed with the project input `createpermissionset : boolean`) which tightens security - based on three groups that need to be provided as part of the API call.

1. admin group: admin rights on the generated projects / spaces / repositories
1. user group: read / write rights on the generated projects / spaces / repositories
1. readonly group: read rights on the generated projects / spaces / repositories

The configuration for the permission sets are configured:
1. JIRA Project is provisioned with its own permissionset [defined in src/main/resources/permission-templates/jira.permission.all.txt](src/main/resources/permission-templates)
2. Confluence Project is provisioned with special permission set [defined in src/main/resources/permission-templates/confluence.permission.*](src/main/resources/permission-templates)
3. Bitbucket Project is provisioned with tight read & write roles

# Project setup

The Project is based on Spring Boot, using several techs which can be seen in the build.gradle.

If you want to build locally - create gradle.properties in the root 

    - nexus_url=<NEXUS HOST>
    - nexus_folder=candidates
    - nexus_user=<NEXUS USER>
    - nexus_pw=<NEXUS_PW> 
 
```bash
#to run the server execute
gradle bootRun
```

If you want to overwrite the provided application.properties just create a configmap out of them and 
inject the key into /config/application.properties in the container - voila, you can overwrite the config.
The base configuration map /as well as the deployment yamls can be found in /ocp-config, and overwrites parameters from application.

# Frontend Code

The frontend UI - is based on jquery and thymeleaf.
 
# Frontend Code

The backend is based on Spring Boot. Both frontende and backend are tested thru Mockito 
 
## Consuming REST APIs in Java

Generally this is a pain. To ease development, a few tools are in use:
- Jackson
- OKTTP3 Client
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

**Atlassian API's**

- [Jira API](https://docs.atlassian.com/jira/REST/server/#api/2/fullJiraProject-createProject)
- [Confluence API](https://docs.atlassian.com/ConfluenceServer/rest/6.12.1/)
- [Bitbucket API](https://developer.atlassian.com/server/bitbucket/reference/rest-api/)
- [Crowd API](https://developer.atlassian.com/server/crowd/crowd-rest-apis/)
- [Rundeck API](https://rundeck.org/docs/api/)
- [Mockito](https://site.mockito.org)