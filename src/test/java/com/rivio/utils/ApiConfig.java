package com.rivio.utils;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.restassured.RestAssured.given;

/**
 * Central config for Rivio HRMS API tests.
 *
 * KEY FIX: Credentials are loaded from config.properties so they can be
 * changed without touching Java code. The static initialiser attempts login
 * with the primary password then tries every fallback in order.
 */
public class ApiConfig {

    private static final Properties PROPS = loadProps();

    public static final String BASE_URL       = PROPS.getProperty("base.url",
            "https://riviobackendspringboot-production.up.railway.app/api");
    public static final String ADMIN_EMAIL    = PROPS.getProperty("admin.email",    "admin@rivio.com");
    public static final String ADMIN_PASSWORD = PROPS.getProperty("admin.password", "Admin@123");

    private static final String[] FALLBACKS   = PROPS.getProperty("admin.password.fallbacks", "")
            .split(",");

    // ── Shared runtime state ──────────────────────────────────────────────────
    private static volatile String authToken;
    private static volatile int    loggedInUserId;
    private static volatile int    loggedInEmployeeProfileId;

    private static int createdUserId;
    private static int createdRoleId;
    private static int createdLocationId;
    private static int createdDepartmentId;
    private static int createdDesignationId;
    private static int createdEmployeeId;
    private static int createdLeaveTypeId;
    private static int createdLeaveRequestId;
    private static int createdAttendanceId;
    private static int createdHolidayId;
    private static int createdJobOpeningId;
    private static int createdCandidateId;
    private static int createdSalaryComponentId;
    private static int createdPayCycleId;

    // ── Static initialiser: login once at class-load ──────────────────────────
    static {
        performLogin();
    }

    public static void performLogin() {
        if (tryLogin(ADMIN_EMAIL, ADMIN_PASSWORD)) return;
        for (String fb : FALLBACKS) {
            String pwd = fb.trim();
            if (!pwd.isEmpty() && tryLogin(ADMIN_EMAIL, pwd)) return;
        }
        throw new RuntimeException(
            "Login FAILED for '" + ADMIN_EMAIL + "' with all configured passwords.\n" +
            "▶ Update admin.password in src/test/resources/config.properties and rerun.\n" +
            "  You can also add more candidates to admin.password.fallbacks.");
    }

    private static boolean tryLogin(String email, String password) {
        try {
            System.out.println("[ApiConfig] Trying: " + email + " / " + mask(password));
            Response r = given()
                    .baseUri(BASE_URL)
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                    .post("/auth/login");

            if (r.getStatusCode() == 200
                    && Boolean.TRUE.equals(r.jsonPath().getBoolean("success"))) {
                authToken                 = r.jsonPath().getString("data.token");
                loggedInUserId            = r.jsonPath().getInt("data.userId");
                loggedInEmployeeProfileId = r.jsonPath().getInt("data.employeeProfileId");
                System.out.println("[ApiConfig] ✅ Login OK  userId=" + loggedInUserId
                        + "  empProfileId=" + loggedInEmployeeProfileId);
                return true;
            }
            System.out.println("[ApiConfig] ❌ HTTP " + r.getStatusCode()
                    + " — " + r.jsonPath().getString("message"));
            return false;
        } catch (Exception e) {
            System.out.println("[ApiConfig] ❌ Exception: " + e.getMessage());
            return false;
        }
    }

    private static String mask(String p) {
        if (p == null || p.length() <= 2) return "***";
        return p.charAt(0) + "***" + p.charAt(p.length() - 1);
    }

    // ── Specs ─────────────────────────────────────────────────────────────────

    public static RequestSpecification unauthSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .log(LogDetail.ALL)
                .build();
    }

    public static RequestSpecification authSpec() {
        if (authToken == null) performLogin();
        if (authToken == null)
            throw new IllegalStateException(
                "Auth token is null — login failed. Fix credentials in config.properties.");
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + authToken)
                .addFilter(new AllureRestAssured())
                .log(LogDetail.ALL)
                .build();
    }

    // ── Properties loader ─────────────────────────────────────────────────────

    private static Properties loadProps() {
        Properties p = new Properties();
        try (InputStream is = ApiConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) { p.load(is); System.out.println("[ApiConfig] Loaded config.properties"); }
            else             { System.out.println("[ApiConfig] config.properties not found — using defaults"); }
        } catch (IOException e) {
            System.out.println("[ApiConfig] Failed to load config.properties: " + e.getMessage());
        }
        return p;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public static String getAuthToken()                     { return authToken; }
    public static void   setAuthToken(String t)             { authToken = t; }

    public static int  getLoggedInUserId()                  { return loggedInUserId; }
    public static void setLoggedInUserId(int id)            { loggedInUserId = id; }

    public static int  getLoggedInEmployeeProfileId()       { return loggedInEmployeeProfileId; }
    public static void setLoggedInEmployeeProfileId(int id) { loggedInEmployeeProfileId = id; }

    public static int  getCreatedUserId()                   { return createdUserId; }
    public static void setCreatedUserId(int id)             { createdUserId = id; }

    public static int  getCreatedRoleId()                   { return createdRoleId; }
    public static void setCreatedRoleId(int id)             { createdRoleId = id; }

    public static int  getCreatedLocationId()               { return createdLocationId; }
    public static void setCreatedLocationId(int id)         { createdLocationId = id; }

    public static int  getCreatedDepartmentId()             { return createdDepartmentId; }
    public static void setCreatedDepartmentId(int id)       { createdDepartmentId = id; }

    public static int  getCreatedDesignationId()            { return createdDesignationId; }
    public static void setCreatedDesignationId(int id)      { createdDesignationId = id; }

    public static int  getCreatedEmployeeId()               { return createdEmployeeId; }
    public static void setCreatedEmployeeId(int id)         { createdEmployeeId = id; }

    public static int  getCreatedLeaveTypeId()              { return createdLeaveTypeId; }
    public static void setCreatedLeaveTypeId(int id)        { createdLeaveTypeId = id; }

    public static int  getCreatedLeaveRequestId()           { return createdLeaveRequestId; }
    public static void setCreatedLeaveRequestId(int id)     { createdLeaveRequestId = id; }

    public static int  getCreatedAttendanceId()             { return createdAttendanceId; }
    public static void setCreatedAttendanceId(int id)       { createdAttendanceId = id; }

    public static int  getCreatedHolidayId()                { return createdHolidayId; }
    public static void setCreatedHolidayId(int id)          { createdHolidayId = id; }

    public static int  getCreatedJobOpeningId()             { return createdJobOpeningId; }
    public static void setCreatedJobOpeningId(int id)       { createdJobOpeningId = id; }

    public static int  getCreatedCandidateId()              { return createdCandidateId; }
    public static void setCreatedCandidateId(int id)        { createdCandidateId = id; }

    public static int  getCreatedSalaryComponentId()        { return createdSalaryComponentId; }
    public static void setCreatedSalaryComponentId(int id)  { createdSalaryComponentId = id; }

    public static int  getCreatedPayCycleId()               { return createdPayCycleId; }
    public static void setCreatedPayCycleId(int id)         { createdPayCycleId = id; }
}
