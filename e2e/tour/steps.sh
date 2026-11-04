# The screens the tour walks, in order. Sourced by tour.sh, which defines snap/run/tap/type_in.
# Positions are for a 1080x2400 screen (pixel_6): bottom bar arrows at 17%/83%, 92.5%.
NEXT=83%,92.5%
BACK=17%,92.5%

run "launchApp: {appId: $APP, clearState: true}"
run "extendedWaitUntil: {visible: ENGLISH, timeout: 30000}"
snap language
tap "LanguageOption_ENGLISH" "ENGLISH"
tap "ArrowForwardButton" "$NEXT"
snap signup-privacy
tap "PrivacyPolicyCloseButton" "50%,90%"
snap signup-credential
tap "EmailCredentialButton" "EMAIL"
snap signup-credential-email
tap "PhoneCredentialButton" "PHONE"
type_in "PhoneField" "5551230001" "Phone"
run "hideKeyboard"
tap "ArrowForwardButton" "$NEXT"
snap signup-name
type_in "FirstNameField" "Ada" "First Name"
type_in "LastNameField" "Admin" "Last Name"
run "hideKeyboard"
snap signup-name-filled
tap "ArrowForwardButton" "$NEXT"
run "extendedWaitUntil: {visible: {id: CaptureButton}, timeout: 15000}"
snap selfie
tap "InfoButton" "83%,92.5%"
snap selfie-tutorial
tap "TutorialDoneButton" "50%,85%"
tap "CaptureButton" "50%,92.5%"
run "extendedWaitUntil: {visible: {id: ApproveButton}, timeout: 15000}"
snap selfie-review
tap "ApproveButton" "83%,92.5%"
run "extendedWaitUntil: {visible: {id: TrackButton}, timeout: 15000}"
snap dashboard
USER=UserButton_5551230001

# Top bar: language picker from the dashboard, then dev options (long press on the logo).
tap "TopBarLanguageButton" "88%,5%"
snap language-topbar
tap "LanguageOption_ENGLISH" "ENGLISH"
tap "ArrowForwardButton" "$NEXT"
wait_id TrackButton
hold "TopBarLogo" "50%,5%"
snap dev-options
tap "SyncOverlaySwitch" "88%,9%"
snap dev-options-sync-on
tap "SyncOverlaySwitch" "88%,9%"
back_to TrackButton

# Settings and everything under it.
tap "SettingsButton" "8%,5%"
snap settings
tap "SettingsProfile" "Profile"
snap profile-select
tap "$USER" "25%,20%"
tap "ArrowForwardButton" "$NEXT"
snap profile
back_to SettingsProfile
tap "SettingsPrivacy" "Privacy Policy"
snap settings-privacy
tap "PrivacyPolicyCloseButton" "50%,90%"
tap "SettingsMap" "Map"
snap map-empty
tap "MapBackButton" "$BACK"
tap "SettingsEditTrees" "Edit Trees"
snap tree-edit-user-select
tap "$USER" "25%,20%"
tap "ArrowForwardButton" "$NEXT"
snap tree-list-empty
back_to SettingsProfile
tap "SettingsLogout" "Logout"
snap settings-logout-dialog
tap "DialogNegativeButton" "25%,60%"
back_to TrackButton

# Messages.
tap "MessagesButton" "Messages"
snap messages-user-select
tap "$USER" "25%,20%"
tap "ArrowForwardButton" "$NEXT"
snap messages-empty
back_to TrackButton

# Capture one tree with a note.
tap "TrackButton" "Track"
snap capture-user-select
tap "$USER" "25%,20%"
tap "ArrowForwardButton" "$NEXT"
snap add-org
tap "ArrowForwardButton" "$NEXT"
wait_id TutorialDoneButton 10000
snap capture-tutorial
tap "TutorialDoneButton" "50%,85%"
wait_id CaptureButton
snap capture
tap "CaptureButton" "50%,92.5%"
wait_id ApproveButton 60000
snap review
wait_id TutorialDoneButton 5000 && { snap review-tutorial; tap "TutorialDoneButton" "50%,85%"; }
tap "NoteButton" "50%,92.5%"
snap review-note-dialog
type_in "DialogTextField" "tour note"
run "hideKeyboard"
tap "DialogPositiveButton" "75%,60%"
tap "ApproveButton" "83%,92.5%"
wait_id CaptureButton 30000
snap capture-after-save
back_to TrackButton
snap dashboard-one-tree

# The new tree in tree edit and on the map.
tap "SettingsButton" "8%,5%"
tap "SettingsEditTrees" "Edit Trees"
tap "$USER" "25%,20%"
tap "ArrowForwardButton" "$NEXT"
snap tree-list
tap "TreeButton_0" "25%,20%"
tap "ArrowForwardButton" "$NEXT"
wait_id TreeNoteField
snap tree-detail
back_to SettingsMap
tap "SettingsMap" "Map"
run "extendedWaitUntil: {visible: {id: MapTreeCard_0}, timeout: 10000}"
snap map-one-tree
tap "MapBackButton" "$BACK"
