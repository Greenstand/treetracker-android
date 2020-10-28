fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android test
```
fastlane android test
```
Runs all the tests
### android bump_version_code_and_push
```
fastlane android bump_version_code_and_push
```
Bump version code and push
### android firebase_beta
```
fastlane android firebase_beta
```
Release a build on Firebase Beta
### android beta_release
```
fastlane android beta_release
```
Job for Travis to Submit a new Greenstand Beta Build to Crashlytics Beta
### android justdiggit_beta
```
fastlane android justdiggit_beta
```
Submit a new JustDigIt Beta Build to Crashlytics Beta
### android deploy
```
fastlane android deploy
```
Deploy a new version to the Google Play
### android bump_version_code
```
fastlane android bump_version_code
```
Bump version code

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
