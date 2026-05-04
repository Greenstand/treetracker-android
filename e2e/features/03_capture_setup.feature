Feature: Capture setup flow

  Scenario: User goes back to the dashboard after a capture session
    Given the app is launched with an existing user
    When I tap "TRACK"
    And I dismiss sync reminder if present
    And I select the first user and advance
    And I select the first wallet and advance
    And I enter organization "Test Org"
    And I tap the right arrow
    And I should see the capture screen
    And I take a tree capture
    And I should see the tree image review screen
    And I accept the tree capture
    And I should be back on the capture screen
    And I tap the back arrow
    Then I should reach the dashboard
