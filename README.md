# PoorAuth

A deliberately bad login plugin for a joke server. Do not use with real accounts.

## Build locally

    mvn package

Then copy target/poorauth-1.0.jar into your server's plugins folder.

## Build on GitHub

Push this repo to GitHub. The workflow in .github/workflows/build.yml builds the jar on every push and pull request. Download it from the Actions tab under the run's Artifacts section (named "poorauth").
