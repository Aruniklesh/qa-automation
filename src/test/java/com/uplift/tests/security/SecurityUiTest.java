package com.uplift.tests.security;

import com.uplift.base.BaseTest;
import com.uplift.config.ConfigReader;
import com.uplift.data.JourneyContext;
import com.uplift.pages.admin.AdminDashboardPage;
import com.uplift.pages.admin.AdminLoginPage;
import com.uplift.reporting.ReportLogger;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Security checks on the Admin portal.
 *
 * UI-SEC-1  Wrong password does not open the dashboard
 * UI-SEC-2  After the session is cleared, the dashboard URL does not open without logging in again
 * UI-SEC-3  An employer admin cannot log in to the Admin portal (role-based access)
 */
public class SecurityUiTest extends BaseTest {

    /** Removes cookies and browser storage, so the browser behaves like a new visitor. */
    private void clearSession() {
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
                "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch (e) {}");
    }

    private AdminLoginPage openAdminLogin() {
        AdminLoginPage loginPage = new AdminLoginPage(driver).open(ConfigReader.getAdminPortalUrl());
        clearSession();
        driver.navigate().refresh();
        return loginPage;
    }

    @Test(priority = 1, description = "UI-SEC-1: Wrong password does not open the Admin dashboard")
    public void wrongPasswordDoesNotOpenDashboard() {
        AdminDashboardPage dashboard = openAdminLogin()
                .loginAs(ConfigReader.getAdminUsername(), "Wrong-Pass-" + System.currentTimeMillis());

        Assert.assertFalse(dashboard.isDashboardLoaded(),
                "Dashboard must not open with a wrong password. Current URL: " + driver.getCurrentUrl());
    }

    @Test(priority = 2, description = "UI-SEC-2: Dashboard URL needs a valid session")
    public void dashboardNeedsValidSession() {
        AdminDashboardPage dashboard = openAdminLogin()
                .loginAs(ConfigReader.getAdminUsername(), ConfigReader.getAdminPassword());
        Assert.assertTrue(dashboard.isDashboardLoaded(), "Setup: admin should log in");
        String dashboardUrl = driver.getCurrentUrl();
        ReportLogger.info("Dashboard URL: " + dashboardUrl);

        clearSession();
        driver.get(dashboardUrl);

        Assert.assertFalse(new AdminDashboardPage(driver).isDashboardLoaded(),
                "Dashboard must not open without a session (cookies and storage cleared). URL: "
                        + driver.getCurrentUrl());
    }

    @Test(priority = 3, description = "UI-SEC-3: An employer admin cannot log in to the Admin portal")
    public void employerAdminCannotUseAdminPortal() {
        String email;
        String password;
        try {
            email = JourneyContext.employerAdminEmail();
            password = JourneyContext.employerAdminPassword();
        } catch (RuntimeException e) {
            throw new SkipException("No employer admin login available. Run the journey first, or set "
                    + "employer.admin.email and employer.admin.password in secrets.properties");
        }

        AdminDashboardPage dashboard = openAdminLogin().loginAs(email, password);

        Assert.assertFalse(dashboard.isDashboardLoaded(),
                "An employer admin must not get into the Admin portal. URL: " + driver.getCurrentUrl());
    }
}
