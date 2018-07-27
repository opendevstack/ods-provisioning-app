# OpenDevStack Provisioning Application

This application creates new opendevstack digital projects. It delegates the tasks required to several services such as jira, confluence, bitbucket and openshift.

Basic idea:
1. Admin (user in `openshift admin group`) creates new project
	- Jira Space
	- Confluence Spance
	- Bitbucket Project
	- Openshift projects *-dev *-test *-cd 
2. Normal user `openshift user group` creates all resources required for a working component
    - jenkins pipeline
    - confluence
    - git repos
    - subdomain & service discovery in openshift
3. The involved people receive an email with the setup, URLs etc.

# Project setup
The Project is based on Spring Boot, using several techs which can be seen in the build.gradle.

if you want to build locally - create gradle.properties in the root 
nexus_url=<NEXUS HOST>
nexus_folder=candidates
nexus_user=<NEXUS USER>
nexus_pw=<NEXUS_PW>
 
```bash
#to run the server execute
gradle bootRun
```

If you want to overwrite the provided application.properties just create a configmap out of them and 
inject the key into /config/application.properties in the container - voila, you can overwrite the config.
The base configuration map /as well as the deployment yamls can be found in /ocp-config 

# Frontend Code

The frontend UI
 
## Consuming REST APIs in Java
Generally this is a pain. To ease development, a few tools are in use
- Jackson
- OKTTP3 Client
- jsonschema2pojo generator (see link below)

The process is this: 
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