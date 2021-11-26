# Changelog

## Unreleased

## [4.0] - 2021-18-11

### Added

- SPA dependencies update incl. Angular 12 ([#692](https://github.com/opendevstack/ods-provisioning-app/issues/692))
- Add swagger-ui support ([#679](https://github.com/opendevstack/ods-provisioning-app/pull/679))
- Handle logout in SPA ([#675](https://github.com/opendevstack/ods-provisioning-app/issues/675))
- Handle form based auth in SPA ([#637](https://github.com/opendevstack/ods-provisioning-app/issues/637))
- Add default permissions to project groups on project creation ([#636](https://github.com/opendevstack/ods-provisioning-app/pull/636))
- Add support for odsbox for local development([#579](https://github.com/opendevstack/ods-provisioning-app/issues/579))
- Setup webhook in jira projects on project creation event ([#452](https://github.com/opendevstack/ods-provisioning-app/issues/452))
- Parameterise jira project type templates ([#404](https://github.com/opendevstack/ods-provisioning-app/issues/404))
- Provision app should support reuse of shared schemes for Jira & not create permission schemes every time ([#151](https://github.com/opendevstack/ods-provisioning-app/issues/151))
- Add changelog enforcer as GitHub Action to workflow ([#657](https://github.com/opendevstack/ods-provisioning-app/issues/657))
- Add a configurable ui disclaimer to be set with properties ([#706](https://github.com/opendevstack/ods-provisioning-app/issues/706)) 

### Fixed

- Bump ws from 6.2.1 to 6.2.2 in /client ([#689](https://github.com/opendevstack/ods-provisioning-app/pull/689))
- Bump dns-packet from 1.3.1 to 1.3.4 in /client ([#688](https://github.com/opendevstack/ods-provisioning-app/pull/688))
- Bump elliptic from 6.5.3 to 6.5.4 in /client ([#687](https://github.com/opendevstack/ods-provisioning-app/pull/687))
- Bump browserslist from 4.11.0 to 4.16.6 in /client ([#686](https://github.com/opendevstack/ods-provisioning-app/pull/686))
- Bump lodash from 4.17.19 to 4.17.21 in /client ([#685](https://github.com/opendevstack/ods-provisioning-app/pull/685))
- Bump url-parse from 1.4.7 to 1.5.1 in /client ([#684](https://github.com/opendevstack/ods-provisioning-app/pull/684))
- Bump hosted-git-info from 2.8.8 to 2.8.9 in /client ([#683](https://github.com/opendevstack/ods-provisioning-app/pull/683))
- Bump y18n from 4.0.0 to 4.0.1 in /client ([#680](https://github.com/opendevstack/ods-provisioning-app/pull/680))
- Handle basic auth in SPA ([#637](https://github.com/opendevstack/ods-provisioning-app/issues/637))
- Fix SPA searchbar layout ([#664](https://github.com/opendevstack/ods-provisioning-app/issues/664))
- Wrong exception logging of bitbucket project key pre flight check ([#655](https://github.com/opendevstack/ods-provisioning-app/issues/655))
- Show an error message when the component id does not match the regex expression ([#624](https://github.com/opendevstack/ods-provisioning-app/issues/624))
- Set confluence shortcut in jira even if confluence server is not activated ([#613](https://github.com/opendevstack/ods-provisioning-app/issues/613))
- Missing verification of confluence project already exists on project creation event ([#607](https://github.com/opendevstack/ods-provisioning-app/issues/607))
- Jira project key check logs exception on project creation ([#605](https://github.com/opendevstack/ods-provisioning-app/issues/605))
- Missing verification of bitbucket project already exists on project creation event ([#600](https://github.com/opendevstack/ods-provisioning-app/issues/600))
- Github build failing due to git bad object ([#595](https://github.com/opendevstack/ods-provisioning-app/issues/595))
- Missing unix basic commands on prov-app after deploying ODS 3 ([#588](https://github.com/opendevstack/ods-provisioning-app/issues/588))
- Improve authorization of quickstarter endpoint ([#572](https://github.com/opendevstack/ods-provisioning-app/issues/572))
- Unknown exception (e.g. existing JIRA project) raised in REST create project endpoint / addProject causes removal of existing projects ([#514](https://github.com/opendevstack/ods-provisioning-app/issues/514))
- Logging in debug level shows too much jwt details ([#486](https://github.com/opendevstack/ods-provisioning-app/issues/486))
- DELETE_COMPONENTS API stores and returns project with deleted quickstarter([#702](https://github.com/opendevstack/ods-provisioning-app/issues/702))
- API DELETE*: wrong jenkins run job (lastExecutionJobs) returned ([#790](https://github.com/opendevstack/ods-provisioning-app/issues/790))

## [3.0] - 2020-08-11

### Added

- Provide modern SPA user interface to improve UX ([#518](https://github.com/opendevstack/ods-provisioning-app/issues/518))
- Release the provisioning app as docker image ([#337](https://github.com/opendevstack/ods-provisioning-app/issues/337))
- add provision quickstarter sample script to the documentation ([#513](https://github.com/opendevstack/ods-provisioning-app/issues/513))
- add basic auth and pre flight checks documentation ([#509](https://github.com/opendevstack/ods-provisioning-app/issues/509))
- enable basic auth beside crowd authentication for easy endpoint consumption ([#504](https://github.com/opendevstack/ods-provisioning-app/issues/504))
- application info REST endpoint ([#490](https://github.com/opendevstack/ods-provisioning-app/issues/490))
- Add error codes to the preconditions check response ([#479](https://github.com/opendevstack/ods-provisioning-app/issues/479))
- Support BasicAuth beside OAuth2 OIDC at runtime ([#376](https://github.com/opendevstack/ods-provisioning-app/issues/376))
- Add building and pushing the docker image to CI workflow ([#419](https://github.com/opendevstack/ods-provisioning-app/pull/419))
- Enable Azure AD (OAuth2 OIDC) spring boot support ([#424](https://github.com/opendevstack/ods-provisioning-app/issues/424))
- prov-app preflight check ([#330](https://github.com/opendevstack/ods-provisioning-app/issues/330))
- Provide get all project summary endpoint ([#405](https://github.com/opendevstack/ods-provisioning-app/issues/405))
- Add e2e-spock-geb Quickstarter to ProvApp CM ([#366](https://github.com/opendevstack/ods-provisioning-app/issues/366))

### Changed

- Disable cleanup of incomplete projects by default ([#507](https://github.com/opendevstack/ods-provisioning-app/issues/507))
- Protect API operations better ([#172](https://github.com/opendevstack/ods-provisioning-app/issues/172))
- enable precondition checks for project provision by default ([#489](https://github.com/opendevstack/ods-provisioning-app/issues/489))
- reuse application properties from src/main/resources in openshift to simplify configuration ([#446](https://github.com/opendevstack/ods-provisioning-app/issues/446))
- Documentation of quickstarter configuration ([#338](https://github.com/opendevstack/ods-provisioning-app/issues/338))
- Separate quickstarter config from application.properties ([#363](https://github.com/opendevstack/ods-provisioning-app/issues/363))
- Uptake of new shared library for provision app - and harmonization of env mapping ([#415](https://github.com/opendevstack/ods-provisioning-app/issues/415))
- Set default branch to master instead of production ([#435](https://github.com/opendevstack/ods-provisioning-app/pull/435))
- Improve configuration of readable repositories ([#412](https://github.com/opendevstack/ods-provisioning-app/issues/412))
- Logout from identity manager should be optional ([#365](https://github.com/opendevstack/ods-provisioning-app/issues/365))
- OCP templates should point to latest tag ([#331](https://github.com/opendevstack/ods-provisioning-app/issues/331))

### Fixed

- delete components api does not remove deleted components from projects storage ([#547](https://github.com/opendevstack/ods-provisioning-app/issues/547))
- numbers in project name causes project provision to fail ([#495](https://github.com/opendevstack/ods-provisioning-app/issues/495))
- underscore in component name causes provision failure ([#465](https://github.com/opendevstack/ods-provisioning-app/issues/465))
- webhook proxy delete-component POST request contains wrong secret ([#474](https://github.com/opendevstack/ods-provisioning-app/issues/474))
- pre condition checks do not ignore case for groups and username checks ([#475](https://github.com/opendevstack/ods-provisioning-app/issues/475))
- ODS Project Provision does not add links to project - because of missing project permissions / roles ([#403](https://github.com/opendevstack/ods-provisioning-app/issues/403))
- project templates key endpoint do not return body in json format ([#460](https://github.com/opendevstack/ods-provisioning-app/issues/460))
- wrong username is displayed in about view when OAuth2 profile is enabled ([#444](https://github.com/opendevstack/ods-provisioning-app/issues/444))
- Jira user preflight check needs to be configurable ([#436](https://github.com/opendevstack/ods-provisioning-app/issues/436))
- Jenkins create project job uses env.BITBUCKET_HOST and not configured application.properties' one ([#407](https://github.com/opendevstack/ods-provisioning-app/issues/407))
- Case sensitive role extraction from jwt fails to map user roles due to case sensitive role names ([#374](https://github.com/opendevstack/ods-provisioning-app/issues/374))
- DeploymentConfig has badly configured resource constraints ([#396](https://github.com/opendevstack/ods-provisioning-app/issues/396))
- logback configuration does not default to old hardcoded file path ([#384](https://github.com/opendevstack/ods-provisioning-app/issues/384))
- Provision app removes existing project (when existing name / key is passed to "addProject" API) ([#345](https://github.com/opendevstack/ods-provisioning-app/issues/345))
- Webhook Proxy does not run under jenkins serviceaccount ([#340](https://github.com/opendevstack/ods-provisioning-app/issues/340))
- User id extraction from JWT is hardcoded to name ([#375](https://github.com/opendevstack/ods-provisioning-app/issues/375))
- Maps jwt roles to lowercase by default ([#400](https://github.com/opendevstack/ods-provisioning-app/pull/400))
- Enables e2e spock quickstarter ([#388](https://github.com/opendevstack/ods-provisioning-app/pull/388))
- Adds default value for logback file path ([#385](https://github.com/opendevstack/ods-provisioning-app/pull/385))
- Adds option to disable logout from identity manager ([#372](https://github.com/opendevstack/ods-provisioning-app/pull/372))
- Fixes hardcoded logback file path and failed test when running locally ([#361](https://github.com/opendevstack/ods-provisioning-app/pull/361))
- Provisioning of quickstarters fails if `pod name` label is longer than 63 characters ([#335](https://github.com/opendevstack/ods-provisioning-app/pull/335))
- Bug existing project cleanup ([#347](https://github.com/opendevstack/ods-provisioning-app/pull/347))
- Adds component id and component type validation rules and shortens pipeline execution url length ([#344](https://github.com/opendevstack/ods-provisioning-app/pull/344))
- Run `webhook-proxy` under `jenkins` serviceaccount ([#341](https://github.com/opendevstack/ods-provisioning-app/pull/341))
- Use image tag `latest` for prov-app DC ([#332](https://github.com/opendevstack/ods-provisioning-app/pull/332))

## [2.0] - 2019-12-13

### Added

- Replace Rundeck with (Jenkins) pipelines ([#265](https://github.com/opendevstack/ods-provisioning-app/pull/265))
- Added support for project specific CD user ([#297](https://github.com/opendevstack/ods-provisioning-app/pull/297))
- Create project specific trigger secret ([#317](https://github.com/opendevstack/ods-provisioning-app/issues/317))
- Order "Select existing project" drop-down ([#299](https://github.com/opendevstack/ods-provisioning-app/issues/299))

### Changed

- Create service framework to allow integration to other bugtrackers/scm & idmgt ([#86](https://github.com/opendevstack/ods-provisioning-app/issues/86))
- Uptake 4.1.0 okhttp ([#303](https://github.com/opendevstack/ods-provisioning-app/issues/303))
- Bump httpasyncclient from 4.0-beta3-atlassian-1 to 4.1.4 ([#205](https://github.com/opendevstack/ods-provisioning-app/issues/205))
- Upgrade Spring to 2.2.2 ([#319](https://github.com/opendevstack/ods-provisioning-app/pull/319))
- Configure image tag and Git ref for production pipeline ([#324](https://github.com/opendevstack/ods-provisioning-app/pull/324))

### Fixed

- App wants to create jira components for project without bugtracker space ([#292](https://github.com/opendevstack/ods-provisioning-app/issues/292))
- Provisioning is allowed even when Openshift project is not created ([#195](https://github.com/opendevstack/ods-provisioning-app/issues/195))
- Add gitattributes for CRLF handling ([#258](https://github.com/opendevstack/ods-provisioning-app/issues/258))
- Provisioning app fails if Active directory groups does not exists. ([#192](https://github.com/opendevstack/ods-provisioning-app/issues/192))
- JIRA components are (wrongly) created for auxiliary repositories ([#255](https://github.com/opendevstack/ods-provisioning-app/issues/255))
- Provisioning app fails to clean up failed provisioning attempt ([#267](https://github.com/opendevstack/ods-provisioning-app/issues/267))
- Random error while creating projects - reason "closed" ([#264](https://github.com/opendevstack/ods-provisioning-app/issues/264))
- Session timeout issues ([#318](https://github.com/opendevstack/ods-provisioning-app/issues/318))
- Quickstarters are not populated due to Rundeck session issues ([#293](https://github.com/opendevstack/ods-provisioning-app/issues/293))

## [1.2.0] - 2019-10-10

### Added

- Provision app should create jira component when new boilerplate component is provisioned ([#147](https://github.com/opendevstack/ods-provisioning-app/issues/147))
- Provision app code does not follow google java conventions as mandated in PMC ([#167](https://github.com/opendevstack/ods-provisioning-app/issues/167))
- Provision application only writes projectdata for further use in case no errors occur during provision ([#157](https://github.com/opendevstack/ods-provisioning-app/issues/157))
- Provision app fails to save state of a project in case of (another) project descriptor being corrupted ([#171](https://github.com/opendevstack/ods-provisioning-app/issues/171))
- Add option to authenticate via oauth2 instead of crowd. ([#170](https://github.com/opendevstack/ods-provisioning-app/issues/170))
- Feature/webjars dependency agnostic([#235](https://github.com/opendevstack/ods-provisioning-app/issues/235))
- Spotless: Enforce formatting ([#185](https://github.com/opendevstack/ods-provisioning-app/issues/185))
- Bugfix for checking against existing projects and addition of jira component creation -> master ([#148](https://github.com/opendevstack/ods-provisioning-app/issues/148))
- Service adapter framework ([#164](https://github.com/opendevstack/ods-provisioning-app/issues/164))

### Fixed

- Prov app fails when another Rundeck Job is stil running (api.error.execution.conflict)([#145](https://github.com/opendevstack/ods-provisioning-app/issues/145))
- Latest master does not load provision endpoint ([#239](https://github.com/opendevstack/ods-provisioning-app/issues/239))
- Antorra docs need to be updated with latest master ([#245](https://github.com/opendevstack/ods-provisioning-app/issues/245))
- getProject is returning always a project even if the requested project does not exist ([#174](https://github.com/opendevstack/ods-provisioning-app/issues/174))

### Changed

- Add service framework to hook in different adapter implementations instead of the provided ones ([#86](https://github.com/opendevstack/ods-provisioning-app/issues/86))
- Webjars dependencies should be version agnostic([#233](https://github.com/opendevstack/ods-provisioning-app/issues/233))

## [1.1.0] - 2019-05-28

### Added

- Define templates & allow pick of project templates for jira / confluence projects ([#26](https://github.com/opendevstack/ods-provisioning-app/issues/26))
- With `special permissionset` set to `true` and openshift project to be created,
  groups are now passed to openshift and set with appropriate rights (view / edit / admin) ([#112](https://github.com/opendevstack/ods-project-quickstarters/issues/112))
- Add kanban project template - based on `com.pyxis.greenhopper.jira:gh-kanban-template` ([#109](https://github.com/opendevstack/ods-provisioning-app/issues/109))

### Fixed

- Upon successful creation of an initiative, the new project does not show up in `modify initiative` list without a deep refresh ([#94](https://github.com/opendevstack/ods-provisioning-app/issues/94))
- Severe performance degregation during login based on amount of groups configured in crowd ([#106](https://github.com/opendevstack/ods-provisioning-app/issues/106))
- NON SSO crowd setup (e.g. SAML enabled) breaks provision app ([#131](https://github.com/opendevstack/ods-provisioning-app/issues/131))
- Without (deep) refreshing page newly created initiative is not showing up in picklist on "modify initiative" ([#94](https://github.com/opendevstack/ods-provisioning-app/issues/94))
- Rundeck integration returns "auth successful" - although login failed (e.g. timeout .. ) ([#126](https://github.com/opendevstack/ods-provisioning-app/issues/126))
- Modify initiative should show project key and name ([#121](https://github.com/opendevstack/ods-provisioning-app/issues/121))

### Changed

- Refactoring of repeated http call logic - centralized in [RestClient](https://github.com/opendevstack/ods-provisioning-app/blob/master/src/main/java/org/opendevstack/provision/util/RestClient.java) ([#84](https://github.com/opendevstack/ods-provisioning-app/issues/84))
- Upgrade of provision application to spring boot `2.1.4` ([#
  ](https://github.com/opendevstack/ods-provisioning-app/issues/119))

## [1.0.2] - 2019-04-02

### Fixed

- Default notification scheme (ID 10000) now attached to a provisioned JIRA project ([#90](https://github.com/opendevstack/ods-provisioning-app/issues/90))
- Attachment permission was missing for all groups ([#78](https://github.com/opendevstack/ods-provisioning-app/issues/78))
- Browse_project permission was missing for readonly groups ([#85](https://github.com/opendevstack/ods-provisioning-app/issues/85))
- Tighten bitbucket project rights with `special permissionset` ([#87](https://github.com/opendevstack/ods-provisioning-app/issues/87))

## [1.0.1] - 2019-01-25

### Changed

- Limit description to 100 characters ([#70](https://github.com/opendevstack/ods-provisioning-app/pull/70))

### Fixed

- In case of special permission set, do not create OpenShift projects with current user as admin ([#73](https://github.com/opendevstack/ods-provisioning-app/pull/73))

## [1.0.0] - 2018-12-03

### Added

- Support for named permission sets. One can provide role names and those are used in jira / confluence and bitbucket - rather than inheriting system wide role permissions (#19, #40)
- Mail sending can be now explicitely disabled (#47)
- Bitbucket Project is only created if `openshift == true`, but a project can be upgraded later to openshift usage, if allowed by application.properties (#44)
- Logfiles are per project now - located in /opt/provision/history/logs (#36)

### Changed

- Extract all needed config params into config map injected into the DC as spring boot config
- Move provisioning app to latest shared library version
- Update gradle to 4.9
- IE as browser is not supported, show warning (#37)

### Fixed

- Several OC artifacts were missing so `tailor update` did not work
- Wrong confluence & jira URL was generated and returned (#35)
- Special permission set bug with lowercase mixed project key (#46)
- Occasional 504 timeout on provision app (#34)
- Error handling massively fixed to provide insight in what goes wrong - if it does (#38)

## [0.1.0] - 2018-07-27

Initial release.
