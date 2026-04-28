import "dotenv/config";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const config: any = {
  runner: "local",
  specs: ["./features/**/*.feature"],
  exclude: [],
  maxInstances: 1,

  capabilities: [
    {
      platformName: "Android",
      "appium:deviceName": process.env.DEVICE_NAME || "emulator-5554",
      "appium:app": process.env.APK_PATH,
      "appium:automationName": "UiAutomator2",
      "appium:appPackage": "org.greenstand.android.TreeTracker.debug",
      "appium:appActivity":
        "org.greenstand.android.TreeTracker.activities.TreeTrackerActivity",
      "appium:autoGrantPermissions": true,
      // Keep app state between scenarios; individual tests clear data as needed
      "appium:noReset": true,
      "appium:newCommandTimeout": 240,
      "appium:uiautomator2ServerLaunchTimeout": 60000,
    },
  ],

  logLevel: "warn",
  bail: 0,
  waitforTimeout: 15000,
  connectionRetryTimeout: 120000,
  connectionRetryCount: 3,

  services: [
    [
      "appium",
      {
        command: "appium",
        args: {
          relaxedSecurity: true,
          log: "./test-artifacts/appium.log",
        },
      },
    ],
  ],

  framework: "@wdio/cucumber-framework",

  reporters: [
    "spec",
    [
      "allure",
      {
        outputDir: "./test-artifacts/allure-results",
        disableWebdriverStepsReporting: false,
        useCucumberStepReporter: true,
      },
    ],
  ],

  cucumberOpts: {
    require: ["./features/step-definitions/**/*.ts"],
    requireModule: ["ts-node/register"],
    backtrace: false,
    dryRun: false,
    failFast: false,
    snippets: true,
    source: true,
    strict: false,
    tags: process.env.WDIO_TAGS || "not @skip",
    timeout: 90000,
    ignoreUndefinedDefinitions: false,
  },
};
