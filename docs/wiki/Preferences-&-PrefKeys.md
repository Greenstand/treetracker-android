## What is it?
Preferences is a wrapper class around the Android SharedPreferences. It's to allow better control over the naming and management of SharedPreferences. There should be no references to SharedPreferences in the codebase aside from in the Preferences class.

## PrefKey
This is used as keys instead of strings. They can be composed together to create paths and "directories".

## UserPrefKey
This can be used with PrefKeys. The only difference is that when a UserPrefKey is used, the Preferences class will automatically add the current users ID to the path. The purpose of this is to allow certain preferences to be saved on a per user basis. 

For example.
User A sets the app language to Swahili while user B sets the app language to English. We want each of their preferences to be saved for the same key. As a user of the Preferences class we just need to define a key as follow:
`val myKey = UserPrefKey("myKey")`
You'll notice that we don't have to worry about the current user or their ID, the system will add that for us later on. 

### SESSION PrefKey
This is a unique Prefkey. Anything that depends on session to make its path will be deleted when the user logs out (session ends)
- `val key = PrefKeys.SESSION + PrefKey("MyKey") // Will be deleted on user logout`
- `val userKey = PrefKeys.SESSION + UserPrefKey("MyUserKey") // Will be deleted on user logout`
- `val genericKey = PrefKeys.ROOT + UserPrefKey("GenericKey") // Will NOT be deleted on user logout`
- `val genericKey = PrefKeys.SYSTEM_SETTINGS + PrefKey("GenericKey") // Will NOT be deleted on user logout`

