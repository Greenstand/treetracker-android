Feature: Settings screen

  @skip
  Scenario: User navigates to Settings
    Given the app is launched with an existing user
    When I open Settings
    Then I should see "View/Edit Profile"
    And I should see "Map"
    And I should see "View Privacy Policy"
    And I should see "Log out"
    And I should see "Delete Account"

  @skip
  Scenario: Logout confirmation dialog appears
    Given the app is launched with an existing user
    When I open Settings
    And I tap "Log out"
    Then I should see "Are you sure you want to logout?"
    When I tap "Cancel"
    Then I should see "SETTINGS"
