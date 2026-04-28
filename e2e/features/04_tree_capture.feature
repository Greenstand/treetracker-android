Feature: Tree capture flow

  @skip
  Scenario: User reaches the camera capture screen
    Given the app is launched with an existing user
    When I tap "TRACK"
    And I dismiss sync reminder if present
    And I select the first user and advance
    And I advance through org setup
    Then I should see the capture screen
