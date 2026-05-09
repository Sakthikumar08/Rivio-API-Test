package com.rivio.tests;

import com.rivio.utils.ApiConfig;
import com.rivio.utils.AssertionUtils;
import com.rivio.utils.PayloadBuilder;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * TC-ROLE-001 → TC-ROLE-007
 *
 * Seeded roles:  1=Super Admin, 2=HR Manager, 3=Department Head, 4=Employee
 * Seeded perms:  1=ALL_ACCESS, 2=APPROVE_LEAVE, 3=RUN_PAYROLL, 4=MANAGE_CANDIDATES, 5=VIEW_PROFILE
 */
@Epic("Rivio HRMS API")
@Feature("Roles & Permissions")
public class TC02_RolesPermissionsTest {

    // ── GET /permissions ──────────────────────────────────────────────────────

    @Test(priority = 1, description = "Get all permissions — expect 5 seeded records")
    @Story("Permissions")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /permissions → list includes ALL_ACCESS, APPROVE_LEAVE, etc.")
    public void tc_role_001_getAllPermissions() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/permissions")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> perms = response.jsonPath().getList("data");
        assertTrue(perms.size() >= 5, "Expected at least 5 seeded permissions, got: " + perms.size());

        // Verify shape
        assertNotNull(response.jsonPath().get("data[0].id"));
        assertNotNull(response.jsonPath().get("data[0].keyName"));
        assertNotNull(response.jsonPath().get("data[0].module"));
        System.out.println("✅ Permissions fetched. Count: " + perms.size());
    }

    // ── GET /roles ────────────────────────────────────────────────────────────

    @Test(priority = 2, description = "Get all roles — expect 4 seeded roles")
    @Story("Roles")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /roles → Super Admin, HR Manager, Department Head, Employee")
    public void tc_role_002_getAllRoles() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/roles")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> roles = response.jsonPath().getList("data");
        assertTrue(roles.size() >= 4, "Expected at least 4 seeded roles, got: " + roles.size());
        System.out.println("✅ Roles fetched. Count: " + roles.size());
    }

    // ── GET /roles/1 ──────────────────────────────────────────────────────────

    @Test(priority = 3, description = "Get role by ID — Super Admin (id=1)")
    @Story("Roles")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /roles/1 → name=Super Admin")
    public void tc_role_003_getRoleById() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/roles/1")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        assertEquals(response.jsonPath().getInt("data.id"), 1);
        assertEquals(response.jsonPath().getString("data.name"), "Super Admin");
        System.out.println("✅ Role by ID retrieved: Super Admin");
    }

    // ── POST /roles ───────────────────────────────────────────────────────────

    private static String createdRoleName;
    private static String updatedRoleName;

    @Test(priority = 4, description = "Create a new custom role")
    @Story("Roles")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /roles → new role created with returned ID")
    public void tc_role_004_createRole() {
        long ts = System.currentTimeMillis();
        createdRoleName = "Automation QA Lead " + ts;
        updatedRoleName = "Automation QA Lead Updated " + ts;
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createRolePayload(createdRoleName))
                .when()
                .post("/roles")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode());

        int roleId = response.jsonPath().getInt("data.id");
        assertTrue(roleId > 0);
        assertEquals(response.jsonPath().getString("data.name"), createdRoleName);
        ApiConfig.setCreatedRoleId(roleId);
        System.out.println("✅ Role created, ID: " + roleId);
    }

    // ── PUT /roles/{id} ───────────────────────────────────────────────────────

    @Test(priority = 5, dependsOnMethods = "tc_role_004_createRole",
          description = "Update the created role name")
    @Story("Roles")
    @Severity(SeverityLevel.NORMAL)
    @Description("PUT /roles/{id} → name updated")
    public void tc_role_005_updateRole() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createRolePayload(updatedRoleName))
                .when()
                .put("/roles/" + ApiConfig.getCreatedRoleId())
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        assertEquals(response.jsonPath().getString("data.name"), updatedRoleName);
        System.out.println("✅ Role updated: " + ApiConfig.getCreatedRoleId());
    }

    // ── GET /roles/2/permissions ──────────────────────────────────────────────

    @Test(priority = 6, description = "Get permissions for HR Manager (role id=2)")
    @Story("Permissions")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /roles/2/permissions → APPROVE_LEAVE, RUN_PAYROLL, MANAGE_CANDIDATES, VIEW_PROFILE")
    public void tc_role_006_getPermissionsForRole() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/roles/2/permissions")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> perms = response.jsonPath().getList("data");
        assertTrue(perms.size() >= 4, "HR Manager should have 4 permissions, got: " + perms.size());
        System.out.println("✅ HR Manager permissions count: " + perms.size());
    }

    // ── POST /roles/{id}/permissions ─────────────────────────────────────────

    @Test(priority = 7, dependsOnMethods = "tc_role_004_createRole",
          description = "Bind permissions 2,5 (APPROVE_LEAVE + VIEW_PROFILE) to created role")
    @Story("Permissions")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /roles/{id}/permissions — body: [2, 5] → permissions replaced")
    public void tc_role_007_bindPermissionsToRole() {
        List<Integer> permIds = List.of(2, 5);   // seeded permission IDs

        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(permIds)
                .when()
                .post("/roles/" + ApiConfig.getCreatedRoleId() + "/permissions")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Permissions [2,5] bound to role: " + ApiConfig.getCreatedRoleId());
    }
}
