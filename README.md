# OpenDevStack Provisioning Application

This application creates new OpenDevStack digital projects. It is the central entrypoint to get started with a new project / or provision new components based on quickstarters.
It delegates the tasks to create / update resources to several services such as jira, confluence, bitbucket and rundeck.

## Basic idea & usage:
1. An admin (user in a group defined in property `crowd.admin.group`) creates new ODS project. This in turn creates 
    - a Jira Project (name based on project `key` & `name`)
    - a Confluence Space (name based on project's `key`)
    - the required Openshift projects named `key`-dev, `key`-test and `key`-cd - in case `openshiftproject == true`. Internally this is done thru a rest call to rundeck triggering the [create-projects rundeck job](https://github.com/opendevstack/ods-project-quickstarters/blob/master/rundeck-jobs/openshift/create-projects.yaml)
    - a Bitbucket Project (name based on project `key`) - in case `openshiftproject == true`. Within this project two default repositories are created `key`-oc-config-artifacts for all `yaml` resources as well as `key`-design for any design artifacts (e.g. sketches)

2. A normal user (user in a group defined in property `crowd.user.group`) creates all resources required for a working component - 
this happens thru the user interface - in going to modify project / picking your project and then the wanted quickstarter. Internally this is done thru a rest call to rundeck - with the picked job as parameter - [here](https://github.com/opendevstack/ods-project-quickstarters/tree/master/rundeck-jobs/quickstarts)
    - Bitbucket repository within the chosen project named `key`-`boilerplate name`
    - Openshift components based on the chosen boilerplate, coming from [ods-quickstarters](https://github.com/opendevstack/ods-project-quickstarters)

3. The involved people receive an email with the setup, URLs to components etc. - in case `mail.enabled == true` 

## Integration with Bitbucket (webhooks)

Next to the provision app creating the bitbucket repository for a chosen quickstarter - it also creates a webhook on that repo, which triggers on three events
```
    List<String> events = new ArrayList<String>();
        events.add("repo:refs_changed");
        events.add("pr:merged");
        events.add("pr:declined");
    webhook.setEvents(events);
```
This webhook calls the [webhook proxy](https://github.com/opendevstack/ods-core/tree/master/jenkins/webhook-proxy) which in turn creates an openshift `build config` of type `pipeline` in the `name`-cd project and executes it.

##  Permissions

By default no special permissions are set on either confluence / jira / bitbucket or openshift, only system-wide settings are inherited.

However there is a special knob to tighten security (which can be passed with the project input `createpermissionset : boolean`)  - based on three groups that need to be provided as part of the API call / from the userinterface.

1. admin group: admin rights on the generated projects / spaces / repositories
1. user group: read / write rights on the generated projects / spaces / repositories
1. readonly group: read rights on the generated projects / spaces / repositories

The configuration for the permission sets are configured:
1. JIRA Project is provisioned with its own permissionset [defined in src/main/resources/permission-templates/jira.permission.all.txt](src/main/resources/permission-templates/jira.permission.all.txt)
2. Confluence Project is provisioned with special permission set [defined in src/main/resources/permission-templates/confluence.permission.*](src/main/resources/permission-templates)
3. Bitbucket Project is provisioned with tight read & write roles
4. Openshift Project roles linked to the passed groups (`READONLY` - `view`, `ADMINGROUP` - `admin`, `USERS` - `edit`)

## Project/Space types based on templates
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
# list of templates surfaced to the UI and API
project.template.key.names=default,<name>
```

## Using the provision application via API / thru direct REST calls

``` bash 
PROVISION_API_HOST=<host name>

curl -D headers.txt -k -H "Content-Type: application/x-www-form-urlencoded" \
-X POST ${PROVISION_API_HOST}/j_security_check \
-d username=<username> -d password=<password>

# grab the login status, and exit if error
login_status=$(cat headers.txt | grep ${PROVISION_API_HOST}/login?error)

if [[ $login_status != "" ]]; then echo "Login Error"; exit 1; fi;

# grab the needed IDs and bake the cookies
JSESSION_ID=$(cat headers.txt | grep "Set-Cookie: JSESSION" | cut -d ';' -f1 | cut -d ":" -f2)";" 
CROWD_COOKIE=$(cat headers.txt | grep "Set-Cookie: crowd" | cut -d ';' -f1 | cut -d ":" -f2)

COOKIES=${JSESSION_ID}${CROWD_COOKIE}

# sample provision file >> create.txt
{
  "name" : "<Mandatory name>",
  "key" : "<Mandatory key>",
  "createpermissionset" : true,
  "jiraconfluencespace" : true,
  "admin" : "<admin user>",
  "adminGroup" : "<admin group>",
  "userGroup" : "<user group>",
  "readonlyGroup" : "<readonly group>",
  "openshiftproject" : false
}

provisionfile=create.txt

# invoke the provision API to create a new project
curl -k -X POST --cookie "$COOKIES" -d @"$provisionfile" \
-H "Content-Type: application/json; charset=utf-8" -v ${PROVISION_API_HOST}/api/v1/project
```

# Internal architecture

The Project is based on Spring Boot, using several technologies which can be seen in the [build.gradle](build.gradle).

The provision app is merely an orchestrator that does HTTP REST calls to Atlassian Crowd, Jira, Confluence, Bitbucket and
Rundeck (for openshift interaction).

The APIs exposed for direct usage, and also for the UI are in the [controller package](src/main/java/org/opendevstack/provision/controller). 
The connectors to the various tools to create resources are in the [services package](src/main/java/org/opendevstack/provision/services)

If you want to build / run locally - create `gradle.properties` in the project's root to configure connectivity to OpenDevStack NEXUS

    - nexus_url=<NEXUS HOST>
    - nexus_folder=candidates
    - nexus_user=<NEXUS USER>
    - nexus_pw=<NEXUS_PW> 
 
```bash
# to run the server execute
gradle bootRun
```

To overwrite the provided [application.properties](src/main/resources/application.properties) a configmap is created out of them and injected into /config/application.properties within the container.
The base configuration map as well as the deployment yamls can be found in [ocp-config](ocp-config/prov-app/cm.yml), and overwrite parameters from application.

## Frontend Code

The frontend is based on jquery and thymeleaf. All [posting to the API](src/main/resources/static/js/client.js) happens out of java script (client.js)
 
## Backend Code

The backend is based on Spring Boot, authenticates against Atlassian Crowd and exposes consumable APIs (`api/v1/project`). 
Storage of created projects happens on the filesystem thru the [StorageAdapter](src/main/java/org/opendevstack/provision/storage/LocalStorage.java).
Both frontend (html) and backend are tested thru Junit & Mockito
 
## Consuming REST APIs in Java

Generally this is a pain. To ease development, a few tools are in use:
- Jackson (see link below)
- OKTTP3 Client (see link below)
- jsonschema2pojo generator (see link below)

The process for new operations to be called is: 
1. Look up the API call that you intend to make
2. see if there is a JSON Schema available
3. Generate (a) Pojo('s) for the Endpoint
4. Use the pojo to build your request, convert it to JSON with Jackson and send it via OKHTTP3, and the Provision Application's [RestClient](src/main/java/org/opendevstack/provision/util/RestClient.java)
 
## Link collection

- [Mkyong spring boot + security + thymeleaf example](http://www.mkyong.com/spring-boot/spring-boot-spring-security-thymeleaf-example/)
- [Getting more Webjars](http://www.webjars.org/)
- [Generating POJOs from JSON Schemas](http://www.jsonschema2pojo.org/) very helpful for the Atlassian API Docs
- [OKHttp3](https://square.github.io/okhttp)
- [Mockito](https://site.mockito.org)
- [Jackson](https://github.com/FasterXML/jackson)

**Atlassian API's**

- [Jira API](https://docs.atlassian.com/jira/REST/server/#api/2/fullJiraProject-createProject)
- [Confluence API](https://docs.atlassian.com/ConfluenceServer/rest/6.12.1/)
- [Bitbucket API](https://developer.atlassian.com/server/bitbucket/reference/rest-api/)
- [Crowd API](https://developer.atlassian.com/server/crowd/crowd-rest-apis/)
- [Rundeck API](https://rundeck.org/docs/api/)

# FAQ:

1. Where is the provision app deployed?<BR>
A. the provision application is deployed on openshift, in both `prov-dev` and `prov-test`. `prov-dev` is the development environment in case you want to change / enhance the application, while the production version of the application is deployed in `prov-test`. The URL to get to the provision application, is defined thru a route. Ít's `https://prov-app-test.`<openshift application domains>.

1. Why are three Openshift projects created when I provision a new project?<br>
A: The `project-name`-dev & -test ones are runtime namespaces. Depending on which branch you merge / commit your code into, images will be built & deployed in one of the two (further information on how this is done - can be found in the [jenkins-shared-library](https://github.com/opendevstack/ods-jenkins-shared-library) <br> 
In contrast to this, the `project-name`-cd namespace hosts a project specific instance of the [ODS Jenkins](https://github.com/opendevstack/ods-core/tree/master/jenkins) and also of the [Webhook Proxy](https://github.com/opendevstack/ods-core/tree/master/jenkins/webhook-proxy). When a built is triggered, builder pods (=deployments of [Jenkins slaves](https://github.com/opendevstack/ods-project-quickstarters/tree/master/jenkins-slaves)) are created in this project.<BR>
This was a cautious design choice to give a project team as much power as possible - when it comes to configuration of jenkins.

1. What is `RUNDECK` used for?<br>
A: Rundeck is used as orchestration engine when the provision application triggers provision jobs (e.g. create new projects, create components). This architecture is *subject to change* likely in release 2.0, to dramatically reduce complexity in multi cluster scenarios.

1. Where do I find the logs, if something went wrong? <BR>
A. Within the Openshift `pod` of the provision app (in `project`dev/test, namely in `/opt/provision/history/logs` a logfile is created per `project`)
    
1. Where is the real configuration of the provision application? <BR>
A. The base configuration in the the `application.properties` in the codebase, the setup specific one is in a config map deployed within the `prov-dev/test` project.
