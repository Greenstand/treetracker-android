Feature: Wallet select flow

  @skip
  Scenario: User sees wallet select screen after selecting a user
    Given the app is launched with an existing user
    When I tap "TRACK"
    And I dismiss sync reminder if present
    And I select the first user and advance
    Then I should see the wallet select screen
