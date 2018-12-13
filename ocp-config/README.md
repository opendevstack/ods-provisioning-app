# OCP Configuration

There are two parts to OCP config: the CD environment (`prov-cd`) and the app
environments (`prov-dev` and `prov-test`).

To apply the OCP config for `prov-cd`:
```
cd prov-cd
tailor update
```

To apply the OCP config for `prov-dev`:
```
cd prov-app
tailor update -f Tailorfile.dev
```

To apply the OCP config for `prov-test`:
```
cd prov-app
tailor update -f Tailorfile.test
```
