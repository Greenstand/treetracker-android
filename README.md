# TreeTracker Android

This is the Android app for the TreeTracker open source project (www.treetracker.org). 
This project coordinates tree planting employment for people living in extreme poverty.
The Android segment allows people to track and verify reforestation plantings,
paying planters on a per planting basis.

## Deployment

There is one prerequisite to using the appropriate gradle tasks:

1) Placing the relevant keys.json from the PlayStore in the ./app folder [example here](https://docs.fastlane.tools/getting-started/android/setup/#collect-your-google-credentials)

Once this is done, you can then proceed by running one of the following tasks to run the release:

* `bootstrapReleasePlayResources` | Downloads the play store listing for the Release build. No download of image resources. See #18.
* `generateReleasePlayResources`  | Collects play store resources for the Release build
* `publishListingRelease`         | Updates the play store listing for the Release build

## Contributing

Please review the roadmap (https://github.com/Greenstand/treetracker.org/wiki/Roadmap) or issue tracker here on github.
All contributions should be submitted as pull requests against the master branch at https://github.com/Greenstand/treetracker-android/
