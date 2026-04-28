Feature: New user completes full onboarding

  @skip
  Scenario: New user completes signup and reaches Dashboard
    Given the app is launched fresh
    When I complete signup
    Then I should see "UPLOAD"
    And I should see "MESSAGES"
    And I should see "TRACK"
