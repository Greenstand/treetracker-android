![Build Status](https://github.com/Greenstand/treetracker-android/workflows/Treetracker%20Android%20App%20CI/badge.svg?branch=master)

# Treetracker Android for FCC

## Current Milestones and Issue Topics

### Next feature release

Feature we are currently prioritizing
https://github.com/orgs/Greenstand/projects/109/views/2

&nbsp;
&nbsp;

## Project Details

This is the Android app for Greenstands Treetracker open source project (www.treetracker.org). 
This project coordinates the digital capture of tree growth data in the field, allowing to establish employment for people living in extreme poverty based on tree planting.
The Android segment is the data collection tool that transports the information into the next service through a data pipeline towards the veryification service.

For more on design intent and the app's user story see the [wiki in this repository](https://github.com/Greenstand/treetracker-android/wiki/User-Story)

&nbsp;
&nbsp;

## Project Setup
Developers will need to ask the #android_chat channel in Slack for the treetracker.keys.properties file to build gradle for the application.

For development, select the build variant _dev_. This build variant is configured to allow trees to be added without a specific accuracy.  

&nbsp;

## QC Deployment

To join the Greenstand Testers for this app follow this [testing invite link](https://appdistribution.firebase.dev/i/f98b34cc1ff2c0b7) and add your mail used on your android device
Note: QC deployment pipeline are on Github Actions.

### CREATING INTERNAL RELEASES MANUALLY
To create an internal release manually on firebase, 
Increase the version code by 1(Optionally you can change the version name if it's a major release)
Change the build variant to 'beta'  
Generate an unsigned apk or aab in the above variant,
On firebase console, go to firebase distribution, switch the app to 'Treetracker Test' 
Upload the apk or aab generated above, Select Greenstand Testers and complete it.

### AUTOMATED INTERNAL RELEASES WITH GITHUB ACTIONS(CURRENTLY NOT AVAILABLE)
Click on Actions in this [repo](https://github.com/Greenstand/treetracker-android.git)  
Select Release Beta and run workflow
Select Branch you want to create release for and run. This would create a new release on Firebase and also send messages on slack to members that a new release has been created.













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

Review the project board for current priorities [Android Project](https://github.com/orgs/Greenstand/projects/109)

Please review the [issue tracker](https://github.com/Greenstand/treetracker-android/issues) here on this github repository 

Check out the cool [roadmap](https://github.com/Greenstand/Development-Overview/blob/master/Roadmap.md)

All contributions should be submitted as pull requests against the master branch in this github repository. https://github.com/Greenstand/treetracker-android/
