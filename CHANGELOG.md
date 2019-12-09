# Changelog

## Unreleased
### Added
- Replace Rundeck with (Jenkins) pipelines ([#265](https://github.com/opendevstack/ods-provisioning-app/pull/265))
- Added support for project specific CD user ([#297](https://github.com/opendevstack/ods-provisioning-app/pull/297))

### Changed
- Create service framework to allow integration to other bugtrackers/scm & idmgt ([#86](https://github.com/opendevstack/ods-provisioning-app/issues/86)
- Uptake 4.1.0 okhttp ([#303](https://github.com/opendevstack/ods-provisioning-app/issues/303))
- Bump httpasyncclient from 4.0-beta3-atlassian-1 to 4.1.4  ([#205](https://github.com/opendevstack/ods-provisioning-app/issues/205))

### Fixed
- App wants to create jira components for project without bugtracker space ([#292](https://github.com/opendevstack/ods-provisioning-app/issues/292))
- Provisioning is allowed even when Openshift project is not created ([#195](https://github.com/opendevstack/ods-provisioning-app/issues/195))
- Add gitattributes for CRLF handling ([#258](https://github.com/opendevstack/ods-provisioning-app/issues/258))
- Provisioning app fails if Active directory groups does not exists. ([#192](https://github.com/opendevstack/ods-provisioning-app/issues/192))
- JIRA components are (wrongly) created for auxiliary repositories ([#255](https://github.com/opendevstack/ods-provisioning-app/issues/255))
- Provisioning app fails to clean up failed provisioning attempt ([#267](https://github.com/opendevstack/ods-provisioning-app/issues/267))
- Random error while creating projects - reason "closed" ([#264](https://github.com/opendevstack/ods-provisioning-app/issues/264))

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
