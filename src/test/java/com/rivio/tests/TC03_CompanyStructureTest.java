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
 * TC-CS-001 → TC-CS-013
 *
 * Seeded:
 *   Locations:    1=Bengaluru HQ, 2=Mumbai Branch, 3=New York Office
 *   Departments:  1=Engineering, 2=Human Resources, 3=Sales
 *   Designations: 1=VP of Engineering, 2=Senior SE, 3=Frontend Dev, 4=HR Director, 5=Sales Executive
 */
@Epic("Rivio HRMS API")
@Feature("Company Structure")
public class TC03_CompanyStructureTest {

    // ══ LOCATIONS ══════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Get all locations — expect 3 seeded")
    @Story("Locations")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /locations → Bengaluru HQ, Mumbai Branch, New York Office")
    public void tc_cs_001_getAllLocations() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/locations")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> locs = response.jsonPath().getList("data");
        assertTrue(locs.size() >= 3, "Expected at least 3 seeded locations, got: " + locs.size());

        // Validate Bengaluru HQ (id=1) is present
        List<String> names = response.jsonPath().getList("data.name");
        assertTrue(names.contains("Bengaluru HQ"), "Bengaluru HQ should be in locations");
        System.out.println("✅ Locations: " + locs.size());
    }

    @Test(priority = 2, description = "Create a new location — Chennai Office")
    @Story("Locations")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_cs_002_createLocation() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createLocationPayload("Chennai Office", "INR", "Asia/Kolkata"))
                .when()
                .post("/locations")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode() + "\n" + response.getBody().asString());

        int locationId = response.jsonPath().getInt("data.id");
        assertTrue(locationId > 0);
        assertEquals(response.jsonPath().getString("data.name"), "Chennai Office");
        ApiConfig.setCreatedLocationId(locationId);
        System.out.println("✅ Location created, ID: " + locationId);
    }

    @Test(priority = 3, dependsOnMethods = "tc_cs_002_createLocation",
          description = "Update Chennai Office → Pune Office")
    @Story("Locations")
    @Severity(SeverityLevel.NORMAL)
    public void tc_cs_003_updateLocation() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createLocationPayload("Pune Office", "INR", "Asia/Kolkata"))
                .when()
                .put("/locations/" + ApiConfig.getCreatedLocationId())
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        assertEquals(response.jsonPath().getString("data.name"), "Pune Office");
        System.out.println("✅ Location updated: " + ApiConfig.getCreatedLocationId());
    }

    // ══ DEPARTMENTS ════════════════════════════════════════════════════════════

    @Test(priority = 4, description = "Get all departments — expect 3 seeded")
    @Story("Departments")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /departments → Engineering, Human Resources, Sales")
    public void tc_cs_004_getAllDepartments() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/departments")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> depts = response.jsonPath().getList("data");
        assertTrue(depts.size() >= 3, "Expected at least 3 seeded departments");

        List<String> names = response.jsonPath().getList("data.name");
        assertTrue(names.contains("Engineering"));
        assertTrue(names.contains("Human Resources"));
        System.out.println("✅ Departments: " + depts.size());
    }

    @Test(priority = 5, description = "Create a new QA department (manager=userId 1)")
    @Story("Departments")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_cs_005_createDepartment() {
        // manager userId=1 (admin) is seeded
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createDepartmentPayload("Quality Assurance", 1))
                .when()
                .post("/departments")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode() + "\n" + response.getBody().asString());

        int deptId = response.jsonPath().getInt("data.id");
        assertTrue(deptId > 0);
        assertEquals(response.jsonPath().getString("data.name"), "Quality Assurance");
        ApiConfig.setCreatedDepartmentId(deptId);
        System.out.println("✅ Department created, ID: " + deptId);
    }

    @Test(priority = 6, dependsOnMethods = "tc_cs_005_createDepartment",
          description = "Update QA Department name")
    @Story("Departments")
    @Severity(SeverityLevel.NORMAL)
    public void tc_cs_006_updateDepartment() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createDepartmentPayload("QA & Testing", 1))
                .when()
                .put("/departments/" + ApiConfig.getCreatedDepartmentId())
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Department updated: " + ApiConfig.getCreatedDepartmentId());
    }

    // ══ DESIGNATIONS ══════════════════════════════════════════════════════════

    @Test(priority = 7, description = "Get all designations — expect 5 seeded")
    @Story("Designations")
    @Severity(SeverityLevel.NORMAL)
    public void tc_cs_007_getAllDesignations() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/designations")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> desigs = response.jsonPath().getList("data");
        assertTrue(desigs.size() >= 5, "Expected at least 5 seeded designations");
        System.out.println("✅ Designations: " + desigs.size());
    }

    @Test(priority = 8, description = "Get designations filtered by Engineering dept (id=1)")
    @Story("Designations")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /designations?departmentId=1 → VP of Engineering, Senior SE, Frontend Dev")
    public void tc_cs_008_getDesignationsByDepartment() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("departmentId", 1)
                .when()
                .get("/designations")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> desigs = response.jsonPath().getList("data");
        assertTrue(desigs.size() >= 3, "Engineering should have at least 3 designations");
        System.out.println("✅ Engineering designations: " + desigs.size());
    }

    @Test(priority = 9, dependsOnMethods = "tc_cs_005_createDepartment",
          description = "Create QA Automation Engineer designation under QA dept")
    @Story("Designations")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_cs_009_createDesignation() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createDesignationPayload(
                        "QA Automation Engineer", ApiConfig.getCreatedDepartmentId()))
                .when()
                .post("/designations")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode() + "\n" + response.getBody().asString());

        int desigId = response.jsonPath().getInt("data.id");
        assertTrue(desigId > 0);
        assertEquals(response.jsonPath().getString("data.title"), "QA Automation Engineer");
        ApiConfig.setCreatedDesignationId(desigId);
        System.out.println("✅ Designation created, ID: " + desigId);
    }

    @Test(priority = 10, dependsOnMethods = "tc_cs_009_createDesignation",
          description = "Update designation title")
    @Story("Designations")
    @Severity(SeverityLevel.NORMAL)
    public void tc_cs_010_updateDesignation() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createDesignationPayload(
                        "Senior QA Automation Engineer", ApiConfig.getCreatedDepartmentId()))
                .when()
                .put("/designations/" + ApiConfig.getCreatedDesignationId())
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Designation updated: " + ApiConfig.getCreatedDesignationId());
    }

    // ── Cleanup (reverse order) ───────────────────────────────────────────────

    @Test(priority = 11, dependsOnMethods = "tc_cs_010_updateDesignation",
          description = "Delete the test designation")
    @Story("Designations") @Severity(SeverityLevel.MINOR)
    public void tc_cs_011_deleteDesignation() {
        Response r = given().spec(ApiConfig.authSpec()).when()
                .delete("/designations/" + ApiConfig.getCreatedDesignationId()).then().extract().response();
        AssertionUtils.assertSuccess(r);
        System.out.println("✅ Designation deleted");
    }

    @Test(priority = 12, dependsOnMethods = "tc_cs_011_deleteDesignation",
          description = "Delete the test department")
    @Story("Departments") @Severity(SeverityLevel.MINOR)
    public void tc_cs_012_deleteDepartment() {
        Response r = given().spec(ApiConfig.authSpec()).when()
                .delete("/departments/" + ApiConfig.getCreatedDepartmentId()).then().extract().response();
        AssertionUtils.assertSuccess(r);
        System.out.println("✅ Department deleted");
    }

    @Test(priority = 13, dependsOnMethods = "tc_cs_003_updateLocation",
          description = "Delete the test location")
    @Story("Locations") @Severity(SeverityLevel.MINOR)
    public void tc_cs_013_deleteLocation() {
        Response r = given().spec(ApiConfig.authSpec()).when()
                .delete("/locations/" + ApiConfig.getCreatedLocationId()).then().extract().response();
        AssertionUtils.assertSuccess(r);
        System.out.println("✅ Location deleted");
    }
}
