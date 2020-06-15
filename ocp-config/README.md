# OCP Configuration

This directory contains the necessary resources to bootstrap the provisioning application in an existing openshift namespace.
From ODS version 3 the provision app will run after the default installation in the `ods` namespace.

To create the provisioning app in an existing `foo` namespace, copy the existing `ods-provisioning-app.env.sample` to a new file `ods-provisioning-app.env`.
After adapting the example values you can apply the configuration via:

```
tailor update -n foo
```
