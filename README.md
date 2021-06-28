![Build Status](https://github.com/Greenstand/treetracker-android/workflows/Treetracker%20Android%20App%20CI/badge.svg?branch=master)

# Treetracker Android

## Current Milestones and Issue Topics

### Next feature release

Feature we are currently prioritizing
https://github.com/Greenstand/treetracker-android/milestone/17

&nbsp;
&nbsp;

## Project Details

This is the Android app for the TreeTracker open source project (www.treetracker.org). 
This project coordinates tree planting employment for people living in extreme poverty.
The Android segment allows people to track and verify reforestation plantings,
paying planters on a per planting basis.

For more on design intent and the app's user story see the [wiki in this repository](https://github.com/Greenstand/treetracker-android/wiki/User-Story)

&nbsp;
&nbsp;

## Project Setup
Developers will need to ask the #android channel in Slack for the treetracker.keys.properties file to build gradle for the application.

For development, select the build variant _dev_. This build variant is configured to allow trees to be added without a specific accuracy.  

&nbsp;
&nbsp;

## QC Deployment

Note: QC deployment pipeline are about to be changed and moved to Github actions from Travis. The 
following details will be updated once the change is made.

This repo has been configured to be easily deployed to QC through Travis. The process, however, is still a manual.

In order to get a new build to QC, take the following steps:

1. Go to the [Project page in Travis](https://travis-ci.com/Greenstand/treetracker-android)
2. Select _More Options > Trigger build_
3. On the dialog, select the appropriate branch, and use either of these configurations:
    * For an Android Beta build:
```
script:
  - "fastlane android beta"
```
    * For an Android JustDigIt build:
```
script:
  - "fastlane android justdiggit_beta"
```	
4. Run the build and then wait for it to complete. _Voilá._

Running without a `script` custom parameter will result in a standard build to be run without any artifacts deployed.

### Fastlane

Fastlane must be installed using
bundle install --path vendor/bundle
fastlane install_plugins
firebase login

otherwise the firebase plugin will not work



&nbsp;
&nbsp;

## Deployment

There is one prerequisite to using the appropriate gradle tasks:

1) Placing the relevant keys.json from the PlayStore in the ./app folder [example here](https://docs.fastlane.tools/getting-started/android/setup/#collect-your-google-credentials)

Once this is done, you can proceed by running one of the following tasks to run the release:

* `bootstrapReleasePlayResources` | Downloads the play store listing for the Release build. No download of image resources. See #18.
* `generateReleasePlayResources`  | Collects play store resources for the Release build
* `publishListingRelease`         | Updates the play store listing for the Release build

&nbsp;
&nbsp;

## Contributing

 See [Contributing in the Development-Overview README](https://github.com/Greenstand/Development-Overview/blob/master/README.md)

Review the project board for current priorities [Android Project](https://github.com/orgs/Greenstand/projects/5)

Please review the [issue tracker](https://github.com/Greenstand/treetracker-android/issues) here on this github repository 

Check out the cool [roadmap](https://github.com/Greenstand/Development-Overview/blob/master/Roadmap.md)

All contributions should be submitted as pull requests against the master branch in this github repository. https://github.com/Greenstand/treetracker-android/
