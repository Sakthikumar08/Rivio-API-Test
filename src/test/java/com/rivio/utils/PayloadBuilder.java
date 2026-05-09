package com.rivio.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory methods for building request payload maps.
 * Using Map<String, Object> keeps the code free of DTO classes
 * and serialises cleanly via Jackson / RestAssured.
 */
public class PayloadBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Auth ────────────────────────────────────────────────────────────────

    public static Map<String, Object> loginPayload(String email, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("password", password);
        return map;
    }

    public static Map<String, Object> createUserPayload(String email, String password, int roleId) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("password", password);
        map.put("roleId", roleId);
        return map;
    }

    public static Map<String, Object> resetPasswordPayload(String newPassword) {
        Map<String, Object> map = new HashMap<>();
        map.put("newPassword", newPassword);
        return map;
    }

    // ── Roles ───────────────────────────────────────────────────────────────

    public static Map<String, Object> createRolePayload(String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        return map;
    }

    // ── Company Structure ────────────────────────────────────────────────────

    public static Map<String, Object> createLocationPayload(String name, String currencyCode, String timezone) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("currencyCode", currencyCode);
        map.put("timezone", timezone);
        return map;
    }

    public static Map<String, Object> createDepartmentPayload(String name, int managerUserId) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("managerUserId", managerUserId);
        return map;
    }

    public static Map<String, Object> createDesignationPayload(String title, int departmentId) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("departmentId", departmentId);
        return map;
    }

    // ── Employees ────────────────────────────────────────────────────────────

    public static Map<String, Object> onboardEmployeePayload(int userId, String code,
                                                              String firstName, String lastName,
                                                              int deptId, int desigId, int locId,
                                                              String joiningDate, String empType) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("employeeCode", code);
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("departmentId", deptId);
        map.put("designationId", desigId);
        map.put("locationId", locId);
        map.put("reportsToProfileId", null);
        map.put("joiningDate", joiningDate);
        map.put("employmentType", empType);
        return map;
    }

    public static Map<String, Object> updateJobDetailsPayload(int deptId, int desigId, int locId) {
        Map<String, Object> map = new HashMap<>();
        map.put("departmentId", deptId);
        map.put("designationId", desigId);
        map.put("locationId", locId);
        return map;
    }

    public static Map<String, Object> updateBasicInfoPayload(String phoneNo, String bankAccount) {
        Map<String, Object> map = new HashMap<>();
        map.put("phoneNo", phoneNo);
        map.put("bankAccount", bankAccount);
        return map;
    }

    public static Map<String, Object> updateEmployeeStatusPayload(String status, String exitDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("exitDate", exitDate);
        map.put("overridePastDate", false);
        return map;
    }

    // ── Leave ────────────────────────────────────────────────────────────────

    public static Map<String, Object> createLeaveTypePayload(String name, double allotment, double carryForward) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("yearlyAllotment", allotment);
        map.put("carryForwardLimit", carryForward);
        return map;
    }

    public static Map<String, Object> allocateBalancesPayload(int year) {
        Map<String, Object> map = new HashMap<>();
        map.put("year", year);
        return map;
    }

    public static Map<String, Object> applyLeavePayload(int employeeProfileId, int leaveTypeId,
                                                          String startDate, String endDate, double days) {
        Map<String, Object> map = new HashMap<>();
        map.put("employeeProfileId", employeeProfileId);
        map.put("leaveTypeId", leaveTypeId);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("daysRequested", days);
        return map;
    }

    public static Map<String, Object> leaveStatusPayload(String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        return map;
    }

    // ── Attendance ───────────────────────────────────────────────────────────

    public static Map<String, Object> punchInPayload(int employeeProfileId, String date,
                                                       String punchIn) {
        Map<String, Object> map = new HashMap<>();
        map.put("employeeProfileId", employeeProfileId);
        map.put("date", date);
        map.put("punchIn", punchIn);
        map.put("punchOut", null);
        return map;
    }

    public static Map<String, Object> punchOutPayload(String punchOut) {
        Map<String, Object> map = new HashMap<>();
        map.put("punchOut", punchOut);
        return map;
    }

    // ── Work Days / Holidays ─────────────────────────────────────────────────

    public static Map<String, Object> updateWorkDayPayload(boolean isWorkingDay) {
        Map<String, Object> map = new HashMap<>();
        map.put("isWorkingDay", isWorkingDay);
        return map;
    }

    public static Map<String, Object> createHolidayPayload(String date, String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("date", date);
        map.put("name", name);
        return map;
    }

    // ── ATS ─────────────────────────────────────────────────────────────────

    public static Map<String, Object> createJobOpeningPayload(String title, int deptId, int locId) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("departmentId", deptId);
        map.put("locationId", locId);
        return map;
    }

    public static Map<String, Object> jobOpeningStatusPayload(String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        return map;
    }

    public static Map<String, Object> applyAsCandidatePayload(String name, String email, String resumeUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("email", email);
        map.put("resumeUrl", resumeUrl);
        return map;
    }

    public static Map<String, Object> candidateStagePayload(String stage) {
        Map<String, Object> map = new HashMap<>();
        map.put("stage", stage);
        return map;
    }

    // ── Payroll ──────────────────────────────────────────────────────────────

    public static Map<String, Object> createSalaryComponentPayload(String name, String type, double value) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("value", value);
        return map;
    }

    public static Map<String, Object> createPayCyclePayload(String cycleName, String fromDate, String toDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("cycleName", cycleName);
        map.put("fromDate", fromDate);
        map.put("toDate", toDate);
        return map;
    }

    public static Map<String, Object> finalizePayCyclePayload() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "FINALIZED");
        return map;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public static String today() {
        return LocalDate.now().format(DATE_FMT);
    }

    public static String daysFromNow(int days) {
        return LocalDate.now().plusDays(days).format(DATE_FMT);
    }
}
