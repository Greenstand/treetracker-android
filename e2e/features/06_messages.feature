Feature: Messages screen

  @skip
  Scenario: User navigates to Messages from Dashboard
    Given the app is launched with an existing user
    When I tap "MESSAGES"
    Then I should see the messages screen

  @skip
  Scenario: User sees empty messages state
    Given the app is launched with an existing user
    When I tap "MESSAGES"
    And I select the first user
    Then I should see "No Messages, Yet."
