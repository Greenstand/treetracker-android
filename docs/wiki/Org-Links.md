# Org Links

 * Deciders: Jonathan, Zaven
 * Date: 2022-07-7

## Context
When using the Treetracker app to plant trees, users need to input their org each app session. By associating an org with their app session they're able to then have their trees tied to that given organization. This is the only functionality in the app related to orgs, and it's a temporary solution until org links are added.

Org links are web links that will open the app when clicked, and automatically customize the app in ways that the organization desires. An example of this is changing the color theme, adding extra tracking features for the trees, custom icons, etc... In addition to this customization, it will also add the org as an option for the planter to select. 

If another org link is used, it will again customize the app. If a planter wishes to work for the previous org, instead of clicking the link again, they will be able to select the org from the org screen. Upon selection of the different org from this screen, the app will refresh it's theme/icons and tree capture flow.

If the device if offline when this happens, then assets that must be downloaded will have placeholders until they can be downloaded to the device. An example of this is the Greenstand logo being replaced with something else.

This org link should work offline outside of the need to download assets. This means that all information must be encoded into the link itself. The app will parse the link, extracting pre-made customization options. For instance, the link can tell the app to include a tree height capture screen. This screen will already be in the app, the link will simply tell the app to enable it. The link will also dictate the tree capture flow.

For the UI of this feature, the org input screen will be removed and replaced with a org selection screen (much like the language selection screen). This screen will provide the different orgs that you can select. (after they are added by clicking on an external link). A user cannot add an org manually, it can only be done via links. To access this org selection screen a button will be added to the dashboard on the top left corner. It will be in the same style as the language button, but will take the user to the org screen.


## Problem Statement
Different planting organizations want custom features in the app which is hard to maintain through different app flavors. On top of this, planters need to input the correct org when planting for the plants to be associated with the given org. If this step is done incorrectly, it makes extra work on the backend to resolve this.


## Decision Drivers 
* Moving away from App Flavors, which make testing and maintenance hard
* Ensure that planters can no longer input an incorrect organization name into the app
* Allow easy switching between different organizations


## Considered Options
