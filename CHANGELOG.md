# Changelog

## [Unreleased]

1. Provisioning app now supports named permission sets. One can provide user and roles - rather than inheriting system wide role permissions (#19)
1. Extract all needed config params into config map injected into the DC as spring boot config
1. Move provisioning app to new shared library version (#13)

1. Others (bugfixes)
   1. Several OC artifacts were missing so tailor update did not work
   1. Update gradle to 4.9 (#13) 

## [0.1.0] - 2018-07-27

Initial release.

[Unreleased]: https://github.com/opendevstack/ods-provisioning-app/compare/0.1.0...HEAD
