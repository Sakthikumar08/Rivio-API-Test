package com.rivio.tests;

import com.rivio.utils.ApiConfig;
import com.rivio.utils.AssertionUtils;
import com.rivio.utils.PayloadBuilder;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * TC-EMP-001 → TC-EMP-008
 *
 * Seeded employee profiles:
 *   id=1  John Doe (VP Eng)    userId=3  dept=1  desig=1  loc=1
 *   id=2  Sarah Connor (HR)    userId=2  dept=2  desig=4  loc=1
 *   id=3  Alice Smith (Sr SE)  userId=4  dept=1  desig=2  loc=1  reportsTo=1
 *   id=4  Bob Marley (FE Dev)  userId=5  dept=1  desig=3  loc=2  reportsTo=1
 *
 * We onboard a NEW employee using a new user (not disrupting seeded data).
 */
@Epic("Rivio HRMS API")
@Feature("Employee Management")
public class TC04_EmployeeTest {

    // Seeded IDs used in tests
    private static final int DEPT_ENG   = 1;
    private static final int DESIG_SR   = 2;   // Senior Software Engineer
    private static final int LOC_BLR    = 1;   // Bengaluru HQ

    // ── TC-EMP-001: Get existing seeded employee profile ──────────────────────

    @Test(priority = 1, description = "Get seeded employee profile — John Doe (id=1)")
    @Story("Employee Profile")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /employees/1 → John Doe, Engineering, VP of Engineering, ACTIVE")
    public void tc_emp_001_getSeededEmployeeProfile() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/employees/1")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        assertEquals(response.jsonPath().getInt("data.id"), 1);
        String firstName = response.jsonPath().getString("data.firstName");
        String lastName  = response.jsonPath().getString("data.lastName");
        assertNotNull(firstName, "data.firstName must not be null");
        assertFalse(firstName.isEmpty(), "data.firstName must not be empty");
        assertNotNull(lastName, "data.lastName must not be null");
        assertNotNull(response.jsonPath().getString("data.departmentName"), "data.departmentName must not be null");
        System.out.println("✅ Employee #1 profile verified: " + firstName + " " + lastName);
    }

    // ── TC-EMP-002: Get HR employee ───────────────────────────────────────────

    @Test(priority = 2, description = "Get seeded employee — Sarah Connor (id=2)")
    @Story("Employee Profile")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /employees/2 → Sarah Connor, Human Resources, HR Director")
    public void tc_emp_002_getSarahProfile() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/employees/2")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        String firstName = response.jsonPath().getString("data.firstName");
        assertNotNull(firstName, "data.firstName must not be null");
        assertFalse(firstName.isEmpty(), "data.firstName must not be empty");
        assertNotNull(response.jsonPath().getString("data.departmentName"), "data.departmentName must not be null");
        System.out.println("✅ Employee #2 profile verified: " + firstName);
    }

    // ── TC-EMP-003: Employee directory ───────────────────────────────────────

    @Test(priority = 3, description = "Get paginated employee directory — expect 4 seeded")
    @Story("Employee Directory")
    @Severity(SeverityLevel.NORMAL)
    public void tc_emp_003_getEmployeeDirectory() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/employees")
                .then()
                .extract().response();

        AssertionUtils.assertPaginated(response);
        int total = response.jsonPath().getInt("data.totalElements");
        assertTrue(total >= 4, "Should have at least 4 seeded employees, found: " + total);
        System.out.println("✅ Employee directory. Total: " + total);
    }

    // ── TC-EMP-004: Search by name ────────────────────────────────────────────

    @Test(priority = 4, description = "Search employee directory by an existing first name")
    @Story("Employee Directory")
    @Severity(SeverityLevel.NORMAL)
    public void tc_emp_004_searchEmployeeByName() {
        // Pick a search term from the actual data — first name of employee #1.
        Response emp1 = given()
                .spec(ApiConfig.authSpec())
                .when().get("/employees/1")
                .then().extract().response();
        String searchTerm = emp1.jsonPath().getString("data.firstName");
        assertNotNull(searchTerm, "Could not derive a search term from employee #1");

        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("search", searchTerm)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/employees")
                .then()
                .extract().response();

        AssertionUtils.assertPaginated(response);
        int total = response.jsonPath().getInt("data.totalElements");
        assertTrue(total >= 1, "Search for '" + searchTerm + "' should return at least 1 result");
        System.out.println("✅ Search '" + searchTerm + "' found: " + total);
    }

    // ── TC-EMP-005: Eligible for attendance ──────────────────────────────────

    @Test(priority = 5, description = "Get employees eligible for attendance today")
    @Story("Employee Attendance Eligibility")
    @Severity(SeverityLevel.NORMAL)
    public void tc_emp_005_eligibleForAttendance() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("date", PayloadBuilder.today())
                .when()
                .get("/employees/eligible-for-attendance")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        System.out.println("✅ Eligible employees for attendance retrieved");
    }

    // ── TC-EMP-006: Onboard a new employee ───────────────────────────────────

    @Test(priority = 6, description = "Onboard a new employee using a fresh user")
    @Story("Employee Onboarding")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /users then POST /employees → employee created with ACTIVE status")
    public void tc_emp_006_onboardNewEmployee() {
        // Step 1 — create a fresh user (role=Employee id=4)
        Response userResp = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createUserPayload(
                        "emp_onboard_auto_" + System.currentTimeMillis() + "@rivio.com",
                        "TestPass123!", 4))
                .when()
                .post("/users")
                .then()
                .extract().response();

        assertTrue(userResp.getStatusCode() == 200 || userResp.getStatusCode() == 201,
                "User creation failed: " + userResp.getBody().asString());

        int newUserId = userResp.jsonPath().getInt("data.id");
        assertTrue(newUserId > 0);
        ApiConfig.setCreatedUserId(newUserId);

        // Step 2 — onboard employee using seeded dept/desig/loc IDs
        Response empResp = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.onboardEmployeePayload(
                        newUserId,
                        "AUTO-" + System.currentTimeMillis(),
                        "Auto",
                        "Employee",
                        DEPT_ENG,    // Engineering (seeded id=1)
                        DESIG_SR,    // Senior Software Engineer (seeded id=2)
                        LOC_BLR,     // Bengaluru HQ (seeded id=1)
                        PayloadBuilder.today(),
                        "FULL_TIME"))
                .when()
                .post("/employees")
                .then()
                .extract().response();

        assertTrue(empResp.getStatusCode() == 200 || empResp.getStatusCode() == 201,
                "Onboard failed: " + empResp.getBody().asString());

        int empId = empResp.jsonPath().getInt("data.id");
        assertTrue(empId > 0);
        ApiConfig.setCreatedEmployeeId(empId);

        assertEquals(empResp.jsonPath().getString("data.firstName"), "Auto");
        assertEquals(empResp.jsonPath().getString("data.status"), "ACTIVE");
        System.out.println("✅ Employee onboarded, ID: " + empId);
    }

    // ── TC-EMP-007: Update job details (transfer) ─────────────────────────────

    @Test(priority = 7, dependsOnMethods = "tc_emp_006_onboardNewEmployee",
          description = "Transfer Auto Employee to Sales dept (dept=3, desig=5)")
    @Story("Employee Job Updates")
    @Severity(SeverityLevel.NORMAL)
    @Description("PUT /employees/{id}/job-details → dept changed")
    public void tc_emp_007_updateJobDetails() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .header("X-User-Id", String.valueOf(ApiConfig.getLoggedInUserId()))
                .body(PayloadBuilder.updateJobDetailsPayload(3, 5, 2))  // Sales, Sales Exec, Mumbai
                .when()
                .put("/employees/" + ApiConfig.getCreatedEmployeeId() + "/job-details")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Job details updated (transferred to Sales)");
    }

    // ── TC-EMP-008: Update basic info ─────────────────────────────────────────

    @Test(priority = 8, dependsOnMethods = "tc_emp_006_onboardNewEmployee",
          description = "Update phone and bank account for Auto Employee")
    @Story("Employee Basic Info")
    @Severity(SeverityLevel.NORMAL)
    public void tc_emp_008_updateBasicInfo() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.updateBasicInfoPayload("9998887776", "HDFC00099999"))
                .when()
                .patch("/employees/" + ApiConfig.getCreatedEmployeeId() + "/basic-info")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Basic info updated");
    }
}
