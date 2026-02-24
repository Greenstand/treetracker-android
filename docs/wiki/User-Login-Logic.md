# This is a work in Progress

# The Tree tracker - planter app 

Answering the question of "who is responsible for the growth of individual trees."

The Login logic is designed to address the following situations that arise from shared ownership of phones and the desire to pay planters directly based on a verification of tree growth work:
-Verify who is actually tracking the trees and pay them for that.

To establish who is actually responsible for the existence of a tree and reward that specific individual.

- multiple planters are using the same device
- users unable to login via email (user don't have email)
- users unable to verify their account due to working off the network
- planters switch devices
- planters enter wrong details
- Primitive users have been / will be scammed on the ground (by users logging in for them, and not logging out etc.)
- App must be so easy to log in that anyone, regardless of literacy, will be able to log in and go.

# User Identification concept: 

- Tree records are sent to the server with the planter identifiers. Whether or not this user already exists is worked out server side.
- Planter_identifications are the identifier + photo records that are captured each time they identify themselves
- User is forced to identify themselves ever two hours, the phone number + photograph are required after a time out period
- Each tree record is tagged with a set of planter_identifications
- If a user has already entered their details' ON THAT PARTICULAR PHONE, then they only enter their phone number and add a verification photograph. (If not recognized by the device skip, the user must re-enter other user credentials.)

[User Login Flow](https://github.com/Greenstand/treetracker-android/issues/178#issuecomment-467268480) [Under this issue](https://github.com/Greenstand/treetracker-android/issues/178)

Notes: The 'does the user have an account' is not the right question though. It should be 'Is the user present on this device'

### Method (needs review)

Developers can look in 'planter_details' for an existing record for that user identifier. 

Note: that it's planter_details and not planter_identifications

Planter details = The planter must also register with their phone once per phone, these are the planter details.
They are entered the first time a user enters details with a new device, or when a user enters information incorrectly and so accidently creates new planter details. This field corresponds to "user_table" in main db.

Planter Identification = every time the user enters details into the phone. These are linked to the tree id in the "trees" table of the main db. 
- there is an identification timeout, the planter must re-identify each time they time out (these are the planter_identifications)
  
There will be 1 record per planter in planter_details, but many in planter_identifications.   
 
If less than 2 hours (from the last login time) user will be automatically logged in otherwise it will be open the login part. 


Why is checking in the planter_identifications is different from checking in the planter_details? 
The 'details' (used for planter_details) are captured only one time per user per device.

We track device. Each phone has a registered planter. Each tree entry is connected to planters details. It is not clear if the planter can create a new entry in the "user" table by changing the entries during login. Or **if this table is locked to the original user of the app from that device. To be checked.**

treetracker=> select * from devices where id = 3;
 id |    android_id    | app_version | app_build | manufacturer |  brand  |  model   | hardware |  device  |  serial  | android_release | android_sdk
