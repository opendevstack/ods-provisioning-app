# Changelog

## [Unreleased]

1. Provisioning app now supports named permission sets. One can provide role names and those are used in jira / confluence and bitbucket - rather than inheriting system wide role permissions [#19], [#40]
1. Extract all needed config params into config map injected into the DC as spring boot config
1. Move provisioning app to new shared library version (01-latest)
1. Mail sending can be now explicitely disabled [#47]
1. Bitbucket Project is only created if openshift == true, but a project can be upgraded later to openshift usage, if allowed by application.properties [#44]
1. Logfiles are per project now - located in /opt/provision/history/logs [#36]

1. Others (bugfixes)
   1. Several OC artifacts were missing so `tailor update` did not work
   1. Update gradle to 4.9 
   1. Wrong confluence & jira URL was generated and returned [#35]
   1. Special permission set bug with lowercase mixed project key [#46]
   1. IE as browser is not supported, show warning [#37]
   1. Occasional 504 timeout on provision app [#34]
   1. Error handling massively fixed to provide insight in what goes wrong - if it does [#38]

## [0.1.0] - 2018-07-27

Initial release.

[Unreleased]: https://github.com/opendevstack/ods-provisioning-app/compare/0.1.0...HEAD
