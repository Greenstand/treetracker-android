# Task 14: Add Appium + WebdriverIO + Cucumber E2E Tests

## Summary
Adds a BDD E2E test suite for the treetracker-android app using Appium 2, WebdriverIO v9, and Cucumber (TypeScript).

## Stack
- **Appium 2** + UiAutomator2 driver — native Android automation
- **WebdriverIO v9** — test runner with first-class Appium support
- **Cucumber** — BDD feature files (`.feature`)
- **Allure** — HTML test reports

## Files Added
- `e2e/package.json`
- `e2e/tsconfig.json`
- `e2e/wdio.conf.ts`
- `e2e/utils/helpers.ts`
- `e2e/.env.example`
- `e2e/features/01_splash_to_dashboard.feature`
- `e2e/features/02_signup_flow.feature`
- `e2e/features/03_capture_setup.feature`
- `e2e/features/04_tree_capture.feature`
- `e2e/features/05_settings.feature`
- `e2e/features/06_messages.feature`
- `e2e/features/07_org_picker.feature`
- `e2e/features/step-definitions/steps.ts`

## How to Run

```bash
# Prerequisites (one-time)
npm install -g appium
appium driver install uiautomator2
cd treetracker-android && ./gradlew assembleDebug

# Run tests
cd e2e
cp .env.example .env   # set APK_PATH and DEVICE_NAME
npm install
npm test

# Single feature
npm run test:spec features/05_settings.feature

# View Allure report
npx allure serve test-artifacts/allure-results
```
