## <a name="Overview"/> Overview

Greenstand's Treetracker mobile App aims to mitigate climate change and address issues of poverty by creating economic opportunity in planting, growing and maintaining trees. 

The application is being developed to enable tracking tree growth by taking pictures to "capture" the environmental state. These "Captures are uploaded to the Greenstand system where its associated environmental and social impact can be analyzed, calculated, and traded as a digital token. 

As of Jan 2026, the Tree Tracker App has registered 8 million Captures from 56 different countries and help the creation of thousands of jobs.

## <a name="Intended Use Cases"/>

The model provides transparency for growers (individuals and organizational) by enabling claims for "impact" and "impact ownership" claims for trees as they grow. It is used by organizations paying people to grow and used by individual. The Tree Tacker provides visual verification that enables a higher trust. Most users use this data to raise funds for their work.

Ultimate goal is for users to earn incomes by tending to trees and collecting more captures to receive the monthly payment.
  
## <a name="Intended Users"/> Intended Users

The target users are those living near or close to the extreme poverty line in equatorial regions of the global south.  

There are three groups of target users. 

#### 1. Organizational Trackers, or any entity coordinating large numbers of planters and trees.

This instance will often have multiple users attached to an organizational account, which is used for verification and planter management. 
These users work directly for organizations and tend to only track for that organization. Organizations typically want all "their users" data to flow directly to them. (Note: Organizational managers tend to interact with the cloud based tools, not the mobile app)
Hallmarks of these users are: 
- Generally supervised and interact with the organization
- Semi-savvy smartphone users
- Often tracking thousands of trees in a day.  
- Their trees are sent directly to the Organizational level admin panel. 

#### 2. Village Coordinators or Independent Trackers. These are individuals who are entering the global restoration economy. They may or may not be directly responsible for the impact they help track. 
These users can act as data collectors, supporting individual growers with registering and tracking. They are generally always looking for a way to sell their tree captures.

This Group tends to
- Use What's app as the primary source of communications 
- Have a compatible smart phone
- Have access to Youtube, etc.
- Be able to access the internet for uploading, signing in etc.
- May have limited access to a computer

#### 3. Individual growers
These tend not to have a cell phone. But are the primary implementors on the ground. T
They tend to: 
- Do not own a compatible smart phone. 
- Do not use Email 
- Disenfranchised populations, woman and indigenous peoples.
- Be completely offline 
- Be illiterate and unable to read or write. 
- No access to formal education
- No access to banking systems
- No access to a formal identity 
- Tend to have a phone number, via a sim card, but have limited access to a phone. 

## <a name="Consideration and Constraints"/> Considerations and constraints

Given the primary intended users are in developing countries most likely with a lot quality smart phone. The application features, use cases are designed with assumptions of inexpensive devices, lack of high bandwidth network, lack of email addresses, other considerations and constraints listed below.

### App must

* Work offline
With the exception of first time login, and uploading data, internet connectivity must not be required to use the app. It must be assumed that the Grower is in a remote part of the world with no network.

* Upload in edge and undependable networks
Images by nature require a lot of data to upload. Uploading data is often interrupted due to network issues or users data bundles running out (Users often buy small "internet bundle") 

* Optimize battery life
Charging phone in a remote region is often a challenge. Application features, design, implementation and maintenance should be keep this mind. High power consumption tasks, computing, and GPS searching should be kept to absolute minimum required to avoid draining the battery life.

* Be simple to use
Keeping the user interface/flows simple is paramount given the target users are probably not tech-savvy and neither would spend too much time on the device while doing planting activities.

* Be available in a local translation
Users are all over the world.

* Primarily rely on devices GPS
To track a tree that is planted, accuracy of the tree location is important.

* Deliver Quality Data
Capturing the grower information such as his name, photo, phone number, planted tree images, tree locations are critical in analyzing, and crediting the grower for his work.

### Unique Challenges

The following lists some additional considerations regarding the circumstances in using the application from the target users standpoint.

* Bright sunlight and hard to see screens.
* Extremely poor internet connections, uploads often stopped and restarted.
* Primarily used in remote places where charging and reception are not available
* Primary users (Trackers) often illiterate (unable to read)
* Multiple Growers registered using phone numbers via the tree tracker app.

## <a name="Tracker Registration, Login and Identification"/> Tracker Registration, Login, and Identification

The login process is designed with desire to identify trackers and growers, and pay them directly based on
verification of tree growth.

The "Village Coordinator" user most likely dose not have an email address. 

The Growers tend to have a phone number which can be used for Grower Registration. Verifying identify of the Grower responsible for the growth of individual trees is required for payment verification.

Key points relevant for Tracker Login and Identification:

* Trackers generally don't have access to email.

* Trackers most commonly use WhatsApp for communications.

* Trackers tend to enter wrong details when creating accounts, wrong phone number, no country code, etc., due factor authentication is recommend for account creation.

### Grower Registration

Requires update
Growers have been / will be scammed on the ground (by device owners or others logging in for them)

~ Grower login and identification begins with user registering himself on a device for the first time.

The registration process requires Grower to

1. Accept the privacy policy

2. enter a phone number or email address (Device owner most likely will use the device's phone number or the secondary user in a
developed country would likely use the email rather than the phone number).

3. Enter their first and last name

4. Optional Organization name

5. Capture a picture (selfie) of themselves

The email address or the phone number acts as the Grower identifier associated with the growth of a individual tree. The image of the Grower
is another way to verify and identify to pay for the growth of a tree.  

None of the steps above requires network connection to perform any verification. The photo and the identifier is stored locally on the device
which is synced later to the server when network connection is made available.~

### Grower Login and Identification
Requires Update

~Planter login and identification is similar to the registration flow except, if the planter has already used a specific device, the identifier is
found on the device from the earlier registration and hence privacy policy and signup with Greenstand steps are skipped. The planter is prompted to
take a picture again. Taking a picture is always done for both registration on a new device as well as for each check-in. This is required for
verification of the planter identity as the user might do tree growth tracking activity from multiple devices and the pictures are vital to mitigate
planter users being exploited for their work.

Thus login and identification process creates a check-in log with his identifier and photo each time. Any tree planting or tracking activity is
associated with this check-in which are uploaded to the Greenstand servers when the device owner syncs the data on network availability.

The key things to note about Planter login and identification are

* planter identifier (a phone number or email address) is a important way to identify a user

* every login creates a check-in log with the identifier and a picture of the planter

* all tree tracking activities are tagged with the check-in log for later verification and analysis

* prompt the user to login on the device every two hours to ensure correct tagging of tree tracking activities on the device.~

## <a name="Grower Activity Verification and Payout Data"/> Grower Activity Verification and Payout Data

### Grower Activity Verification
Requires Update
~ The capability to verify the accuracy of the data obtained from one or more devices regarding who is to be paid for the growth of
a tree relies on the fact that data regarding the individual's trees location, associated tracking activities with picture of the grower
and Grower's identifier is available to Greenstand for offline analysis when the data is synced on network availability.~ 

### Payout Data

Requires Update
~ When the Grower logs in a device, the payout information for the tree growth tracking is presented in the Tree data section with the
following information (this feature is evolving and what is documented here may be little different from what is seen in current
release version of the application)

1. Number of trees uploaded
2. Number of trees approved
3. Payment pending and
4. Payment made so far

Note: The actual payment process to the grower or the Organization co-ordinating the planting activities are not done via the Treetracker
app.~

## <a name="Use cases"/> Use Cases

This section documents all the use cases and the desired functionality built or to be built/modified in the Treetracker application.
Each use case has its user experience design, flow and any constraints documented in their own page, click on the link for more details.

Note: Please document the UX, flow, functionality and any constraints in a separate wiki page and link them here. Also, for new features
don't add the link to this document until it has been implemented. For modification of existing features, propose the changes for the use
case in a new page and replace the referred link here when the feature gets implemented.

### [Registration]()

This is the first step where the Grower user enters the name, phone number or email address, picture of the user and signs up with Greenstand
to use the treetracker application.

### [Grower checkin]()
Requires review
~Any time a Grower desires to take a picture of tree planting or tree growth tracking activities for uploading to greenstand later, he/she/they
logs in to the application. This login process is akin to a check-in process and all activities are tagged with this check-in details. The check-in details include, the Grower identifier and Grower's picture.~

### [Growers login switching]()

Requires review

~In cases where the device is shared, a different Growers can be logged in by switching the login. This step is a simple one where the Growers needing
to check-in can switch the user currently checked in by clicking on switch user menu action in the Action bar. This takes the user to the login screen
where inputs such as Growers identifier can be entered (email or phone number) and subsequently the camera screen to take the self image to complete
the check-in process.~

### [Landing page on successful Login]()

Requires review
~ On successful login, the planter should be taken to a Map of his current location given the device supports GPS location.  The map shows the locations
of all the trees planted by the user using the device. The use will be able to navigate to the following sections of the application from this home page

1. "Add Tree" button which when pressed would trigger the [Planting and tracking a tree](./Planting-and-tracking-a-tree) use case

2. "Red arrow mark" fab button (floating action button) that takes user to the tree and payout data screen

3. "Tree Data" from the action bar menu which takes the user to see tree and payout data screen

4. "About" from the action bar menu takes to the "About Greenstand" screen

5. "Change User" from the action bar menu which logs out the current user and prompts login screen for a new user~ 

If the GPS location permission is not enabled the user the user is not given
the ability to add a tree and must be instructed to change GPS settings.

### [Planting and tracking a tree]()

The simplest use case is when a planter digs a hole, plants a tree, takes a photo with the treetracker app, puts the phone in the pocket and repeats
the process.

### [Planting several hundreds trees]()

This use case is where a large group of planters are involved in planting trees. Capturing the pictures is done at the end or sporadically. Accidental
duplication of photos of trees already located could happen in this scenario. GPS data, images of the plants may help in detecting these instances of
duplication, but is not a concern for the Application though Greenstand verification process would care for it. Any methods or design of the application
that would help mitigate this is desirable. For e.g. (currently not implemented or under consideration), we could calculate the location of an individual
tree based on the device's location and its orientation and estimating the distance between the device and the tree from the camera image.

### [FMNR - Famer Managed Natural Regeneration] 

This use case has farmers tending to trees and often requires a before and after photo. For example, capture #1 the tree is lying down, capture #2 the farmer has tree propped up. 

### [Tracking growth of the trees]()

To track the growth of the planted trees, users would at regular intervals take the picture of the growing trees and upload it to Greenstand. This use case is similar to the other planting use cases and may not differ in any specific functionality of the app and is given for completeness of the listed use cases.

### [Capturing tree height when tracking tree growth]()

Obsolete Edge use case
~ A optional feature of the app to track the height of the tree is presented if the feature is turned on. This feature takes the user to a screen after taking a picture of an individual tree being tracked that has a vertical bar containing 5 different colors sections. Tapping on these section sets a color coded height value  of the tree being captured. The colors used are `green`, `purple`, `yellow`, `blue` and `orange`. Each section of the bar (different colors) each represent 50 cms (half meter) to capture the height metric of the tree.~

### [Capturing Planter's picture]()

~While registering or checking in it is vital to capture the planter's face as clearly as possible to help ascribe the tree planting and growth tracking
activities. There aren't any checks within the app at this point to ensure a clear image is taken prior to letting the user login. This is the section where any details about capturing the planter's picture can be specified and elaborated.~

### [Syncing Planter and Tree data]()

User is given the ability to upload all the planter data and the tree data stored in the device to the Greenstand servers. In addition, the payout data is downloaded and stored on the device. If the upload process is interrupted due to network issues, the upload process resumes from where it left on network availability.
