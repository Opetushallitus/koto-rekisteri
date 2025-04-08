# end-to-end testing

```shell

# Do clean install before running the tests
npm ci

# Run code generator to create playwright tests
npx playwright codegen

# Run tests on CLI
npx playwright test

# Run tests with UI
npx playwright test --ui


```

## Hook playwright tests in idea debugger

1. Start idea debugger as usual
2. Change `playwright.config.ts`, the return object of workerWebServer

```diff
return {
-  command: "./mvnw spring-boot:run"
  url: `http://localhost:${port}`
-  reuseExistingServer: false,
+  reuseExistingServer: true,
   // ...
}
```

3. Run your playwright tests, eg. `npx playwright test --ui`

## Check playwright report on GH actions

1. If you have a pull request, open any check, for example `Build / Frontend tests`
2. Select `Summary` from left-top on the UI
3. From `Artifacts`, select `playwright-report`
