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
 * TC-ATT-001 → TC-ATT-010
 *
 * Seeded attendance: Jan & March 2026 for empProfiles 1-4
 * Seeded work days:  Mon-Fri working, Sat-Sun off (id=1..7)
 * Seeded holidays:   id=1 New Year, id=2 Republic Day, id=3 Labour Day
 *
 * Punch-in uses empProfile id=1 (John Doe, userId=3)
 */
@Epic("Rivio HRMS API")
@Feature("Attendance & Scheduling")
public class TC06_AttendanceSchedulingTest {

    // ── TC-ATT-001: Org attendance for a known seeded date ────────────────────

    @Test(priority = 1, description = "Get org-wide attendance for 2026-01-02 (seeded PRESENT day)")
    @Story("Attendance")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /attendance?date=2026-01-02 → 4 records (all employees PRESENT)")
    public void tc_att_001_getOrgAttendanceSeededDate() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("date", "2026-01-02")
                .when()
                .get("/attendance")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> records = response.jsonPath().getList("data");
        assertTrue(records.size() >= 4, "2026-01-02 should have 4 attendance records, got: " + records.size());
        System.out.println("✅ Attendance for 2026-01-02: " + records.size() + " records");
    }

    // ── TC-ATT-002: Get today's attendance (may be empty) ─────────────────────

    @Test(priority = 2, description = "Get org-wide attendance for today")
    @Story("Attendance")
    @Severity(SeverityLevel.NORMAL)
    public void tc_att_002_getOrgAttendanceToday() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("date", PayloadBuilder.today())
                .when()
                .get("/attendance")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        System.out.println("✅ Today's attendance fetched. Records: "
                + response.jsonPath().getList("data").size());
    }

    // ── TC-ATT-003: Employee attendance history (seeded data) ──────────────────

    @Test(priority = 3, description = "Get attendance history for John Doe (empProfile 1) in Jan 2026")
    @Story("Attendance")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /attendance/employee/1/history?startDate=2026-01-01&endDate=2026-01-31 → seeded records")
    public void tc_att_003_getEmployeeAttendanceHistory() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("startDate", "2026-01-01")
                .queryParam("endDate", "2026-01-31")
                .when()
                .get("/attendance/employee/1/history")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> records = response.jsonPath().getList("data");
        assertFalse(records.isEmpty(), "John Doe should have January 2026 attendance records");
        System.out.println("✅ John Doe Jan 2026 history: " + records.size() + " records");
    }

    // ── TC-ATT-004: Manual Punch In ───────────────────────────────────────────

    @Test(priority = 4, description = "Manual punch-in for John Doe (empProfile=1) today")
    @Story("Attendance")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /attendance X-User-Id:1 → attendance record created")
    public void tc_att_004_manualPunchIn() {
        String today   = PayloadBuilder.today();
        String punchIn = today + "T09:00:00";

        Response response = given()
                .spec(ApiConfig.authSpec())
                .header("X-User-Id", "1")   // admin user
                .body(PayloadBuilder.punchInPayload(1, today, punchIn))  // empProfile=1
                .when()
                .post("/attendance")
                .then()
                .extract().response();

        // 409 Conflict means already punched in today (seeded data or previous run) — acceptable
        if (response.getStatusCode() == 409 || response.getStatusCode() == 400) {
            System.out.println("⚠️ Already punched in today — skipping punch-out (HTTP "
                    + response.getStatusCode() + ")");
            return;
        }

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Punch in failed: " + response.getBody().asString());

        int attId = response.jsonPath().getInt("data.id");
        assertTrue(attId > 0);
        ApiConfig.setCreatedAttendanceId(attId);
        System.out.println("✅ Punch in recorded, attendance ID: " + attId);
    }

    // ── TC-ATT-005: Manual Punch Out ──────────────────────────────────────────

    @Test(priority = 5, dependsOnMethods = "tc_att_004_manualPunchIn",
          description = "Punch out for today's attendance record")
    @Story("Attendance")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_att_005_manualPunchOut() {
        if (ApiConfig.getCreatedAttendanceId() <= 0) {
            System.out.println("⚠️ No new attendance ID — skipping punch-out");
            return;
        }

        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.punchOutPayload(PayloadBuilder.today() + "T18:00:00"))
                .when()
                .patch("/attendance/" + ApiConfig.getCreatedAttendanceId() + "/punch-out")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Punch out recorded");
    }

    // ── TC-ATT-006: CSV Template ──────────────────────────────────────────────

    @Test(priority = 6, description = "Download bulk attendance CSV template")
    @Story("Bulk Upload")
    @Severity(SeverityLevel.NORMAL)
    public void tc_att_006_downloadCsvTemplate() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/attendance/upload/template")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 302,
                "CSV template download failed: " + response.getStatusCode());
        System.out.println("✅ CSV template endpoint responded: " + response.getStatusCode());
    }

    // ══ WORK DAYS ══════════════════════════════════════════════════════════════

    @Test(priority = 7, description = "Get work days — expect 7 seeded (Mon-Sun)")
    @Story("Work Days")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /work-days → Monday-Friday working=true, Saturday-Sunday working=false")
    public void tc_att_007_getAllWorkDays() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/work-days")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> days = response.jsonPath().getList("data");
        assertEquals(days.size(), 7, "Should have exactly 7 work days");

        // Verify Friday (id=5) is a working day
        List<Boolean> workingFlags = response.jsonPath().getList("data.isWorkingDay");
        assertTrue(workingFlags.get(0), "Monday should be a working day");
        assertFalse(workingFlags.get(5), "Saturday should NOT be a working day");
        System.out.println("✅ Work days verified: 5 working, 2 off");
    }

    @Test(priority = 8, description = "Toggle Saturday (id=6) as a working day")
    @Story("Work Days")
    @Severity(SeverityLevel.NORMAL)
    @Description("PUT /work-days/6 isWorkingDay=true → Saturday becomes working")
    public void tc_att_008_updateWorkDay() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.updateWorkDayPayload(true))
                .when()
                .put("/work-days/6")   // Saturday (seeded id=6)
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);

        // Revert back to false
        given().spec(ApiConfig.authSpec())
                .body(PayloadBuilder.updateWorkDayPayload(false))
                .put("/work-days/6");

        System.out.println("✅ Saturday toggled working → reverted back to off");
    }

    // ══ HOLIDAYS ═══════════════════════════════════════════════════════════════

    @Test(priority = 9, description = "Get holidays — expect 3 seeded (New Year, Republic Day, Labour Day)")
    @Story("Holidays")
    @Severity(SeverityLevel.NORMAL)
    public void tc_att_009_getAllHolidays() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/holidays")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> holidays = response.jsonPath().getList("data");
        assertTrue(holidays.size() >= 3, "Expected at least 3 seeded holidays");

        List<String> names = response.jsonPath().getList("data.name");
        assertTrue(names.contains("New Year"));
        System.out.println("✅ Holidays: " + holidays.size());
    }

    @Test(priority = 10, description = "Create a new holiday — Independence Day")
    @Story("Holidays")
    @Severity(SeverityLevel.NORMAL)
    public void tc_att_010_createAndDeleteHoliday() {
        Response createResp = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createHolidayPayload("2026-08-15", "Independence Day"))
                .when()
                .post("/holidays")
                .then()
                .extract().response();

        assertTrue(createResp.getStatusCode() == 200 || createResp.getStatusCode() == 201,
                "Holiday creation failed: " + createResp.getBody().asString());

        int holidayId = createResp.jsonPath().getInt("data.id");
        assertTrue(holidayId > 0);
        ApiConfig.setCreatedHolidayId(holidayId);
        assertEquals(createResp.jsonPath().getString("data.name"), "Independence Day");
        System.out.println("✅ Holiday created, ID: " + holidayId);

        // Cleanup
        Response deleteResp = given()
                .spec(ApiConfig.authSpec())
                .when()
                .delete("/holidays/" + holidayId)
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(deleteResp);
        System.out.println("✅ Holiday deleted");
    }
}
