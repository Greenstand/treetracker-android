Feature: Capture setup flow

  @skip
  Scenario: User starts tree capture from Dashboard
    Given the app is launched with an existing user
    When I tap "TRACK"
    And I dismiss sync reminder if present
    Then I should see the user select screen

  @skip
  Scenario: User advances from user select to org setup
    Given the app is launched with an existing user
    When I tap "TRACK"
    And I dismiss sync reminder if present
    And I select the first user and advance
    Then I should see text containing "Organization"
