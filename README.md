# Workplace Pension Platform (Ireland) - QA Automation Framework

Java 17 · Selenium · TestNG · REST Assured · ExtentReports · Maven · Jenkins

**Done by Arun**

## 1. What this project does

It tests one journey of the pension platform automatically, through the web portals and the API:

**Admin creates an employer → Admin adds an employer admin → Employer admin adds employees → Employees appear in the list and receive an invite.**

```
ADMIN PORTAL                          EMPLOYER PORTAL                       API
log in                                read invite email (code + link)       get token / 401 without token
create employer (name, CRN,      ──►  set password, log in             ──►  create employee -> 201 + schema
 address, payroll date, IBAN, BIC)    add one employee                      read it back and compare
add employer admin (name, email,      bulk upload CSV: 3 valid, 2 invalid   bad PPSN -> 400 + message
 mobile, status, occupation, PEP)     16 invalid-data checks                employee visible in the portal
        │                             log out
        └── invite email ──► test inbox (Gmail)

Every test ──► HTML report, with a screenshot when a test fails
```

| Test | What it covers |
| :--- | :--- |
| `PreflightTest` | Checks your settings work before the long run: values set, URLs reachable, browser opens, API login, inbox login |
| `AdminPortalJourneyTest` | Admin login, create employer, add employer admin |
| `EmployerPortalJourneyTest` | Invite email, first login, add employee, bulk upload (3 valid + 2 invalid), intro email, logout |
| `EmployeeValidationTest` | One data-driven test: 16 invalid-data scenarios on the Add employee form (data in JSON) |
| `EmployeeApiTest` | Token, 401, create (201 + schema), read back, bad PPSN (400), portal cross-check |
| `FrameworkSmokeTest` | Offline self-test of config, data generators, CSV builder and report |

The 24 designed test cases (17 automated, 7 manual, with reasons) are in [docs/TEST_CASES_PART_A.md](docs/TEST_CASES_PART_A.md).

**Documentation (in the `docs` folder)**

| Document | What it is |
| :--- | :--- |
| [Test_Cases.xlsx](docs/Test_Cases.xlsx) | Test cases in Excel: 24 cases, 16 data-driven scenarios, 6 API checks, traceability to the assignment |
| [Test_Plan.docx](docs/Test_Plan.docx) | Test plan: scope, approach, environment, data, criteria, risks, defect handling |
| [Test_Suite_Document.docx](docs/Test_Suite_Document.docx) | The suites, every test, order and dependencies, how to run |
| [Sample_Bug_Report.docx](docs/Sample_Bug_Report.docx) | Bug report format, severity guide, blank template and bug log |
| [Flow_Charts.docx](docs/Flow_Charts.docx) | Flow charts of the journey and of the framework and pipeline (PNG files in `docs/flowcharts`) |
| [TEST_CASES_PART_A.md](docs/TEST_CASES_PART_A.md) | Part A test case table and the automate-vs-manual reasoning |

## 2. Current status

| Area | Status |
| :--- | :--- |
| Framework, test data, reporting, Jenkinsfile | Done |
| Self-test (`FrameworkSmokeTest`) | Passing 7 of 7 |
| Journey, validation and API tests | Written from the application guide. They need their first run against the QA environment (URLs and logins are supplied separately). Locators may need small adjustments in the page classes |
| Jenkins pipeline | Runs on a local Jenkins: checkout, compile and the test stages start with credentials from the Jenkins store. Test stages report the missing QA settings until the QA URLs are supplied. Report and screenshots for the full run come after the first QA run |
| Assumptions | Listed in section 10 |
| Optional extras | Appium mobile test and parallel browsers are not done |

## 3. Project structure

```
src/main/java/com/uplift
  pages/        Page Objects: the only place with locators (one class per screen)
  api/          API helper: token, create employee, get employee
  config/       ConfigReader: reads all settings
  data/         Unique QA-AUTO- test data, valid PPSN/IBAN generators, CSV builder
  models/       Employer, EmployerAdmin and Employee data classes
  utils/        Waits, screenshots, Gmail inbox reader
  reporting/    ExtentReports listener (report + failure screenshot)
  base/         Browser start/stop, base test classes
src/test/java/com/uplift/tests    The tests listed above
src/test/resources
  environments/qa.properties      URLs and API paths of the QA environment
  secrets.properties.example      Template for passwords (copy to secrets.properties)
  testng-*.xml                    Which tests run (preflight, ui, api, regression, smoke)
  testdata/                       Invalid-data scenarios (JSON)
  schemas/                        Expected API response (JSON schema)
Jenkinsfile                       Pipeline
docs/                             Test cases (Excel), test plan, test suite document, bug report, flow charts
```

## 4. What you need

- **JDK 17 or newer** - check with `java -version`
- **Maven 3.9 or newer** - check with `mvn -v`
- **Google Chrome** (Edge or Firefox also work). The browser driver downloads automatically.
- **A Gmail account for test emails** with 2-step verification turned on and an [App Password](https://myaccount.google.com/apppasswords) created. Invite emails are sent to addresses like `yourname+qa-auto-...@gmail.com`, so each run finds only its own emails.
- **The QA details:** Admin portal URL, Employer portal URL, Admin login, API base URL and the Swagger documentation.

Only use the QA environment. Everything the tests create starts with `QA-AUTO-`, so it is easy to clean up.

## 5. Setup

**Step 1. Get the code and check the build**
```
git clone https://github.com/Aruniklesh/qa-automation.git
cd qa-automation
mvn -B clean test-compile
```
It should end with `BUILD SUCCESS`.

**Step 2. Fill in the URLs and API paths.** Open `src/test/resources/environments/qa.properties`:

| Key | What to enter |
| :--- | :--- |
| `admin.portal.url` | Admin portal address |
| `employer.portal.url` | Employer portal address |
| `api.base.url` | API base address |
| `api.auth.endpoint` | Login path from Swagger |
| `api.employees.endpoint` | Employees path from Swagger |
| `api.auth.username.field`, `api.auth.password.field` | Field names of the login request (default `email`, `password`) |
| `api.auth.token.path` | Where the token is in the login response (default `token`) |
| `api.employee.id.path` | Where the id is in the create response (default `id`) |
| `api.employer.id` | Only if the API needs an employer id |
| `employer.expected.status` | Optional: status text expected in the employer list, e.g. `Active` |

**Step 3. Add the passwords.** Copy `src/test/resources/secrets.properties.example` to `src/test/resources/secrets.properties` (this file is never uploaded to Git) and fill in:

| Key | What to enter |
| :--- | :--- |
| `admin.username`, `admin.password` | Admin portal login |
| `api.auth.username`, `api.auth.password` | API login from the Swagger documentation |
| `inbox.address` | The Gmail address, e.g. `yourname@gmail.com` |
| `inbox.username`, `inbox.password` | The same Gmail address and the **App Password** |

Any value can also be passed on the command line (`"-Dadmin.password=..."`) or as an environment variable (`ADMIN_PASSWORD`). This is how Jenkins supplies them.

**Step 4. Check the settings.**
```
mvn test "-DsuiteXmlFile=src/test/resources/testng-preflight.xml"
```
Each failure says what is wrong and where to fix it. Continue when all checks pass.

## 6. How to run

On Windows PowerShell, keep the quotes around every `-D` option.

| Goal | Command |
| :--- | :--- |
| **Run everything (one command)** | `mvn clean test` |
| Check settings first | `mvn test "-DsuiteXmlFile=src/test/resources/testng-preflight.xml"` |
| UI journey + validation checks | `mvn test "-DsuiteXmlFile=src/test/resources/testng-ui.xml"` |
| API tests only | `mvn test "-DsuiteXmlFile=src/test/resources/testng-api.xml"` |
| One test class | `mvn test "-Dtest=AdminPortalJourneyTest"` |
| Watch the browser | add `"-Dheadless=false"` |
| Use Edge or Firefox | add `"-Dbrowser=edge"` or `"-Dbrowser=firefox"` |
| Self-test without QA access | `mvn test "-DsuiteXmlFile=src/test/resources/testng-smoke.xml"` |

The validation tests and the API portal check need an employer admin. A full run creates one in the journey and reuses it. To run them alone, add `employer.admin.email` and `employer.admin.password` to `secrets.properties`.

## 7. Reports and results

| What | Where |
| :--- | :--- |
| HTML report (open in a browser) | `reports/ExtentReport.html` |
| Failure screenshots | `reports/screenshots/` |
| Raw test results | `target/surefire-reports/` |
| Generated bulk-upload CSV | `target/test-data/` |

In the report, green means passed, red means failed (with a screenshot and the reason), and grey means skipped because an earlier step failed.

## 8. Jenkins

The `Jenkinsfile` has the stages **Checkout → Install → API tests → UI tests → Publish report**.

**One-time Jenkins setup**
1. Install the plugins: Pipeline, Git, Credentials Binding, HTML Publisher, JUnit and Mailer. The Jenkins machine needs Java 17+, Maven and Chrome.
2. Add credentials (Manage Jenkins → Credentials, kind *Username with password*) with these exact IDs:

   | ID | Content |
   | :--- | :--- |
   | `qa-admin-credentials` | Admin portal login |
   | `qa-api-credentials` | API login |
   | `qa-test-inbox` | Gmail address and App Password |
   | `qa-employer-admin` | An already-activated employer admin (used by the API stage) |

3. Set `ADMIN_PORTAL_URL`, `EMPLOYER_PORTAL_URL` and `API_BASE_URL` as global environment variables (Manage Jenkins → System), or put them in `qa.properties`.
4. Create a **Pipeline** job: *Pipeline script from SCM* → Git → this repository → branch `*/main` → script path `Jenkinsfile`.

**Running it**
- Click **Build with Parameters**. Choose the browser and headless mode.
- It also runs nightly and checks the repository for new pushes about every 5 minutes (the first build must be started manually so Jenkins learns the triggers).
- Each stage keeps its own report. The HTML report is shown on the build page, and screenshots and logs are saved as build artifacts.
- If any test fails, the build is marked **UNSTABLE**. A build or pipeline error marks it **FAILED**.
- To get an email on failure, enter an address in the `NOTIFY_EMAIL` parameter.

## 9. If something goes wrong

| Message | Meaning and fix |
| :--- | :--- |
| `Missing required configuration '...'` | A value is not set. Set it as described in section 5 |
| `Cannot reach <url>` | Wrong URL, VPN or network problem |
| `Could not obtain an API token` | Check the login path, field names and token path against the Swagger |
| `Cannot log in to the inbox` | Use the Gmail **App Password**, not the normal password |
| `no visible element matching ...` | The page differs from what the test expects. Look at the screenshot, then adjust that one locator in the page class (usually only the label word in `fieldByLabel("...")`) |
| `No email to ... arrived within 180s` | The invite was not sent to that address, or the inbox settings are wrong |
| `No verification code matching ...` | The email wording differs. Change `inbox.code.regex` in `config.properties` |
| `No upload summary with added/rejected counts` | The summary wording differs. Adjust the matching pattern in `BulkUploadPage` |
| Many tests skipped | An earlier step failed. Fix the first red test |

## 10. Assumptions

The QA environment and Swagger were not available while building, so the framework was written from the application guide with these assumptions. If one is wrong, the fix is a small change in the place named.

| # | Assumption | If it is different, change |
| :--- | :--- | :--- |
| 1 | Field and button names on screen are close to the guide (for example "CRN", "IBAN", "PPSN", "Add employer", "Bulk upload") | The label text in the page class, for example `fieldByLabel("crn")` in `CreateEmployerPage` |
| 2 | The employer address lookup shows a list of suggestions and the first one is acceptable | `employer.address.lookup` in `config.properties`, or `addressSuggestion` in `CreateEmployerPage` |
| 3 | Payroll date, economic status and occupation are dropdowns. If the configured value is not offered, the first real option is used and noted in the report | `employer.payroll.day` and `employer.admin.*` in `config.properties` |
| 4 | The PEP question is a Yes/No choice, and "No" is a valid answer for a test admin | `employer.admin.pep.answer` |
| 5 | The invite email goes to the address entered, contains the verification code next to the word "code", and links to the Employer portal | `inbox.code.regex` in `config.properties`, `EmailInboxHandler` |
| 6 | The invite is read from a Gmail test inbox (with a `+alias` address per run) over IMAP using an App Password | `inbox.*` settings in `secrets.properties` |
| 7 | The employee list and the employer list can be searched, or show the new record on the first page | `searchInput` in `EmployersPage` / `EmployeesPage` |
| 8 | The bulk upload page shows a summary with words like "3 added" and "2 rejected", and a reason next to each rejected row | The `ADDED` / `REJECTED` patterns in `BulkUploadPage` |
| 9 | The portal's CSV template has a header row. Columns are matched by name (first name, last name, email, mobile, date of birth, PPSN, start date), and dates use `dd/MM/yyyy` | `CsvTestDataGenerator`, `csv.date.format` |
| 10 | A valid PPSN has 7 digits followed by a valid check letter. The generated test PPSNs follow the published checksum, so the application should accept them | `TestDataGenerator.validPpsn()` |
| 11 | A valid mobile is an Irish number such as `+353 8x xxxxxxx`, and a valid date of birth means the employee is at least 18 | `TestDataGenerator.irishMobile()`, `EmployeeModel` |
| 12 | The API login takes an email and password, and returns the token in a field called `token` | `api.auth.*` in `environments/qa.properties` |
| 13 | The API uses JSON field names that match `EmployeeModel` (`firstName`, `lastName`, `email`, `mobileNumber`, `dateOfBirth`, `ppsn`, `startDate`), returns the new id in `id`, and a bad PPSN returns 400 with a message that mentions the PPSN | `EmployeeModel`, `api.employee.id.path`, `schemas/employee-response.schema.json` |
| 14 | The API-created employee belongs to the same employer as the employer admin used for the portal check | `api.employer.id` |
| 15 | Test data is not deleted after a run. Everything starts with `QA-AUTO-` so the provider can clean it up | Add an API cleanup step (see "what to add next") |

## 11. Bugs and UX issues found

None reported yet because the QA environment has not been run. Problems found and fixed in this framework during review: assertions that could never fail, wrong bank fields (sort code instead of IBAN/BIC), invalid generated PPSNs, credentials in CI files, and Jenkins stages overwriting each other's results.

## 12. Written questions

**1. If a test fails in Jenkins, how do you find out whether the problem is the application or the test?**
I start with the Extent report and look at three things: the error message, the screenshot, and which step failed. If a status code is wrong, a record is missing, or the screenshot shows an error banner, I suspect the application. If a locator timed out while the page looks perfectly normal, I suspect my test. Then I try the same thing by hand, or through the API. If the API also rejects it, it is a real bug. If it works by hand, I fix the test.

**2. How would you run the UI tests in parallel on Jenkins agents and alert the team on failure?**
The browser handling is already thread-safe, so TestNG can run independent test classes side by side. The journey itself has to stay in order, so I would keep it in one class. In Jenkins I would add a `parallel` block with one stage per browser (or per suite), running on separate agents or on a Selenium Grid. For alerts, the pipeline already has a failure and unstable step that sends an email. Slack would work the same way, with a link to the report so people can click straight through.

**3. You are the only tester on the product. How would you split effort between UI, API and mobile testing?**
Roughly half my time on the API, a third on the web UI, and the rest on mobile and exploratory testing. The business rules, like PPSN, age and duplicates, are quickest and most stable to check at the API. I would keep the UI automation for the few journeys that really matter, like this one. For mobile, I would write a small smoke test and then test by hand on real devices, because identity checks and app builds are hard to automate reliably.

**4. How would you test payment reconciliation, where a bank file must match an employer's contribution payment?**
I would set up an employer with a known contribution amount and then try a bank file for each situation: an exact match, too little, too much, a wrong reference, and the same file sent twice. For each one I would check the status, that the amounts match to the cent, that money is never allocated twice, and that an audit record is written.

**5. How would you handle flaky tests caused by slow emails or third-party identity checks?**
I avoid fixed sleeps. Instead the test keeps checking the inbox until the email arrives or a timeout is reached, and every run uses its own email address, so an old email can never be picked up by mistake. That is how this framework works. For third-party identity checks, I would replace them in QA with a stub that gives fixed answers (pass, fail, PEP match), and keep just a few tests against the real provider. I would allow at most one retry, only for those external steps, and I would show it in the report so it never hides a real problem.

**6. How would you manage test data so the QA environment does not fill up with junk?**
Everything the tests create starts with `QA-AUTO-` and a run id, which is how this framework works, so test data is easy to spot. After a run I would delete what I created through the API where possible, and I would add a scheduled job to remove old `QA-AUTO-` records. For things that rarely change, like a ready-made employer admin, I would reuse one instead of creating a new one every run.

**7. What would you add next if you had another week?**
First, run everything properly against the QA environment and capture the evidence. Then I would add API cleanup of the test data, automate the cases I left manual (invalid IBAN and a bad upload file), run browsers in parallel in Jenkins, and write the Appium smoke test for the registration screen.
