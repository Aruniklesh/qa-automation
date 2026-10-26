package com.uplift.tests.security;

import com.uplift.api.EmployeeApiService;
import com.uplift.base.BaseApiTest;
import com.uplift.config.ConfigReader;
import com.uplift.models.EmployeeModel;
import com.uplift.reporting.ReportLogger;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Security checks on the Auth and Employee API.
 *
 * SEC-1  Login with a wrong password is rejected
 * SEC-2  A fake or tampered token is rejected with 401
 * SEC-3  Reading an employee without a token is rejected with 401
 * SEC-4  Script / SQL text in a name field does not break the server or leak database errors
 * SEC-5  Error responses do not leak stack traces or internal details
 */
public class SecurityApiTest extends BaseApiTest {

    /** Text that should never appear in a response: stack traces, SQL errors, secrets. */
    private static final List<String> LEAK_MARKERS = Arrays.asList(
            "exception", "stacktrace", "at com.", "at org.", "at java.",
            "sql syntax", "sqlexception", "ora-", "psql", "syntax error", "password");

    private String authToken;

    @BeforeClass(alwaysRun = true)
    public void setupAuth() {
        authToken = EmployeeApiService.getToken(
                ConfigReader.getRequired("api.auth.username"), ConfigReader.getRequired("api.auth.password"));
    }

    private static EmployeeModel newEmployee() {
        EmployeeModel employee = EmployeeModel.generateRandomEmployee();
        employee.setEmployerId(ConfigReader.getProperty("api.employer.id"));
        return employee;
    }

    private static void assertNoLeak(Response response, String context) {
        String body = response.asString().toLowerCase();
        for (String marker : LEAK_MARKERS) {
            Assert.assertFalse(body.contains(marker),
                    context + ": response should not expose internal details ('" + marker + "'). Body: "
                            + response.asString());
        }
    }

    @Test(priority = 1, description = "SEC-1: Login with a wrong password is rejected")
    public void wrongPasswordIsRejected() {
        Response response = EmployeeApiService.authenticate(
                ConfigReader.getRequired("api.auth.username"), "Wrong-Pass-" + System.currentTimeMillis());

        int status = response.getStatusCode();
        Assert.assertTrue(status == 400 || status == 401 || status == 403,
                "Wrong password must be rejected (400/401/403) but was " + status + ". Body: " + response.asString());
        assertNoLeak(response, "Wrong-password response");
    }

    @Test(priority = 2, description = "SEC-2: A fake or tampered token is rejected with 401")
    public void fakeOrTamperedTokenIsRejected() {
        Response fake = EmployeeApiService.createEmployee(newEmployee(), "this.is.not-a-real-token");
        Assert.assertEquals(fake.getStatusCode(), 401, "Fake token must be rejected. Body: " + fake.asString());

        Response tampered = EmployeeApiService.createEmployee(newEmployee(), authToken + "x");
        Assert.assertEquals(tampered.getStatusCode(), 401,
                "Tampered token (real token with one extra character) must be rejected. Body: " + tampered.asString());
    }

    @Test(priority = 3, description = "SEC-3: Reading an employee without a token is rejected with 401")
    public void readEmployeeWithoutTokenIsRejected() {
        EmployeeModel employee = newEmployee();
        Response created = EmployeeApiService.createEmployee(employee, authToken);
        Assert.assertEquals(created.getStatusCode(), 201, "Setup: create employee. Body: " + created.asString());
        String id = EmployeeApiService.extractEmployeeId(created);

        Response response = EmployeeApiService.getEmployeeById(id, null);

        Assert.assertEquals(response.getStatusCode(), 401,
                "Personal data (PPSN, DOB) must not be readable without a token. Body: " + response.asString());
        Assert.assertFalse(response.asString().contains(employee.getPpsn()),
                "The 401 response must not contain the employee's PPSN");
    }

    @Test(priority = 4, description = "SEC-4: Script and SQL text in a name field does not break the server")
    public void injectionTextIsHandledSafely() {
        String[] payloads = {"<script>alert(1)</script>", "' OR '1'='1", "Robert'); DROP TABLE employees;--"};

        for (String payload : payloads) {
            EmployeeModel employee = newEmployee();
            employee.setFirstName(payload);
            Response response = EmployeeApiService.createEmployee(employee, authToken);

            Assert.assertTrue(response.getStatusCode() < 500,
                    "Server must not crash (5xx) on input: " + payload + ". Body: " + response.asString());
            assertNoLeak(response, "Input '" + payload + "'");
            ReportLogger.info("Input '" + payload + "' -> HTTP " + response.getStatusCode()
                    + (response.getStatusCode() == 400 ? " (rejected)" : " (accepted: check it shows as plain text in the portal)"));
        }
    }

    @Test(priority = 5, description = "SEC-5: Validation errors do not leak stack traces or internal details")
    public void errorResponseDoesNotLeakDetails() {
        EmployeeModel employee = newEmployee();
        employee.setPpsn("12345AB");

        Response response = EmployeeApiService.createEmployee(employee, authToken);

        Assert.assertEquals(response.getStatusCode(), 400, "Invalid PPSN status. Body: " + response.asString());
        assertNoLeak(response, "Validation error response");
    }
}
