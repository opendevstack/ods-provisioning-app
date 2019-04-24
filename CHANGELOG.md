# Changelog

## [Unreleased]

### Added
- Define templates & allow pick of project templates for jira / confluence projects ([#26](https://github.com/opendevstack/ods-provisioning-app/issues/26))
- With `special permissionset` set true and openshift project to be created, 
groups are now passed to openshift and set with appropriate rights (view / edit / admin)
- Add kanban project template - based on `com.pyxis.greenhopper.jira:gh-kanban-template` ([#109](https://github.com/opendevstack/ods-provisioning-app/issues/109))
 
### Fixed
- Upon successfull creation of an initiative, the new project does not show up in modify list ([#96](https://github.com/opendevstack/ods-provisioning-app/issues/96))

### Changed
- Refactoring of http call logic - centralized in RestClient now ([#84](https://github.com/opendevstack/ods-provisioning-app/issues/84))

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
