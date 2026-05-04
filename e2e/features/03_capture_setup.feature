Feature: Capture setup flow

  Scenario: User enters organization and reaches the capture screen
    Given the app is launched with an existing user
    When I tap "TRACK"
    And I dismiss sync reminder if present
    And I select the first user and advance
    And I select the first wallet and advance
    And I enter organization "Test Org"
    And I tap the right arrow
    Then I should see the capture screen

  Scenario: User takes a tree capture and reaches the image review screen
    Given the app is launched with an existing user
    When I tap "TRACK"
    And I dismiss sync reminder if present
    And I select the first user and advance
    And I select the first wallet and advance
    And I enter organization "Test Org"
    And I tap the right arrow
    And I should see the capture screen
    And I take a tree capture
    Then I should see the tree image review screen
