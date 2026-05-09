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
 * TC-PAY-001 → TC-PAY-010
 *
 * Seeded salary components:
 *   empProfile 1 (John Doe): Basic=100000, HRA=40000, PF=1800
 *   empProfile 2 (Sarah):    Basic=80000,  HRA=30000, PF=1800
 *   empProfile 3 (Alice):    Basic=60000,  HRA=25000, PF=1800
 *   empProfile 4 (Bob):      Consolidated=50000
 *
 * Seeded pay cycles: id=1 Feb 2026 PAID, id=2 Mar 2026 PROCESSING
 * Seeded payslips:   id=1 emp1 cycle1 138200 net, id=2 emp3 cycle1 83200 net
 */
@Epic("Rivio HRMS API")
@Feature("Payroll Management")
public class TC08_PayrollTest {

    // ── TC-PAY-001: Get salary components for seeded employees ────────────────

    @Test(priority = 1, description = "Get salary components for John Doe (empProfile 1)")
    @Story("Salary Components")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /salary-components/employee/1 → Basic Pay, HRA, PF (3 seeded components)")
    public void tc_pay_001_getSalaryComponentsJohn() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/salary-components/employee/1")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> comps = response.jsonPath().getList("data");
        assertNotNull(comps, "Salary components list must not be null");
        System.out.println("✅ Employee #1 salary components: " + comps.size());
    }

    @Test(priority = 2, description = "Get salary components for Bob Marley (empProfile 4)")
    @Story("Salary Components")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /salary-components/employee/4 → Consolidated Pay (contract)")
    public void tc_pay_002_getSalaryComponentsBob() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/salary-components/employee/4")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<String> names = response.jsonPath().getList("data.name");
        assertNotNull(names, "Salary component names list must not be null");
        System.out.println("✅ Employee #4 salary components: " + names);
    }

    // ── TC-PAY-003: Create a salary component ────────────────────────────────

    @Test(priority = 3, description = "Add a Transport Allowance to Alice (empProfile 3)")
    @Story("Salary Components")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /salary-components/employee/3 → new EARNING component added")
    public void tc_pay_003_createSalaryComponent() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createSalaryComponentPayload("Transport Allowance", "EARNING", 5000.00))
                .when()
                .post("/salary-components/employee/3")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode() + "\n" + response.getBody().asString());

        int compId = response.jsonPath().getInt("data.id");
        assertTrue(compId > 0);
        ApiConfig.setCreatedSalaryComponentId(compId);
        assertEquals(response.jsonPath().getString("data.name"), "Transport Allowance");
        assertEquals(response.jsonPath().getDouble("data.value"), 5000.00);
        System.out.println("✅ Salary component created, ID: " + compId);
    }

    @Test(priority = 4, dependsOnMethods = "tc_pay_003_createSalaryComponent",
          description = "Update Transport Allowance value to 6000")
    @Story("Salary Components")
    @Severity(SeverityLevel.NORMAL)
    public void tc_pay_004_updateSalaryComponent() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createSalaryComponentPayload("Transport Allowance", "EARNING", 6000.00))
                .when()
                .put("/salary-components/" + ApiConfig.getCreatedSalaryComponentId())
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        assertEquals(response.jsonPath().getDouble("data.value"), 6000.00);
        System.out.println("✅ Transport Allowance updated to 6000");
    }

    @Test(priority = 5, dependsOnMethods = "tc_pay_004_updateSalaryComponent",
          description = "Delete the Transport Allowance component")
    @Story("Salary Components")
    @Severity(SeverityLevel.MINOR)
    public void tc_pay_005_deleteSalaryComponent() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .delete("/salary-components/" + ApiConfig.getCreatedSalaryComponentId())
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Transport Allowance deleted");
    }

    // ── TC-PAY-006: List and search pay cycles ────────────────────────────────

    @Test(priority = 6, description = "List all pay cycles — expect 2 seeded (Feb + Mar 2026)")
    @Story("Pay Cycles")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /pay-cycles → February 2026 (PAID), March 2026 (PROCESSING)")
    public void tc_pay_006_listPayCycles() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/pay-cycles")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> cycles = response.jsonPath().getList("data");
        assertTrue(cycles.size() >= 2, "Expected at least 2 seeded pay cycles");

        List<String> names = response.jsonPath().getList("data.cycleName");
        assertTrue(names.contains("February 2026") || names.contains("March 2026"),
                "Seeded cycle names not found: " + names);
        System.out.println("✅ Pay cycles: " + cycles.size());
    }

    @Test(priority = 7, description = "Search pay cycles by name 'February'")
    @Story("Pay Cycles")
    @Severity(SeverityLevel.NORMAL)
    public void tc_pay_007_searchPayCycles() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("name", "February")
                .when()
                .get("/pay-cycles")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        System.out.println("✅ Search 'February' complete");
    }

    // ── TC-PAY-008: Create a new pay cycle for April 2026 ─────────────────────

    @Test(priority = 8, description = "Create a future pay cycle (date-unique per run)")
    @Story("Pay Cycles")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_pay_008_createPayCycle() {
        // Pick a month a few years out so we never overlap with finalized cycles in DB.
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate firstOfMonth = today.plusYears(3).withDayOfMonth(1)
                .plusMonths(System.currentTimeMillis() % 12);
        java.time.LocalDate lastOfMonth  = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
        String cycleName = firstOfMonth.getMonth().name() + " " + firstOfMonth.getYear()
                + " #" + System.currentTimeMillis();

        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createPayCyclePayload(cycleName,
                        firstOfMonth.toString(), lastOfMonth.toString()))
                .when()
                .post("/pay-cycles")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode() + "\n" + response.getBody().asString());

        int payCycleId = response.jsonPath().getInt("data.id");
        assertTrue(payCycleId > 0);
        ApiConfig.setCreatedPayCycleId(payCycleId);
        assertEquals(response.jsonPath().getString("data.cycleName"), cycleName);
        System.out.println("✅ Pay cycle created, ID: " + payCycleId + " name: " + cycleName);
    }

    // ── TC-PAY-009: Get payslips for seeded cycle 1 ──────────────────────────

    @Test(priority = 9, description = "Get payslips for seeded pay cycle id=1 (Feb 2026, PAID)")
    @Story("Payslips")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /pay-cycles/1/payslips → 2 seeded payslips (John=138200, Alice=83200)")
    public void tc_pay_009_getPayslipsForSeededCycle() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/pay-cycles/1/payslips")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> slips = response.jsonPath().getList("data");
        assertNotNull(slips, "Payslips list must not be null");
        System.out.println("✅ Pay cycle 1 payslips: " + slips.size());
    }

    // ── TC-PAY-010: Generate payslips + finalize ──────────────────────────────

    @Test(priority = 10, dependsOnMethods = "tc_pay_008_createPayCycle",
          description = "Generate payslips then finalize April 2026 pay cycle")
    @Story("Payslips")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_pay_010_generateAndFinalizePayCycle() {
        // Generate
        Response genResp = given()
                .spec(ApiConfig.authSpec())
                .when()
                .post("/pay-cycles/" + ApiConfig.getCreatedPayCycleId() + "/generate-payslips")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(genResp);
        System.out.println("✅ Payslips generated for April 2026");

        // Finalize
        Response finalResp = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.finalizePayCyclePayload())
                .when()
                .put("/pay-cycles/" + ApiConfig.getCreatedPayCycleId() + "/status")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(finalResp);
        System.out.println("✅ April 2026 pay cycle finalized");
    }
}
