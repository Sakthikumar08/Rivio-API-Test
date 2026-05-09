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
 * TC-AUTH-001 → TC-AUTH-006
 *
 * Seed data (from SQL):
 *   users id=1  admin@rivio.com      role=Super Admin (1)
 *   users id=2  sarah.hr@rivio.com   role=HR Manager  (2)
 *   users id=3  john.manager@rivio.com  role=Department Head (3)
 *   All passwords: password123
 */
@Epic("Rivio HRMS API")
@Feature("Authentication & Users")
public class TC01_AuthTest {

    // ── TC-AUTH-001: Confirm token was captured at startup ────────────────────

    @Test(priority = 1, description = "Verify admin login token is available")
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("ApiConfig logs in at class-load. This test asserts token is non-null.")
    public void tc_auth_001_loginTokenPresent() {
        String token = ApiConfig.getAuthToken();
        assertNotNull(token, "JWT token must not be null — admin@rivio.com / password123 should work");
        assertFalse(token.isEmpty(), "JWT token must not be empty");
        assertEquals(ApiConfig.getLoggedInUserId(), 1, "Logged-in userId should be 1 (admin)");
        System.out.println("✅ Token present. userId=" + ApiConfig.getLoggedInUserId()
                + "  empProfileId=" + ApiConfig.getLoggedInEmployeeProfileId());
    }

    // ── TC-AUTH-002: Live login call validation ───────────────────────────────

    @Test(priority = 2, description = "POST /auth/login with real admin credentials")
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /auth/login — admin@rivio.com / password123 → 200 + token + userId=1")
    public void tc_auth_002_loginLiveCall() {
        Response response = given()
                .spec(ApiConfig.unauthSpec())
                .body(PayloadBuilder.loginPayload(ApiConfig.ADMIN_EMAIL, ApiConfig.ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);

        String token = response.jsonPath().getString("data.token");
        assertNotNull(token, "Token must not be null");
        assertFalse(token.isEmpty());
        assertEquals(response.jsonPath().getInt("data.userId"), 1);
        assertEquals(response.jsonPath().getString("data.role"), "Super Admin");
        System.out.println("✅ Live login success. Role: " + response.jsonPath().getString("data.role"));
    }

    // ── TC-AUTH-003: Login with different seeded user ─────────────────────────

    @Test(priority = 3, description = "Login as HR Manager (sarah.hr@rivio.com)")
    @Story("Login")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /auth/login — sarah.hr@rivio.com / password123 → role=HR Manager")
    public void tc_auth_003_loginAsHrManager() {
        Response response = given()
                .spec(ApiConfig.unauthSpec())
                .body(PayloadBuilder.loginPayload("sarah.hr@rivio.com", ApiConfig.ADMIN_PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        assertEquals(response.jsonPath().getString("data.role"), "HR Manager");
        assertEquals(response.jsonPath().getInt("data.userId"), 2);
        System.out.println("✅ HR Manager login success. userId=" + response.jsonPath().getInt("data.userId"));
    }

    // ── TC-AUTH-004: Invalid credentials rejected ─────────────────────────────

    @Test(priority = 4, description = "Login with wrong password → 401")
    @Story("Login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /auth/login — invalid password → 4xx")
    public void tc_auth_004_loginInvalidCredentials() {
        Response response = given()
                .spec(ApiConfig.unauthSpec())
                .body(PayloadBuilder.loginPayload("admin@rivio.com", "WrongPassword999"))
                .when()
                .post("/auth/login")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() >= 400,
                "Expected 4xx for wrong password, got: " + response.getStatusCode());
        System.out.println("✅ Invalid login rejected with HTTP " + response.getStatusCode());
    }

    // ── TC-AUTH-005: Create + Reset Password + Delete User ───────────────────

    @Test(priority = 5, description = "Create a new user with roleId=4 (Employee)")
    @Story("User Management")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /users — new employee user → id returned, status=ACTIVE")
    public void tc_auth_005_createUser() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createUserPayload("newuser_auto@rivio.com", "TestPass123!", 4))
                .when()
                .post("/users")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode() + " — " + response.getBody().asString());

        int userId = response.jsonPath().getInt("data.id");
        assertTrue(userId > 0);
        ApiConfig.setCreatedUserId(userId);

        assertEquals(response.jsonPath().getString("data.email"), "newuser_auto@rivio.com");
        assertEquals(response.jsonPath().getString("data.status"), "ACTIVE");
        System.out.println("✅ User created, ID: " + userId);
    }

    @Test(priority = 6, description = "Get all users (paginated)")
    @Story("User Management")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /users?page=0&size=10 → at least 5 seeded users")
    public void tc_auth_006_getAllUsers() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/users")
                .then()
                .extract().response();

        AssertionUtils.assertPaginated(response);
        int total = response.jsonPath().getInt("data.totalElements");
        assertTrue(total >= 5, "Should have at least 5 seeded users, found: " + total);
        System.out.println("✅ Users listed. Total: " + total);
    }

    @Test(priority = 7, dependsOnMethods = "tc_auth_005_createUser",
          description = "Reset password for newly created user")
    @Story("User Management")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /users/{id}/reset-password → success")
    public void tc_auth_007_resetPassword() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.resetPasswordPayload("NewSecurePass123!"))
                .when()
                .post("/users/" + ApiConfig.getCreatedUserId() + "/reset-password")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Password reset for userId: " + ApiConfig.getCreatedUserId());
    }

    @Test(priority = 8, dependsOnMethods = "tc_auth_005_createUser",
          description = "Delete the created test user")
    @Story("User Management")
    @Severity(SeverityLevel.NORMAL)
    @Description("DELETE /users/{id} → user deleted or soft-suspended")
    public void tc_auth_008_deleteUser() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .delete("/users/" + ApiConfig.getCreatedUserId())
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ User deleted, ID: " + ApiConfig.getCreatedUserId());
    }
}
