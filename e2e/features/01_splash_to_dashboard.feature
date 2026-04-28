Feature: Splash to Dashboard

  @skip
  Scenario: Returning user lands on Dashboard after launch
    Given the app is launched with an existing user
    Then I should see "UPLOAD"
    And I should see "MESSAGES"
    And I should see "TRACK"
