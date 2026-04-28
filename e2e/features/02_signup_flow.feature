Feature: New user onboarding entry

  Scenario: Fresh launch shows language selection screen
    Given the app is launched fresh
    Then I should see "ENGLISH"
    And I should see "SWAHILI"
    And I should see "PORTUGUESE"

  Scenario: Selecting a language shows credential entry with type selector
    Given the app is launched fresh
    When I tap "ENGLISH"
    And I tap the right arrow
    Then I should see "Privacy Policy"
