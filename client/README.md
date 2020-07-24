# ODS Provisioning App Frontend

## Status

This first version of the new frontend stack was introduced as an experimental feature within OpenDevStack 3.x ([original issue](https://github.com/opendevstack/ods-provisioning-app/issues/518)). It can be activated for users with a `frontend.spa.enabled` feature flag in Spring Boot.

OpenDevStack NEXT will include this new frontend activated by default (without featue flag).

To contribute to the further development, see section "Contributions welcome!" further down.

## Stack

- [Angular](https://angular.io/) as single page application framework
- [Jest](https://jestjs.io/) as unit testing framework
- [Prettier](https://prettier.io/) as pre-commit-hook
- [SASS Lint](https://github.com/sasstools/sass-lint) to check scss files
- [Yarn](https://yarnpkg.com/) as package manager

See package.json for up-to-date version information.

## Getting started

### 1. Install dependencies

- Install YARN globally: `npm install -g yarn`
- Run `yarn`, this will install all dependencies

### 2. Configure API access from localhost

The easiest way to run the Provisioning App frontend locally and using real data is to configure an API proxy:

Copy the content from `proxy.conf.template.json` into a new `proxy.conf.json` file (which is gitignored) and adapt it to your needs.

Doing this there's no need to run Spring Boot locally.

### 3. Test the setup

Run `yarn start:dev` and when it succeeded open `http://localhost:4200` in your browser. You should see the Angular app starting. The app will automatically reload if you change any of the source files.

## Development

### Dev Server

`yarn start:dev`: Starts a live reload dev server on `http://localhost:4200/`

### Build

`yarn build`: The build artifacts will be stored in the `dist/` directory. `yarn build:prod` will be used for the pipeline.

### Tests

- Unittests: `yarn test` / `yarn test:watch`
- E2E (Cypress): TODO

### Linting

- `yarn lint`: Linting for Typescript and SCSS files.

### Prettier

Prettier is used as a pre-commit hook to format js, json, md, ts files. Formatting of html files has been temporarily removed due to inflexibility in context of Angular component markup.

## Contributions welcome!

We're happy if you'd like to contribute to the further development of the new frontend client. Feel free to check the [existing issues tagged with "frontend-spa"](https://github.com/opendevstack/ods-provisioning-app/labels/frontend-spa) or add a new issue there.

## Usage of Angular CLI

`ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
