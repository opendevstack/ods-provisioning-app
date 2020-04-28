# OCP Configuration

This directory contains the necessary resources to bootstrap the provisioning application in an existing openshift namespace.
From ODS version 3 the provision app will run in the ods namespace.

To apply the OCP config for `prov-dev`:
```
tailor update -n prov-dev
```

To apply the OCP config for `prov-test`:
```
tailor update -n prov-test
```
