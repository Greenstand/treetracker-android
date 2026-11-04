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
run "pressKey: back"
tap "CaptureButton" "50%,92.5%"
run "extendedWaitUntil: {visible: {id: ApproveButton}, timeout: 15000}"
snap selfie-review
tap "ApproveButton" "83%,92.5%"
run "extendedWaitUntil: {visible: {id: TrackButton}, timeout: 15000}"
snap dashboard
